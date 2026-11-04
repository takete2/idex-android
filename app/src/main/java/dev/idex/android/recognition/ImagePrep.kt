package dev.idex.android.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import kotlin.math.max
import kotlin.math.min

/** Centered square on white, then ImageNet-normalized NCHW float for the 224 model. */
object ImagePrep {
    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun decodeOriented(bytes: ByteArray): Bitmap {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longSide = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longSide / sample > 1600) sample *= 2
        val decoded = android.graphics.BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: throw RecognitionException("This photo couldn't be read.")
        val rotation = exifDegrees(bytes)
        val upright = if (rotation == 0) decoded else rotate(decoded, rotation)
        return centerSquareOnWhite(upright, 1200)
    }

    /** Pixel sprites are stretched onto a white square the way the iOS sample path does. */
    fun spriteSquare(sprite: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(384, 384, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { isFilterBitmap = false }
        canvas.drawBitmap(sprite, null, RectF(0f, 0f, 384f, 384f), paint)
        return out
    }

    fun centerSquareOnWhite(source: Bitmap, maxSide: Int): Bitmap {
        val side = min(maxSide, min(source.width, source.height)).coerceAtLeast(1)
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        val scale = side.toFloat() / min(source.width, source.height)
        val drawWidth = source.width * scale
        val drawHeight = source.height * scale
        val left = (side - drawWidth) / 2f
        val top = (side - drawHeight) / 2f
        val paint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(source, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)
        return out
    }

    fun toModelInput(bitmap: Bitmap): FloatArray {
        val scaled = if (bitmap.width == 224 && bitmap.height == 224) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        }
        val pixels = IntArray(224 * 224)
        scaled.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        val plane = 224 * 224
        val output = FloatArray(3 * plane)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val red = ((pixel shr 16) and 0xFF) / 255f
            val green = ((pixel shr 8) and 0xFF) / 255f
            val blue = (pixel and 0xFF) / 255f
            output[index] = (red - mean[0]) / std[0]
            output[plane + index] = (green - mean[1]) / std[1]
            output[2 * plane + index] = (blue - mean[2]) / std[2]
        }
        return output
    }

    private fun exifDegrees(bytes: ByteArray): Int {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
