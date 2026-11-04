package dev.idex.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.ImageCapture
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import dev.idex.android.ui.CameraStatus
import dev.idex.android.ui.DexBackHandler
import dev.idex.android.ui.DexScaffold
import dev.idex.android.ui.DexViewModel
import dev.idex.android.ui.Viewfinder
import dev.idex.android.ui.captureStill
import dev.idex.android.ui.theme.IdexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: DexViewModel = viewModel()
            IdexTheme {
                DexRoute(model)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun DexRoute(model: DexViewModel) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val widthClass = calculateWindowSizeClass(activity).widthSizeClass
    val layoutInfo by WindowInfoTracker.getOrCreate(activity)
        .windowLayoutInfo(activity)
        .collectAsStateWithLifecycle(initialValue = WindowLayoutInfo(emptyList()))
    val fold = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
    val verticalFold = fold != null &&
        fold.orientation == FoldingFeature.Orientation.VERTICAL &&
        (fold.isSeparating || fold.state == FoldingFeature.State.FLAT || fold.state == FoldingFeature.State.HALF_OPENED)
    val horizontalSeparating = fold != null &&
        fold.orientation == FoldingFeature.Orientation.HORIZONTAL &&
        fold.isSeparating
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) model.openCamera() else model.onCameraDenied()
    }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) model.importPhoto(uri)
    }
    val state = model.state
    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val canRescan = state.viewfinder == Viewfinder.Result || (state.message != null && !state.isScanning)

    DexBackHandler(model.handlesBack()) { model.back() }
    DexScaffold(
        state = state,
        catalog = model.catalog,
        sprites = model.sprites,
        review = model.review,
        verticalFold = verticalFold,
        horizontalSeparatingFold = horizontalSeparating,
        fold = fold,
        expandedWidth = widthClass == WindowWidthSizeClass.Expanded,
        showCamera = state.viewfinder == Viewfinder.Camera && state.cameraStatus != CameraStatus.Denied,
        onCamera = {
            if (cameraGranted()) model.openCamera() else permission.launch(Manifest.permission.CAMERA)
        },
        onGallery = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onScanOrRescan = {
            if (canRescan) {
                model.rescan(cameraGranted())
            } else {
                model.scan {
                    val capture = imageCapture ?: error("camera")
                    val rotation = view.display?.rotation ?: Surface.ROTATION_0
                    captureStill(capture, rotation, executor)
                }
            }
        },
        onOpenRecord = model::openEntry,
        onOpenCatalog = model::openCatalog,
        onSettings = model::openSettings,
        onCloseEntry = model::back,
        onCloseCatalog = { model.back() },
        onCloseSettings = model::closeSettings,
        onSelect = model::browse,
        onPrevious = { model.step(-1) },
        onNext = { model.step(1) },
        onRead = model::speakSelected,
        onStop = model::stopSpeaking,
        onToggleStats = model::toggleStats,
        onLanguage = model::setLanguage,
        onSound = model::setSoundEnabled,
        onCameraReady = model::onCameraReady,
        onCameraFailed = model::onCameraFailed,
        onCaptureReady = { imageCapture = it },
    )
}

private fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    error("DexRoute requires an Activity")
}
