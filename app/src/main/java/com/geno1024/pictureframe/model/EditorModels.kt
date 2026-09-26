package com.geno1024.pictureframe.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class FrameStyle(val label: String) {
    None("无框"),
    Solid("纯色"),
    Bevel("浮雕"),
    Inset("内嵌"),
    Double("双线"),
    Shadow("投影"),
    Polaroid("拍立得"),
}

data class FrameSpec(
    val style: FrameStyle = FrameStyle.None,
    val color: Color = Color.White,
    val edgeColor: Color = Color(0xFF14151A),
    val width: Float = 0.035f,
    val matWidth: Float = 0f,
    val matColor: Color = Color(0xFFF6F4EF),
    val cornerRadius: Float = 0.008f,
    val shadow: Float = 0.25f,
)

enum class BackgroundType(val label: String) {
    None("透明"),
    Solid("纯色"),
    Gradient("渐变"),
    Blurred("虚化"),
}

data class BackgroundSpec(
    val type: BackgroundType = BackgroundType.None,
    val colors: List<Color> = listOf(Color(0xFFF2F1EC)),
    val angle: Float = 135f,
    val radial: Boolean = false,
    val blurStrength: Float = 0.55f,
    val dim: Float = 0f,
)

enum class Aspect(val label: String, val widthToHeight: Float) {
    Original("原始", 0f),
    Square("1:1", 1f),
    Portrait45("4:5", 0.8f),
    Portrait34("3:4", 0.75f),
    Story("9:16", 9f / 16f),
    Landscape("16:9", 16f / 9f),
}

enum class Resolution(val label: String, val longEdge: Int) {
    Original("原始", 0),
    P1080("1080P", 1080),
    P1440("1440P", 1440),
    P2160("4K", 2160),
}

data class EditorState(
    val frame: FrameSpec = FrameSpec(),
    val background: BackgroundSpec = BackgroundSpec(),
    val aspect: Aspect = Aspect.Original,
    val resolution: Resolution = Resolution.Original,
    val margin: Float = 0.05f,
) {
    fun withFrame(transform: (FrameSpec) -> FrameSpec) = copy(frame = transform(frame))

    fun withBackground(transform: (BackgroundSpec) -> BackgroundSpec) =
        copy(background = transform(background))
}

data class Look(
    val name: String,
    val frame: FrameSpec,
    val background: BackgroundSpec,
    val margin: Float = 0.05f,
)

object Looks {
    val all = listOf(
        Look(
            name = "原图",
            frame = FrameSpec(style = FrameStyle.None),
            background = BackgroundSpec(type = BackgroundType.None),
            margin = 0f,
        ),
        Look(
            name = "白边",
            frame = FrameSpec(style = FrameStyle.Solid, color = Color.White, width = 0.05f, cornerRadius = 0.004f, shadow = 0.18f),
            background = BackgroundSpec(type = BackgroundType.Solid, colors = listOf(Color(0xFFEFEDE7))),
        ),
        Look(
            name = "拍立得",
            frame = FrameSpec(
                style = FrameStyle.Polaroid,
                color = Color(0xFFFBFAF7),
                width = 0.035f,
                matWidth = 0.075f,
                cornerRadius = 0.006f,
                shadow = 0.3f,
            ),
            background = BackgroundSpec(type = BackgroundType.Solid, colors = listOf(Color(0xFFDED9CF))),
        ),
        Look(
            name = "暗夜",
            frame = FrameSpec(style = FrameStyle.Solid, color = Color(0xFF17181C), width = 0.028f, cornerRadius = 0.01f, shadow = 0.4f),
            background = BackgroundSpec(
                type = BackgroundType.Gradient,
                colors = listOf(Color(0xFF23262E), Color(0xFF0E0F13)),
                angle = 135f,
            ),
            margin = 0.06f,
        ),
        Look(
            name = "虚化",
            frame = FrameSpec(style = FrameStyle.Shadow, width = 0.012f, cornerRadius = 0.012f, shadow = 0.45f),
            background = BackgroundSpec(type = BackgroundType.Blurred, blurStrength = 0.7f, dim = 0.12f),
            margin = 0.06f,
        ),
        Look(
            name = "暖阳",
            frame = FrameSpec(style = FrameStyle.Solid, color = Color(0xFFFFF4E2), width = 0.045f, cornerRadius = 0.02f, shadow = 0.22f),
            background = BackgroundSpec(
                type = BackgroundType.Gradient,
                colors = listOf(Color(0xFFFFE2C0), Color(0xFFFFC9A3)),
                angle = 160f,
            ),
            margin = 0.07f,
        ),
        Look(
            name = "双线",
            frame = FrameSpec(
                style = FrameStyle.Double,
                color = Color(0xFF1B1C20),
                edgeColor = Color(0xFFFAF9F6),
                width = 0.03f,
                matWidth = 0.006f,
                cornerRadius = 0.004f,
                shadow = 0.15f,
            ),
            background = BackgroundSpec(type = BackgroundType.Solid, colors = listOf(Color(0xFFD8D3C8))),
        ),
        Look(
            name = "留白",
            frame = FrameSpec(style = FrameStyle.None),
            background = BackgroundSpec(type = BackgroundType.Solid, colors = listOf(Color(0xFFF7F7F5))),
            margin = 0.12f,
        ),
    )

    fun apply(state: EditorState, look: Look) = EditorState(
        frame = look.frame,
        background = look.background,
        aspect = state.aspect,
        resolution = state.resolution,
        margin = look.margin,
    )
}

object Swatches {
    val solid = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF2F1EC),
        Color(0xFFD9D5CB),
        Color(0xFF14151A),
        Color(0xFF2F6BFF),
        Color(0xFF17A398),
        Color(0xFFE5484D),
        Color(0xFFFFC53D),
    )

    val gradients = listOf(
        listOf(Color(0xFF23262E), Color(0xFF0E0F13)),
        listOf(Color(0xFFFFE2C0), Color(0xFFFFC9A3)),
        listOf(Color(0xFFB8E0FF), Color(0xFF7FA8E8)),
        listOf(Color(0xFFFFD1DC), Color(0xFFE8A0B4)),
        listOf(Color(0xFFD6F5E3), Color(0xFF9ADFC0)),
        listOf(Color(0xFFF7F7F5), Color(0xFFD8D3C8)),
    )

    fun argb(color: Color): Int = color.toArgb()
}
