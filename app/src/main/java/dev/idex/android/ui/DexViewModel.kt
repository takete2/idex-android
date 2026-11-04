package dev.idex.android.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.idex.android.audio.DexAudio
import dev.idex.android.data.AppLanguage
import dev.idex.android.data.DexPrefs
import dev.idex.android.data.Pokemon
import dev.idex.android.data.PokemonCatalog
import dev.idex.android.recognition.ImagePrep
import dev.idex.android.recognition.KantoClassifier
import dev.idex.android.recognition.RecognitionCandidate
import dev.idex.android.recognition.loadCatalog
import dev.idex.android.ui.sprites.SpriteStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScanSource { Sample, Camera, Photo }

enum class Viewfinder { Sample, Camera, Review, Result }

enum class CameraStatus { Idle, Starting, Ready, Denied, Unavailable }

data class DexUiState(
    val language: AppLanguage,
    val selectedId: Int,
    val seenIds: Set<Int>,
    val soundEnabled: Boolean,
    val isScanning: Boolean = false,
    val source: ScanSource = ScanSource.Sample,
    val viewfinder: Viewfinder = Viewfinder.Sample,
    val message: String? = null,
    val candidates: List<RecognitionCandidate> = emptyList(),
    val confidence: Double? = null,
    val identified: Boolean = false,
    val cameraStatus: CameraStatus = CameraStatus.Idle,
    val showEntry: Boolean = false,
    val showCatalog: Boolean = false,
    val showSettings: Boolean = false,
    val showStats: Boolean = false,
    val speaking: Boolean = false,
)

class DexViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = DexPrefs(application)
    private val classifier = KantoClassifier(application)
    val catalog: PokemonCatalog = loadCatalog(application)
    val sprites = SpriteStore(application)
    val audio = DexAudio(application)

    var state by mutableStateOf(
        DexUiState(
            language = prefs.language(),
            selectedId = prefs.lastViewed(),
            seenIds = prefs.seen(),
            soundEnabled = prefs.soundEnabled(),
        ),
    )
        private set

    var review: Bitmap? by mutableStateOf(null)
        private set

    private var generation = 0

    init {
        audio.start()
        audio.setSoundEnabled(state.soundEnabled)
        audio.onSpeaking = { speaking -> state = state.copy(speaking = speaking) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { classifier.ensureLoaded() }
        }
    }

    fun text(english: String, chinese: String, japanese: String): String =
        state.language.text(english, chinese, japanese)

    fun selected(): Pokemon? = catalog.pokemon(state.selectedId)

    fun sample(): Pokemon? = catalog.pokemon(DexPrefs.SAMPLE_SPECIES_ID)

    fun setLanguage(language: AppLanguage) {
        audio.playButton()
        audio.stopSpeaking()
        prefs.setLanguage(language)
        state = state.copy(language = language, message = null)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.setSoundEnabled(enabled)
        audio.setSoundEnabled(enabled)
        state = state.copy(soundEnabled = enabled)
    }

    fun openCamera() {
        audio.playButton()
        clearRecognition()
        state = state.copy(
            source = ScanSource.Camera,
            viewfinder = Viewfinder.Camera,
            cameraStatus = CameraStatus.Starting,
            showEntry = false,
        )
    }

    fun onCameraReady() {
        if (state.source == ScanSource.Camera && state.viewfinder == Viewfinder.Camera) {
            state = state.copy(cameraStatus = CameraStatus.Ready)
        }
    }

    fun onCameraDenied() {
        state = state.copy(
            source = ScanSource.Sample,
            viewfinder = Viewfinder.Sample,
            cameraStatus = CameraStatus.Denied,
            message = text(
                "Camera access is off. Import a photo, or enable the camera in Settings.",
                "相机权限未开启。请导入照片，或在系统设置中允许相机。",
                "カメラへのアクセスが無効です。写真を読み込むか、設定でカメラを有効にしてください。",
            ),
        )
    }

    fun onCameraFailed() {
        state = state.copy(
            source = ScanSource.Sample,
            viewfinder = Viewfinder.Sample,
            cameraStatus = CameraStatus.Unavailable,
            message = text(
                "Camera unavailable. Import a photo instead.",
                "相机暂不可用，请改为导入照片。",
                "カメラを利用できません。写真を読み込んでください。",
            ),
        )
    }

    fun openEntry() {
        audio.playButton()
        state = state.copy(showEntry = true, showCatalog = false, showSettings = false)
    }

    fun openCatalog() {
        audio.playButton()
        state = state.copy(showCatalog = true, showSettings = false)
    }

    fun openSettings() {
        audio.playButton()
        state = state.copy(showSettings = true)
    }

    fun closeSettings() {
        state = state.copy(showSettings = false)
    }

    fun toggleStats() {
        audio.playButton()
        state = state.copy(showStats = !state.showStats)
    }

    fun back() {
        state = when {
            state.showSettings -> state.copy(showSettings = false)
            state.showCatalog -> state.copy(showCatalog = false)
            state.showEntry -> state.copy(showEntry = false)
            else -> state
        }
    }

    fun handlesBack(): Boolean = state.showSettings || state.showCatalog || state.showEntry

    fun browse(id: Int) {
        audio.stopSpeaking()
        cancelWork()
        select(id)
        state = clearedRecognition().copy(showCatalog = false, showEntry = true)
        audio.playButton()
    }

    fun step(delta: Int) {
        audio.stopSpeaking()
        cancelWork()
        val next = ((state.selectedId - 1 + delta + 151) % 151) + 1
        select(next)
        state = clearedRecognition()
        audio.playButton()
    }

    fun rescan(cameraGranted: Boolean) {
        val returnToCamera = state.source != ScanSource.Sample && cameraGranted
        cancelWork()
        review = null
        state = state.copy(
            isScanning = false,
            source = if (returnToCamera) ScanSource.Camera else ScanSource.Sample,
            viewfinder = if (returnToCamera) Viewfinder.Camera else Viewfinder.Sample,
            cameraStatus = if (returnToCamera) CameraStatus.Starting else state.cameraStatus,
            message = null,
            candidates = emptyList(),
            confidence = null,
            identified = false,
        )
        audio.playButton()
    }

    fun scan(captureCamera: suspend () -> Bitmap) {
        if (state.isScanning) return
        val operation = ++generation
        viewModelScope.launch {
            state = state.copy(isScanning = true, message = null, candidates = emptyList(), confidence = null, identified = false)
            audio.stopSpeaking()
            audio.playScan()
            try {
                val bitmap = when (state.source) {
                    ScanSource.Sample -> withContext(Dispatchers.IO) {
                        val sprite = sprites.get(DexPrefs.SAMPLE_SPECIES_ID)
                            ?: throw IllegalStateException("sample")
                        ImagePrep.spriteSquare(sprite)
                    }
                    ScanSource.Camera -> captureCamera()
                    ScanSource.Photo -> review ?: throw IllegalStateException("photo")
                }
                if (operation != generation) return@launch
                val prepared = if (state.source == ScanSource.Sample) bitmap else ImagePrep.centerSquareOnWhite(bitmap, 1200)
                review = prepared
                state = state.copy(viewfinder = Viewfinder.Review)
                if (state.source == ScanSource.Camera) {
                    state = state.copy(source = ScanSource.Photo)
                }
                val result = withContext(Dispatchers.Default) { classifier.classify(prepared) }
                if (operation != generation) return@launch
                applyRecognition(result)
            } catch (_: Exception) {
                if (operation != generation) return@launch
                state = state.copy(
                    isScanning = false,
                    message = text(
                        "Couldn't scan. Try a photo or enable the camera.",
                        "未能扫描，请导入照片或启用相机。",
                        "スキャンできませんでした。写真を読み込むか、カメラを有効にしてください。",
                    ),
                )
                audio.playError()
            }
        }
    }

    fun importPhoto(uri: Uri) {
        if (state.isScanning) return
        val operation = ++generation
        viewModelScope.launch {
            state = state.copy(
                isScanning = true,
                message = null,
                candidates = emptyList(),
                confidence = null,
                identified = false,
                source = ScanSource.Photo,
            )
            audio.stopSpeaking()
            audio.playScan()
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("empty")
                if (operation != generation) return@launch
                val prepared = withContext(Dispatchers.Default) { ImagePrep.decodeOriented(bytes) }
                if (operation != generation) return@launch
                review = prepared
                state = state.copy(viewfinder = Viewfinder.Review)
                val result = withContext(Dispatchers.Default) { classifier.classify(prepared) }
                if (operation != generation) return@launch
                applyRecognition(result)
            } catch (_: Exception) {
                if (operation != generation) return@launch
                state = state.copy(
                    isScanning = false,
                    viewfinder = Viewfinder.Sample,
                    source = ScanSource.Sample,
                    message = text(
                        "This photo couldn't be read. Choose another image.",
                        "无法读取这张照片，请选择其他图片。",
                        "この写真を読み込めませんでした。別の画像を選んでください。",
                    ),
                )
                audio.playError()
            }
        }
    }

    fun speakSelected() {
        val pokemon = selected() ?: return
        audio.speak(pokemon.spokenEntry(state.language), state.language)
    }

    fun stopSpeaking() = audio.stopSpeaking()

    private fun applyRecognition(result: dev.idex.android.recognition.RecognitionResult) {
        val match = result.candidates.firstOrNull()
        val pokemon = match?.let { catalog.pokemon(it.dexId) }
        if (match == null || pokemon == null) {
            state = state.copy(
                isScanning = false,
                message = text(
                    "Recognition is unavailable. Restart iDex and try again.",
                    "识别暂不可用，请重启 iDex 后重试。",
                    "識別を利用できません。iDex を再起動して、もう一度お試しください。",
                ),
            )
            audio.playError()
            return
        }
        select(pokemon.id)
        val accepted = result.accepted != null
        val seen = if (accepted) state.seenIds + pokemon.id else state.seenIds
        if (accepted) prefs.setSeen(seen)
        review = null
        state = state.copy(
            isScanning = false,
            viewfinder = Viewfinder.Result,
            candidates = result.candidates,
            confidence = match.confidence,
            identified = accepted,
            seenIds = seen,
            showEntry = false,
            message = if (accepted) {
                null
            } else {
                text(
                    "Best match; low confidence.",
                    "最可能的匹配，置信度较低。",
                    "最有力候補ですが、確信度が低い結果です。",
                )
            },
        )
        if (accepted) {
            audio.announce(pokemon, state.language)
        } else {
            audio.playButton()
        }
    }

    private fun select(id: Int) {
        prefs.setLastViewed(id)
        state = state.copy(selectedId = id)
    }

    private fun clearRecognition() {
        cancelWork()
        state = clearedRecognition()
    }

    private fun clearedRecognition(): DexUiState {
        review = null
        val viewfinder = if (state.source == ScanSource.Camera) Viewfinder.Camera else Viewfinder.Sample
        return state.copy(
            viewfinder = viewfinder,
            message = null,
            candidates = emptyList(),
            confidence = null,
            identified = false,
            isScanning = false,
        )
    }

    private fun cancelWork() {
        generation++
        audio.stopAll()
        state = state.copy(isScanning = false)
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
