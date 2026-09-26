package com.geno1024.pictureframe.render

import androidx.compose.ui.geometry.Size
import com.geno1024.pictureframe.model.Aspect
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.FrameStyle
import com.geno1024.pictureframe.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    private val photoAspect = 4f / 3f
    private val photoLongEdge = 4000

    private fun stageOf(
        state: EditorState,
        size: Size = Size(1080f, 1080f),
    ) = Layout.stage(state, size, photoAspect)

    @Test
    fun originalAspectFollowsThePhoto() {
        val size = Layout.canvasSize(EditorState(), photoAspect, photoLongEdge)
        assertEquals(photoAspect, size.width / size.height, 0.001f)
    }

    @Test
    fun fixedAspectOverridesThePhoto() {
        val state = EditorState(aspect = Aspect.Story)
        val size = Layout.canvasSize(state, photoAspect, photoLongEdge)
        assertEquals(9f / 16f, size.width / size.height, 0.001f)
    }

    @Test
    fun longEdgeIsEvenSoJpegEncodersAcceptIt() {
        for (option in Resolution.entries) {
            for (aspect in Aspect.entries) {
                val state = EditorState(aspect = aspect, resolution = option)
                val size = Layout.canvasSize(state, photoAspect, photoLongEdge)
                assertEquals("${aspect} ${option.label}", 0, size.width.toInt() % 2)
                assertEquals("${aspect} ${option.label}", 0, size.height.toInt() % 2)
            }
        }
    }

    @Test
    fun requestedResolutionDrivesTheLongEdge() {
        val state = EditorState(aspect = Aspect.Square, resolution = Resolution.P1080)
        assertEquals(1080f, Layout.canvasSize(state, photoAspect, photoLongEdge).width, 0.5f)
    }

    @Test
    fun longEdgeIsCapped() {
        val state = EditorState(aspect = Aspect.Square, resolution = Resolution.P2160)
        val size = Layout.canvasSize(state, 1f, 20000)
        assertTrue(size.width <= 4096f)
    }

    @Test
    fun photoKeepsItsAspectInsideTheFrame() {
        val state = EditorState(
            frame = state().frame.copy(style = FrameStyle.Solid, width = 0.05f, matWidth = 0.03f),
            margin = 0.06f,
        )
        val stage = stageOf(state)
        assertEquals(photoAspect, stage.photo.width / stage.photo.height, 0.01f)
    }

    @Test
    fun frameSurroundsMatSurroundsPhoto() {
        val state = EditorState(
            frame = state().frame.copy(style = FrameStyle.Solid, width = 0.05f, matWidth = 0.03f),
            margin = 0.06f,
        )
        val stage = stageOf(state)
        assertContains(stage.frame, stage.mat)
        assertContains(stage.mat, stage.photo)
    }

    private fun assertContains(outer: androidx.compose.ui.geometry.Rect, inner: androidx.compose.ui.geometry.Rect) {
        assertTrue("left ${inner.left} !>= ${outer.left}", inner.left >= outer.left)
        assertTrue("top ${inner.top} !>= ${outer.top}", inner.top >= outer.top)
        assertTrue("right ${inner.right} !<= ${outer.right}", inner.right <= outer.right)
        assertTrue("bottom ${inner.bottom} !<= ${outer.bottom}", inner.bottom <= outer.bottom)
    }

    @Test
    fun noFrameNoMarginLetsThePhotoFillTheCanvas() {
        val state = EditorState(frame = state().frame.copy(style = FrameStyle.None), margin = 0f)
        val stage = stageOf(state, Size(1200f, 900f))
        assertEquals(0f, stage.photo.left, 0.5f)
        assertEquals(0f, stage.photo.top, 0.5f)
        assertEquals(1200f, stage.photo.width, 0.5f)
        assertEquals(900f, stage.photo.height, 0.5f)
    }

    @Test
    fun widerMarginShrinksThePhoto() {
        val tight = stageOf(EditorState(margin = 0f))
        val loose = stageOf(EditorState(margin = 0.15f))
        assertTrue(loose.photo.width < tight.photo.width)
    }

    @Test
    fun polaroidGivesTheBottomMoreRoomThanTheTop() {
        val state = EditorState(
            frame = state().frame.copy(style = FrameStyle.Polaroid, width = 0.03f, matWidth = 0.08f),
            margin = 0.05f,
        )
        val stage = stageOf(state)
        val topGap = stage.mat.top - stage.photo.top
        val bottomGap = stage.mat.bottom - stage.photo.bottom
        assertTrue("$bottomGap !> $topGap", bottomGap > topGap * 2f)
    }

    @Test
    fun geometryIsIdenticalAtPreviewAndExportScale() {
        val state = EditorState(
            frame = state().frame.copy(style = FrameStyle.Bevel, width = 0.04f, matWidth = 0.02f),
            margin = 0.07f,
        )
        val small = stageOf(state, Size(360f, 360f))
        val large = stageOf(state, Size(3600f, 3600f))
        val scale = 10f
        assertEquals(small.photo.left * scale, large.photo.left, 0.5f)
        assertEquals(small.photo.width * scale, large.photo.width, 0.5f)
        assertEquals(small.mat.top * scale, large.mat.top, 0.5f)
        assertEquals(small.frame.bottom * scale, large.frame.bottom, 0.5f)
    }

    @Test
    fun placedImageCoversTheTargetRectExactly() {
        val base = EditorState()
        val states = listOf(base) + FrameStyle.entries.map { style ->
            base.copy(frame = base.frame.copy(style = style, width = 0.06f, matWidth = 0.04f), margin = 0.1f)
        }
        for (state in states) {
            val stage = Layout.stage(state, Size(1080f, 1080f), photoAspect)
            val place = Layout.placeImage(3000, 2000, stage.photo)
            assertEquals("left", stage.photo.left, place.mapX(0f), 0.001f)
            assertEquals("top", stage.photo.top, place.mapY(0f), 0.001f)
            assertEquals("right", stage.photo.right, place.mapX(3000f), 0.001f)
            assertEquals("bottom", stage.photo.bottom, place.mapY(2000f), 0.001f)
        }
    }

    @Test
    fun placedImageIsIndependentOfTheCanvasSize() {
        val state = EditorState(
            frame = EditorState().frame.copy(style = FrameStyle.Bevel, width = 0.05f, matWidth = 0.03f),
            margin = 0.08f,
        )
        val small = Layout.stage(state, Size(400f, 400f), photoAspect)
        val large = Layout.stage(state, Size(4000f, 4000f), photoAspect)
        val a = Layout.placeImage(2000, 1500, small.photo)
        val b = Layout.placeImage(2000, 1500, large.photo)
        assertEquals(a.mapX(0f) * 10f, b.mapX(0f), 0.01f)
        assertEquals(a.mapX(2000f) * 10f, b.mapX(2000f), 0.01f)
        assertEquals(a.mapY(0f) * 10f, b.mapY(0f), 0.01f)
    }

    @Test
    fun containFitsWithinTheBox() {
        val (w, h) = Layout.contain(aspect = 2f, maxWidth = 100f, maxHeight = 100f)
        assertEquals(100f, w, 0.001f)
        assertEquals(50f, h, 0.001f)
    }

    @Test
    fun aDegeneratePhotoAspectDoesNotProduceNaN() {
        val stage = Layout.stage(EditorState(), Size(1000f, 1000f), Float.NaN)
        assertTrue(stage.photo.width.isFinite() && stage.photo.height.isFinite())
    }

    private fun state() = EditorState()
}
