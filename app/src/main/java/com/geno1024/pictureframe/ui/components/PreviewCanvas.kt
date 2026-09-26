package com.geno1024.pictureframe.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.render.Layout
import com.geno1024.pictureframe.render.drawFramed
import kotlin.math.min

private val CheckerLight = Color(0xFF4A4D55)
private val CheckerDark = Color(0xFF3A3D44)

@Composable
fun PreviewCanvas(
    state: EditorState,
    photo: ImageBitmap?,
    blurred: ImageBitmap?,
    photoAspect: Float,
    photoLongEdge: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val canvas = Layout.canvasSize(state, photoAspect, photoLongEdge)
        val (fitWidth, fitHeight) = Layout.contain(
            aspect = canvas.width / canvas.height,
            maxWidth = size.width,
            maxHeight = size.height,
        )
        val target = Size(fitWidth, fitHeight)
        val stage = Layout.stage(state, target, photoAspect)

        withTransform({
            translate((size.width - target.width) / 2f, (size.height - target.height) / 2f)
        }) {
            drawCheckerboard(target, 12.dp.toPx())
            drawFramed(state, stage, photo, blurred)
        }
    }
}

private fun DrawScope.drawCheckerboard(target: Size, cell: Float) {
    drawRect(color = CheckerDark, size = target)
    var row = 0
    var y = 0f
    while (y < target.height) {
        var column = 0
        var x = 0f
        while (x < target.width) {
            if ((row + column) % 2 == 0) {
                drawRect(
                    color = CheckerLight,
                    topLeft = Offset(x, y),
                    size = Size(min(cell, target.width - x), min(cell, target.height - y)),
                )
            }
            column++
            x += cell
        }
        row++
        y += cell
    }
}
