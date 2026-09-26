package com.geno1024.pictureframe.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.geno1024.pictureframe.model.BackgroundType
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.FrameStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun DrawScope.drawFramed(
    state: EditorState,
    stage: Stage,
    photo: ImageBitmap?,
    blurred: ImageBitmap?,
) {
    drawBackground(state, stage, blurred)
    if (state.frame.shadow > 0f) {
        drawSoftShadow(
            rect = stage.frame,
            radius = state.frame.cornerRadius * stage.unit,
            strength = state.frame.shadow,
            unit = stage.unit,
        )
    }
    drawFrameBand(state, stage)
    drawMat(state, stage)
    if (photo != null) drawPhoto(photo, stage)
}

private fun DrawScope.drawBackground(state: EditorState, stage: Stage, blurred: ImageBitmap?) {
    val spec = state.background
    when (spec.type) {
        BackgroundType.None -> Unit
        BackgroundType.Solid ->
            drawRect(color = spec.colors.firstOrNull() ?: Color.Transparent, size = stage.size)
        BackgroundType.Gradient ->
            drawRect(brush = gradientBrush(spec.colors, spec.angle, spec.radial, stage.size), size = stage.size)
        BackgroundType.Blurred -> if (blurred != null) {
            drawImageCover(blurred, stage.size)
        } else {
            drawRect(color = Color(0xFF2A2C33), size = stage.size)
        }
    }
    if (spec.dim > 0f && spec.type != BackgroundType.None) {
        drawRect(color = Color.Black.copy(alpha = spec.dim.coerceIn(0f, 1f)), size = stage.size)
    }
}

private fun DrawScope.drawFrameBand(state: EditorState, stage: Stage) {
    val spec = state.frame
    val rect = stage.frame
    val radius = CornerRadius(spec.cornerRadius * stage.unit)
    val matRadius = CornerRadius(spec.cornerRadius * stage.unit * 0.6f)
    val stroke = stage.frameStroke

    when (spec.style) {
        FrameStyle.None, FrameStyle.Shadow -> Unit

        FrameStyle.Solid, FrameStyle.Polaroid ->
            drawRoundRect(color = spec.color, topLeft = rect.topLeft, size = rect.size, cornerRadius = radius)

        FrameStyle.Bevel -> {
            val light = lerp(spec.color, Color.White, 0.32f)
            val dark = lerp(spec.color, Color.Black, 0.28f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(light, spec.color, dark),
                    start = rect.topLeft,
                    end = rect.bottomRight,
                ),
                topLeft = rect.topLeft,
                size = rect.size,
            )
            drawRoundRect(color = light, topLeft = rect.topLeft, size = rect.size, cornerRadius = radius)
            drawRoundRect(
                color = dark,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = radius,
                style = Stroke(width = max(1f, stroke * 0.22f)),
            )
        }

        FrameStyle.Inset -> {
            val dark = lerp(spec.color, Color.Black, 0.3f)
            val light = lerp(spec.color, Color.White, 0.3f)
            drawRoundRect(color = spec.color, topLeft = rect.topLeft, size = rect.size, cornerRadius = radius)
            drawRoundRect(
                color = dark,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = radius,
                style = Stroke(width = max(1f, stroke)),
            )
            drawRoundRect(
                color = light,
                topLeft = stage.mat.topLeft,
                size = stage.mat.size,
                cornerRadius = matRadius,
                style = Stroke(width = max(1f, stroke * 0.2f)),
            )
        }

        FrameStyle.Double -> {
            val line = max(1f, stroke * 0.26f)
            drawRoundRect(
                color = spec.color,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = radius,
                style = Stroke(width = line),
            )
            drawRoundRect(
                color = spec.edgeColor,
                topLeft = stage.mat.topLeft,
                size = stage.mat.size,
                cornerRadius = matRadius,
                style = Stroke(width = line),
            )
        }
    }
}

private fun DrawScope.drawMat(state: EditorState, stage: Stage) {
    if (state.frame.matWidth <= 0f) return
    val radius = CornerRadius(state.frame.cornerRadius * stage.unit * 0.6f)
    drawRoundRect(
        color = state.frame.matColor,
        topLeft = stage.mat.topLeft,
        size = stage.mat.size,
        cornerRadius = radius,
    )
}

private fun DrawScope.drawPhoto(photo: ImageBitmap, stage: Stage) {
    val target = stage.photo
    val path = Path().apply { addRoundRect(RoundRect(target)) }
    clipPath(path) {
        val place = Layout.placeImage(photo.width, photo.height, target)
        withTransform({
            scale(place.scaleX, place.scaleY, pivot = Offset.Zero)
            translate(place.translateX, place.translateY)
        }) {
            drawImage(photo, filterQuality = FilterQuality.High)
        }
    }
}

private fun DrawScope.drawSoftShadow(rect: Rect, radius: Float, strength: Float, unit: Float) {
    val clamped = strength.coerceIn(0f, 1f)
    val spread = 0.055f * unit * clamped
    val drop = 0.02f * unit * clamped
    val steps = 14
    for (i in steps downTo 1) {
        val t = i / steps.toFloat()
        val grow = spread * t
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.15f * clamped * (1f - t)),
            topLeft = Offset(rect.left - grow, rect.top - grow + drop * t),
            size = Size(rect.width + grow * 2f, rect.height + grow * 2f),
            cornerRadius = CornerRadius(radius + grow),
        )
    }
}

fun gradientBrush(colors: List<Color>, angleDeg: Float, radial: Boolean, size: Size): Brush {
    val stops = colors.ifEmpty { listOf(Color.Transparent) }
    val center = Offset(size.width / 2f, size.height / 2f)
    if (radial) {
        return Brush.radialGradient(
            colors = stops,
            center = center,
            radius = max(size.width, size.height) * 0.75f,
        )
    }
    val radians = Math.toRadians(angleDeg.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val reach = abs(dx) * size.width + abs(dy) * size.height
    return Brush.linearGradient(
        colors = stops,
        start = Offset(center.x - dx * reach / 2f, center.y - dy * reach / 2f),
        end = Offset(center.x + dx * reach / 2f, center.y + dy * reach / 2f),
    )
}

internal fun DrawScope.drawImageCover(image: ImageBitmap, size: Size) {
    val scale = max(size.width / image.width, size.height / image.height)
    val dw = image.width * scale
    val dh = image.height * scale
    withTransform({
        scale(scale, scale, pivot = Offset.Zero)
        translate((size.width - dw) / 2f, (size.height - dh) / 2f)
    }) {
        drawImage(image, filterQuality = FilterQuality.High)
    }
}
