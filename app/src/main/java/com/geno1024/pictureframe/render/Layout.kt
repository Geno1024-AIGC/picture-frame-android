package com.geno1024.pictureframe.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.geno1024.pictureframe.model.Aspect
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.FrameStyle
import com.geno1024.pictureframe.model.Resolution
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_LONG_EDGE = 4096
private const val POLAROID_BOTTOM_SCALE = 2.4f

data class Stage(
    val size: Size,
    val unit: Float,
    val photo: Rect,
    val mat: Rect,
    val frame: Rect,
) {
    val matStroke: Float get() = (mat.width - photo.width) / 2f
    val frameStroke: Float get() = (frame.width - mat.width) / 2f
}

data class ImagePlacement(
    val scaleX: Float,
    val scaleY: Float,
    val translateX: Float,
    val translateY: Float,
) {
    fun mapX(x: Float): Float = x * scaleX + translateX
    fun mapY(y: Float): Float = y * scaleY + translateY
}

object Layout {

    fun canvasSize(state: EditorState, photoAspect: Float, photoLongEdge: Int): Size {
        val ratio = if (state.aspect.widthToHeight > 0f) state.aspect.widthToHeight else photoAspect
        val safeRatio = if (ratio.isFinite() && ratio > 0f) ratio else 1f
        val longEdge = when {
            state.resolution.longEdge > 0 -> min(state.resolution.longEdge, MAX_LONG_EDGE)
            photoLongEdge > 0 -> min(photoLongEdge, MAX_LONG_EDGE)
            else -> 2048
        }
        return if (safeRatio >= 1f) {
            val w = align(longEdge)
            Size(w.toFloat(), align((w / safeRatio).roundToInt()).toFloat())
        } else {
            val h = align(longEdge)
            Size(align((h * safeRatio).roundToInt()).toFloat(), h.toFloat())
        }
    }

    fun stage(state: EditorState, size: Size, photoAspect: Float): Stage {
        val unit = min(size.width, size.height)
        val frameW = when (state.frame.style) {
            FrameStyle.None -> 0f
            else -> state.frame.width * unit
        }
        val matW = state.frame.matWidth * unit
        val margin = state.margin * unit
        val polaroid = state.frame.style == FrameStyle.Polaroid

        val matTop = matW
        val matBottom = if (polaroid) matW * POLAROID_BOTTOM_SCALE else matW

        val insetH = margin + frameW + matW
        val insetV = margin + frameW + matTop
        val insetVBottom = margin + frameW + matBottom

        val availW = max(1f, size.width - insetH * 2f)
        val availH = max(1f, size.height - insetV - insetVBottom)

        val safeAspect = if (photoAspect.isFinite() && photoAspect > 0f) photoAspect else 1f
        val (pw, ph) = contain(safeAspect, availW, availH)

        val photo = Rect(
            offset = androidx.compose.ui.geometry.Offset(
                x = insetH + (availW - pw) / 2f,
                y = insetV + (availH - ph) / 2f,
            ),
            size = Size(pw, ph),
        )

        val mat = Rect(
            left = photo.left - matW,
            top = photo.top - matTop,
            right = photo.right + matW,
            bottom = photo.bottom + matBottom,
        )
        val frame = mat.inflate(frameW)
        return Stage(size = size, unit = unit, photo = photo, mat = mat, frame = frame)
    }

    fun placeImage(imageWidth: Int, imageHeight: Int, target: Rect): ImagePlacement {
        require(imageWidth > 0 && imageHeight > 0) { "image must have a positive size" }
        return ImagePlacement(
            scaleX = target.width / imageWidth,
            scaleY = target.height / imageHeight,
            translateX = target.left,
            translateY = target.top,
        )
    }

    fun contain(aspect: Float, maxWidth: Float, maxHeight: Float): Pair<Float, Float> =
        if (maxWidth / maxHeight > aspect) {
            maxHeight * aspect to maxHeight
        } else {
            maxWidth to maxWidth / aspect
        }

    private fun align(value: Int): Int = max(2, value - value % 2)
}
