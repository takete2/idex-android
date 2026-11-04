package dev.idex.android.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dev.idex.android.data.AppLanguage
import dev.idex.android.data.Pokemon
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

enum class SpeechProblem { NoEngine, MissingVoice }

/** Local chimes, bundled cries, and the system speech engine. No network. */
class DexAudio(context: Context) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var speech: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var soundEnabled: Boolean = true
    private var ready = false
    private var failed = false
    private var started = false
    private var pending: QueuedUtterance? = null
    private var utteranceSerial = 0
    private var activeUtterance: String? = null

    var onSpeaking: (Boolean) -> Unit = {}
    var onSpeechUnavailable: (SpeechProblem) -> Unit = {}

    fun start() {
        if (started) return
        started = true
        try {
            speech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ready = true
                    speech?.setOnUtteranceProgressListener(progressListener)
                    Log.i(TAG, "TextToSpeech ready, engines visible")
                    val queued = pending
                    pending = null
                    if (queued != null && soundEnabled) speakOnMain(queued.text, queued.language)
                } else {
                    ready = false
                    failed = true
                    Log.e(TAG, "TextToSpeech init failed, status=$status. Check TTS_SERVICE package visibility.")
                    val hadPending = pending != null
                    pending = null
                    report(false)
                    if (hadPending) reportUnavailable(SpeechProblem.NoEngine)
                }
            }
        } catch (error: Exception) {
            failed = true
            ready = false
            Log.e(TAG, "TextToSpeech constructor failed", error)
            reportUnavailable(SpeechProblem.NoEngine)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        if (!enabled) stopAll()
    }

    fun playButton() = blip(880.0, 0.045)
    fun playOpen() = blip(520.0, 0.07)
    fun playError() = blip(180.0, 0.12)
    fun playScan() = sweep()

    fun announce(pokemon: Pokemon, language: AppLanguage) {
        stopAll()
        if (!soundEnabled) return
        playCry(pokemon.id) {
            speak(pokemon.spokenEntry(language), language)
        }
    }

    fun speak(text: String, language: AppLanguage) {
        main.post { speakOnMain(text, language) }
    }

    fun stopSpeaking() {
        pending = null
        activeUtterance = null
        speech?.stop()
        report(false)
    }

    fun stopAll() {
        executor.execute {
            player?.runCatching {
                stop()
                release()
            }
            player = null
        }
        stopSpeaking()
    }

    fun release() {
        stopAll()
        speech?.shutdown()
        speech = null
        ready = false
        executor.shutdown()
    }

    fun openSystemSettings(context: Context) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val settings = Intent("com.android.settings.TTS_SETTINGS").addFlags(flags)
        val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(flags)
        val opened = runCatching { context.startActivity(settings) }.isSuccess
        if (!opened) {
            runCatching { context.startActivity(install) }
                .onFailure { Log.e(TAG, "Could not open TTS settings", it) }
        }
    }

    private fun speakOnMain(text: String, language: AppLanguage) {
        if (!soundEnabled || text.isBlank()) return
        if (failed) {
            reportUnavailable(SpeechProblem.NoEngine)
            report(false)
            return
        }
        val engine = speech
        if (!ready || engine == null) {
            pending = QueuedUtterance(text, language)
            Log.i(TAG, "Queuing speech until TextToSpeech is ready")
            return
        }
        if (!applyLanguage(engine, language)) {
            reportUnavailable(SpeechProblem.MissingVoice)
            report(false)
            return
        }
        val utteranceId = "idex-${++utteranceSerial}"
        activeUtterance = utteranceId
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak() returned ERROR for $utteranceId")
            if (activeUtterance == utteranceId) activeUtterance = null
            report(false)
            reportUnavailable(SpeechProblem.NoEngine)
        }
    }

    private fun applyLanguage(engine: TextToSpeech, language: AppLanguage): Boolean {
        val attempts = TtsVoices.candidates(language.speechLocale(), Locale.getDefault())
        for (locale in attempts) {
            val result = engine.setLanguage(locale)
            Log.i(TAG, "setLanguage ${locale.toLanguageTag()} -> $result")
            if (TtsVoices.usable(result)) return true
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Voice unavailable for ${locale.toLanguageTag()} (result=$result)")
            }
        }
        Log.e(TAG, "No usable TTS voice for ${language.speechTag}")
        return false
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (utteranceId != null && utteranceId == activeUtterance) report(true)
        }

        override fun onDone(utteranceId: String?) = finish(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            Log.e(TAG, "TTS error for $utteranceId")
            finish(utteranceId)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            Log.e(TAG, "TTS error $errorCode for $utteranceId")
            finish(utteranceId)
        }

        private fun finish(utteranceId: String?) {
            if (utteranceId != null && utteranceId == activeUtterance) {
                activeUtterance = null
                report(false)
            }
        }
    }

    private fun playCry(id: Int, then: () -> Unit) {
        executor.execute {
            var descriptor: android.content.res.AssetFileDescriptor? = null
            try {
                val path = "cries/%03d.wav".format(id)
                descriptor = appContext.assets.openFd(path)
                val media = MediaPlayer()
                media.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                media.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                media.setOnCompletionListener {
                    it.release()
                    if (player == it) player = null
                    main.post(then)
                }
                media.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "Cry player error what=$what extra=$extra id=$id")
                    mp.release()
                    if (player == mp) player = null
                    main.post(then)
                    true
                }
                media.prepare()
                descriptor.close()
                descriptor = null
                player = media
                media.start()
            } catch (error: Exception) {
                Log.e(TAG, "Cry playback failed for $id. WAV assets must stay uncompressed for openFd.", error)
                runCatching { descriptor?.close() }
                main.post(then)
            }
        }
    }

    private fun report(speaking: Boolean) {
        main.post { onSpeaking(speaking) }
    }

    private fun reportUnavailable(problem: SpeechProblem) {
        main.post { onSpeechUnavailable(problem) }
    }

    private fun blip(frequency: Double, seconds: Double) {
        if (!soundEnabled) return
        executor.execute { tone(frequency, seconds, sweep = false) }
    }

    private fun sweep() {
        if (!soundEnabled) return
        executor.execute { tone(640.0, 0.22, sweep = true) }
    }

    private fun tone(frequency: Double, seconds: Double, sweep: Boolean) {
        val sampleRate = 22050
        val count = (sampleRate * seconds).toInt().coerceAtLeast(1)
        val buffer = ShortArray(count)
        for (index in 0 until count) {
            val time = index / sampleRate.toDouble()
            val current = if (sweep) frequency + 900.0 * (index / count.toDouble()) else frequency
            val envelope = sin(PI * index / count)
            buffer[index] = (sin(2.0 * PI * current * time) * envelope * 7000.0).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * 2)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        try {
            Thread.sleep((seconds * 1000).toLong() + 30)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        track.release()
    }

    private data class QueuedUtterance(val text: String, val language: AppLanguage)

    companion object {
        private const val TAG = "iDex"
    }
}
