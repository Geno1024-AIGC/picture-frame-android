package com.geno1024.pictureframe.render

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max
import kotlin.math.roundToInt

private const val BLUR_TARGET = 128
private const val BLUR_PASSES = 3

fun blurredBackdrop(photo: ImageBitmap, strength: Float): ImageBitmap {
    val longEdge = max(photo.width, photo.height).coerceAtLeast(1)
    val scale = BLUR_TARGET.toFloat() / longEdge
    val w = (photo.width * scale).roundToInt().coerceAtLeast(2)
    val h = (photo.height * scale).roundToInt().coerceAtLeast(2)

    val small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(small)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    val scaleFit = max(w.toFloat() / photo.width, h.toFloat() / photo.height)
    val drawW = photo.width * scaleFit
    val drawH = photo.height * scaleFit
    canvas.drawBitmap(
        photo.asAndroidBitmap(),
        null,
        Rect(
            ((w - drawW) / 2f).roundToInt(),
            ((h - drawH) / 2f).roundToInt(),
            ((w + drawW) / 2f).roundToInt(),
            ((h + drawH) / 2f).roundToInt(),
        ),
        paint,
    )

    val radius = (2 + strength.coerceIn(0f, 1f) * (BLUR_TARGET / 7f)).roundToInt().coerceAtLeast(1)
    val pixels = IntArray(w * h)
    small.getPixels(pixels, 0, w, 0, 0, w, h)
    boxBlur(pixels, w, h, radius, BLUR_PASSES)
    small.setPixels(pixels, 0, w, 0, 0, w, h)
    return small.asImageBitmap()
}

private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int, passes: Int) {
    val scratch = IntArray(pixels.size)
    for (pass in 0 until passes) {
        blurAxis(pixels, scratch, width, height, radius, horizontal = true)
        blurAxis(scratch, pixels, width, height, radius, horizontal = false)
    }
}

private fun blurAxis(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    horizontal: Boolean,
) {
    val outer = if (horizontal) height else width
    val inner = if (horizontal) width else height
    val window = radius * 2 + 1
    for (o in 0 until outer) {
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val c = src[pixelAt(o, i.coerceIn(0, inner - 1), width, inner, horizontal)]
            a += (c ushr 24) and 0xFF
            r += (c ushr 16) and 0xFF
            g += (c ushr 8) and 0xFF
            b += c and 0xFF
        }
        for (i in 0 until inner) {
            dst[pixelAt(o, i, width, inner, horizontal)] =
                (a / window shl 24) or (r / window shl 16) or (g / window shl 8) or (b / window)
            val outIndex = (i - radius).coerceIn(0, inner - 1)
            val inIndex = (i + radius + 1).coerceIn(0, inner - 1)
            val outColor = src[pixelAt(o, outIndex, width, inner, horizontal)]
            val inColor = src[pixelAt(o, inIndex, width, inner, horizontal)]
            a += ((inColor ushr 24) and 0xFF) - ((outColor ushr 24) and 0xFF)
            r += ((inColor ushr 16) and 0xFF) - ((outColor ushr 16) and 0xFF)
            g += ((inColor ushr 8) and 0xFF) - ((outColor ushr 8) and 0xFF)
            b += (inColor and 0xFF) - (outColor and 0xFF)
        }
    }
}

private fun pixelAt(outer: Int, inner: Int, width: Int, height: Int, horizontal: Boolean): Int =
    if (horizontal) outer * width + inner else inner * width + outer
