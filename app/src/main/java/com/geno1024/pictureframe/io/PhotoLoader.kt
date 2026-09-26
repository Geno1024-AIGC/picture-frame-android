package com.geno1024.pictureframe.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SOURCE_EDGE = 3072

data class LoadedPhoto(
    val image: ImageBitmap,
    val width: Int,
    val height: Int,
) {
    val aspect: Float get() = width.toFloat() / height.toFloat()
    val longEdge: Int get() = max(width, height)
}

suspend fun loadPhoto(context: Context, uri: Uri): LoadedPhoto? = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeModern(context, uri)
        } else {
            decodeLegacy(context, uri)
        }
        LoadedPhoto(
            image = bitmap.asImageBitmap(),
            width = bitmap.width,
            height = bitmap.height,
        )
    }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeModern(context: Context, uri: Uri): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val w = info.size.width
        val h = info.size.height
        val longEdge = max(w, h)
        if (longEdge > MAX_SOURCE_EDGE) {
            val scale = MAX_SOURCE_EDGE.toFloat() / longEdge
            decoder.setTargetSize(
                (w * scale).roundToInt().coerceAtLeast(1),
                (h * scale).roundToInt().coerceAtLeast(1),
            )
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.memorySizePolicy = ImageDecoder.MEMORY_POLICY_LOW_RAM
    }
}

private fun decodeLegacy(context: Context, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: error("cannot read bounds")

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SOURCE_EDGE) sample *= 2

    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = openStream(context, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("cannot decode image")

    val rotation = openStream(context, uri)?.use { readRotation(it) } ?: 0
    if (rotation == 0) return decoded
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}

private fun openStream(context: Context, uri: Uri) =
    runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()

private fun readRotation(stream: java.io.InputStream): Int =
    when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
