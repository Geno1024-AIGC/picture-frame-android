package com.geno1024.pictureframe.render

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.geno1024.pictureframe.model.EditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun renderToBitmap(
    state: EditorState,
    photo: ImageBitmap?,
    blurred: ImageBitmap?,
    photoAspect: Float,
    photoLongEdge: Int,
): Bitmap = withContext(Dispatchers.Default) {
    val size = Layout.canvasSize(state, photoAspect, photoLongEdge)
    val width = size.width.toInt().coerceAtLeast(2)
    val height = size.height.toInt().coerceAtLeast(2)
    val target = ImageBitmap(width, height)
    val stage = Layout.stage(state, Size(width.toFloat(), height.toFloat()), photoAspect)

    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(target),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        drawFramed(state, stage, photo, blurred)
    }
    target.asAndroidBitmap()
}
