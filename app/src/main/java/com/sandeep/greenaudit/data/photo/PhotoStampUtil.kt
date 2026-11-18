package com.sandeep.greenaudit.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burns park name / lat-lon / accuracy / date-time into the pixels of a
 * just-captured JPEG, in place. Runs after CameraX saves the raw file, so
 * the stamp is part of the saved image itself, not just an overlay drawn
 * on the live preview. EXIF rotation is applied to the pixels first (and
 * then dropped, since the pixels are upright afterwards) so the stamp
 * reads right-side up regardless of how the device was held.
 */
object PhotoStampUtil {

    fun stamp(
        file: File,
        parkName: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        timestampMillis: Long,
    ) {
        val rotationDegrees = runCatching { ExifInterface(file.absolutePath).rotationDegrees }.getOrDefault(0)
        val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val upright = if (rotationDegrees != 0) rotate(decoded, rotationDegrees) else decoded
        val mutable = upright.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(mutable)
        val lines = listOf(
            parkName,
            "%.5f, %.5f ±%.0f m".format(latitude, longitude, accuracyMeters),
            SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestampMillis)),
        )

        val textPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = mutable.width * 0.035f
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        val padding = mutable.width * 0.03f
        val lineHeight = textPaint.textSize * 1.3f
        val barHeight = lineHeight * lines.size + padding

        canvas.drawRect(
            0f,
            mutable.height - barHeight,
            mutable.width.toFloat(),
            mutable.height.toFloat(),
            Paint().apply { color = Color.argb(150, 0, 0, 0) },
        )
        lines.forEachIndexed { index, line ->
            val baseline = mutable.height - barHeight + padding + lineHeight * (index + 1) - (lineHeight - textPaint.textSize)
            canvas.drawText(line, padding, baseline, textPaint)
        }

        FileOutputStream(file).use { out -> mutable.compress(Bitmap.CompressFormat.JPEG, 92, out) }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
