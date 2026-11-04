package dev.idex.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraPreview(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
    onFailed: () -> Unit,
    onCaptureReady: (ImageCapture?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = androidx.compose.runtime.remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            onCaptureReady(null)
            onDispose { }
        } else {
            val executor = ContextCompat.getMainExecutor(context)
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = runCatching { future.get() }.getOrElse {
                    onFailed()
                    return@addListener
                }
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    onCaptureReady(capture)
                    onReady()
                } catch (error: Exception) {
                    Log.e("iDex", "Camera bind failed", error)
                    onCaptureReady(null)
                    onFailed()
                }
            }, executor)
            onDispose {
                onCaptureReady(null)
                runCatching { future.get().unbindAll() }
            }
        }
    }
}

suspend fun captureStill(capture: ImageCapture, rotation: Int, executor: Executor): Bitmap =
    suspendCancellableCoroutine { continuation ->
        capture.targetRotation = rotation
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        continuation.resume(image.toSoftwareBitmap())
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            },
        )
    }

private fun ImageProxy.toSoftwareBitmap(): Bitmap {
    val plane = planes.first().buffer
    val bytes = ByteArray(plane.remaining())
    plane.get(bytes)
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalStateException("capture")
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return decoded
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}
