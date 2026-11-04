package dev.idex.android.ui

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idex.android.data.Pokemon
import dev.idex.android.ui.sprites.SpriteStore
import dev.idex.android.ui.theme.DexColors
import kotlinx.coroutines.delay

@Composable
fun ScannerPane(
    state: DexUiState,
    sprites: SpriteStore,
    sample: Pokemon?,
    matched: Pokemon?,
    review: Bitmap?,
    wide: Boolean,
    showCamera: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onScanOrRescan: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenCatalog: () -> Unit,
    onSettings: () -> Unit,
    onCameraReady: () -> Unit,
    onCameraFailed: () -> Unit,
    onCaptureReady: (androidx.camera.core.ImageCapture?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = state.language
    val canRescan = state.viewfinder == Viewfinder.Result || (state.message != null && !state.isScanning)
    val scanDisabled = state.isScanning ||
        (state.source == ScanSource.Camera && state.cameraStatus != CameraStatus.Ready && !canRescan)
    val buttonTitle = when {
        state.isScanning -> language.text("SCANNING", "扫描中", "スキャン中")
        canRescan -> language.text("RESCAN", "重新扫描", "再スキャン")
        else -> language.text("SCAN", "扫描", "スキャン")
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScannerLights(
                scanning = state.isScanning,
                speaking = state.speaking,
                identified = state.identified,
                pulse = state.lensPulse,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                PixelText(language.text("POKÉDEX", "宝可梦图鉴", "ポケモン図鑑"), language, 10, color = DexColors.Cream)
                Spacer(Modifier.height(6.dp))
                PixelText(
                    language.text("FIELD UNIT 001", "野外终端 001", "フィールド端末 001"),
                    language,
                    6,
                    color = DexColors.Cream.copy(0.9f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PixelText(language.text("OPTICAL SENSOR", "光学传感器", "光学センサー"), language, 6, color = Color.White.copy(0.65f))
            PixelText(language.text("KANTO / 01", "关都 / 01", "カントー / 01"), language, 6, color = Color.White.copy(0.65f))
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side = minOf(maxWidth * 0.92f, 440.dp)
            ViewfinderBezel(Modifier.size(side)) {
                Box(Modifier.fillMaxSize()) {
                    ViewfinderBody(state, sprites, sample, matched, review, showCamera, onCameraReady, onCameraFailed, onCaptureReady)
                    if (state.isScanning && state.viewfinder == Viewfinder.Review) ScanSweep()
                    ViewfinderHud(state, sample, matched)
                    if (state.viewfinder != Viewfinder.Result && state.viewfinder != Viewfinder.Review) Reticle()
                    if (state.message != null && state.viewfinder != Viewfinder.Result) {
                        MessageCard(state.message, language)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HardwareButton(onClick = onCamera, enabled = !state.isScanning) {
                Icon(Icons.Filled.PhotoCamera, language.text("Use camera", "使用相机", "カメラを使う"), tint = DexColors.Cream)
            }
            HardwareButton(onClick = onGallery, enabled = !state.isScanning) {
                Icon(Icons.Filled.PhotoLibrary, language.text("Import photo", "导入照片", "写真を読み込む"), tint = DexColors.Cream)
            }
            HardwareButton(
                onClick = onScanOrRescan,
                enabled = !scanDisabled,
                color = Color(0xFF1C453D),
                modifier = Modifier.weight(1f),
            ) {
                if (canRescan && !state.isScanning) {
                    Icon(Icons.Filled.Refresh, null, tint = DexColors.Cream)
                    Spacer(Modifier.width(8.dp))
                }
                PixelText(buttonTitle, language, 8, color = DexColors.Cream)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.clickable { if (wide) onOpenCatalog() else onOpenRecord() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = DexColors.Cream)
                    Spacer(Modifier.width(8.dp))
                    PixelText(
                        if (wide) language.text("POKÉDEX", "宝可梦图鉴", "ポケモン図鑑")
                        else language.text("OPEN RECORD", "打开记录", "図鑑を開く"),
                        language,
                        8,
                        color = DexColors.Cream,
                    )
                }
                Spacer(Modifier.height(6.dp))
                PixelText(
                    language.text(
                        "${state.seenIds.size} SEEN · 151 SPECIES",
                        "已发现 ${state.seenIds.size} · 共151种",
                        "発見 ${state.seenIds.size} · 全151種",
                    ),
                    language,
                    6,
                    color = Color.White.copy(0.55f),
                )
            }
            SpeakerGrille()
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, language.text("Settings", "设置", "設定"), tint = Color.White.copy(0.7f))
            }
        }
    }
}

@Composable
private fun ViewfinderBody(
    state: DexUiState,
    sprites: SpriteStore,
    sample: Pokemon?,
    matched: Pokemon?,
    review: Bitmap?,
    showCamera: Boolean,
    onCameraReady: () -> Unit,
    onCameraFailed: () -> Unit,
    onCaptureReady: (androidx.camera.core.ImageCapture?) -> Unit,
) {
    val language = state.language
    when {
        state.viewfinder == Viewfinder.Result && matched != null -> {
            RadarGrid()
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SpriteImage(sprites, matched.id, matched.name(language), Modifier.size(160.dp))
                Spacer(Modifier.height(8.dp))
                PixelText(matched.name(language), language, 10, color = DexColors.Ink)
                if (!state.identified) {
                    Spacer(Modifier.height(6.dp))
                    PixelText(
                        language.text("LOW CONFIDENCE · RESCAN", "置信度较低 · 请重新扫描", "確信度が低い結果 · 再スキャン"),
                        language,
                        6,
                        color = DexColors.Ink,
                        maxLines = 2,
                    )
                }
            }
        }
        state.viewfinder == Viewfinder.Review && review != null -> {
            Image(
                bitmap = review.asImageBitmap(),
                contentDescription = language.text("Captured scan image", "已捕获的扫描图像", "撮影したスキャン画像"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        showCamera && state.viewfinder == Viewfinder.Camera -> {
            Box(Modifier.fillMaxSize()) {
                CameraPreview(
                    enabled = true,
                    modifier = Modifier.fillMaxSize(),
                    onReady = onCameraReady,
                    onFailed = onCameraFailed,
                    onCaptureReady = onCaptureReady,
                )
                if (state.cameraStatus == CameraStatus.Starting) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = DexColors.Ink,
                        strokeWidth = 3.dp,
                    )
                }
            }
        }
        state.viewfinder == Viewfinder.Sample && sample != null -> {
            RadarGrid()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpriteImage(sprites, sample.id, sample.name(language), Modifier.fillMaxSize().padding(36.dp))
            }
        }
        state.cameraStatus == CameraStatus.Denied || state.cameraStatus == CameraStatus.Unavailable -> {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    state.message ?: language.text("Camera unavailable", "相机暂不可用", "カメラを利用できません"),
                    color = DexColors.Ink,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        else -> RadarGrid()
    }
}

@Composable
private fun ViewfinderHud(state: DexUiState, sample: Pokemon?, matched: Pokemon?) {
    val language = state.language
    val title = when {
        state.isScanning -> language.text("SCANNING…", "扫描中…", "スキャン中…")
        state.viewfinder == Viewfinder.Result && state.identified -> language.text("IDENTIFIED", "已识别", "識別完了")
        state.viewfinder == Viewfinder.Result -> language.text("BEST MATCH", "最可能匹配", "最有力候補")
        state.viewfinder == Viewfinder.Sample -> language.text("SAMPLE TARGET", "样本目标", "サンプル")
        state.viewfinder == Viewfinder.Review -> language.text("PHOTO REVIEW", "照片回看", "写真の確認")
        else -> language.text("LIVE VIEW", "实时取景", "ライブビュー")
    }
    val number = when (state.viewfinder) {
        Viewfinder.Result -> matched?.let { "#${it.number}" } ?: ""
        Viewfinder.Sample -> sample?.let { "#${it.number}" } ?: "#061"
        else -> "AF / 1×"
    }
    val trailing = if (state.viewfinder == Viewfinder.Result) {
        "${((state.confidence ?: 0.0) * 100).toInt()}%"
    } else {
        language.text("OFFLINE", "离线", "オフライン")
    }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (state.isScanning) Color(0xFFFFA000) else DexColors.Ink),
            )
            Spacer(Modifier.width(6.dp))
            PixelText(title, language, 6, color = DexColors.Ink, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Row {
            PixelText(number, language, 8, color = DexColors.Ink)
            Spacer(Modifier.weight(1f))
            PixelText(trailing, language, 6, color = DexColors.Ink)
        }
    }
}

@Composable
private fun MessageCard(message: String, language: dev.idex.android.data.AppLanguage) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(DexColors.Charcoal.copy(alpha = 0.94f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PixelText(message, language, 7, color = DexColors.Cream, maxLines = 4)
        }
    }
}

@Composable
private fun ScannerLights(scanning: Boolean, speaking: Boolean, identified: Boolean, pulse: Boolean) {
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val lensLit = LensLights.isLit(identified, speaking, pulse)
    var active by remember { mutableIntStateOf(0) }
    val chasing = scanning && !lensLit && !reduceMotion
    LaunchedEffect(chasing) {
        if (!chasing) return@LaunchedEffect
        while (true) {
            delay(220)
            active = (active + 1) % 3
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Lens(lit = lensLit, pulsing = pulse && identified)
        Spacer(Modifier.width(8.dp))
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Color.Red, Color.Yellow, Color(0xFF3DDC84)).forEachIndexed { index, color ->
                    Indicator(color, lit = scanning && !lensLit && (reduceMotion || index == active))
                }
            }
            Wordmark()
        }
    }
}

@Composable
private fun ScanSweep() {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "beam",
    )
    Canvas(Modifier.fillMaxSize()) {
        val beam = progress * size.height
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, DexColors.Lcd.copy(0.55f))),
            topLeft = Offset(0f, (beam - 64.dp.toPx()).coerceAtLeast(0f)),
            size = Size(size.width, 64.dp.toPx()),
        )
        drawRect(color = DexColors.Cream, topLeft = Offset(0f, beam), size = Size(size.width, 3.dp.toPx()))
    }
}
