package com.geno1024.pictureframe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.geno1024.pictureframe.io.ExportFormat
import com.geno1024.pictureframe.model.Aspect
import com.geno1024.pictureframe.model.BackgroundType
import com.geno1024.pictureframe.model.EditorState
import com.geno1024.pictureframe.model.FrameStyle
import com.geno1024.pictureframe.model.Resolution
import com.geno1024.pictureframe.model.Swatches
import com.geno1024.pictureframe.render.Layout
import com.geno1024.pictureframe.ui.components.ChipRow
import com.geno1024.pictureframe.ui.components.ColorRow
import com.geno1024.pictureframe.ui.components.GradientRow
import com.geno1024.pictureframe.ui.components.LabeledSlider
import com.geno1024.pictureframe.ui.components.PrimaryAction
import com.geno1024.pictureframe.ui.components.SectionLabel

private val WIDTH_RANGE = 0f..0.12f
private val MAT_RANGE = 0f..0.16f
private val RADIUS_RANGE = 0f..0.10f

private val COLOR_STYLES = setOf(FrameStyle.Solid, FrameStyle.Polaroid, FrameStyle.Bevel, FrameStyle.Inset)

@Composable
fun FramePanel(state: EditorState, onEdit: ((EditorState) -> EditorState) -> Unit) {
    val frame = state.frame
    val active = frame.style != FrameStyle.None

    Panel {
        SectionLabel("样式")
        ChipRow(FrameStyle.entries, frame.style, { it.label }) { style ->
            onEdit { it.withFrame { f -> f.copy(style = style) } }
        }

        if (!active) return@Panel

        LabeledSlider("边框宽度", frame.width, WIDTH_RANGE, { v ->
            onEdit { it.withFrame { f -> f.copy(width = v) } }
        })
        LabeledSlider("卡纸", frame.matWidth, MAT_RANGE, { v ->
            onEdit { it.withFrame { f -> f.copy(matWidth = v) } }
        })
        LabeledSlider("圆角", frame.cornerRadius, RADIUS_RANGE, { v ->
            onEdit { it.withFrame { f -> f.copy(cornerRadius = v) } }
        })
        LabeledSlider("投影", frame.shadow, 0f..1f, { v ->
            onEdit { it.withFrame { f -> f.copy(shadow = v) } }
        }, valueLabel = percent(frame.shadow))

        if (frame.style in COLOR_STYLES) {
            SectionLabel("边框颜色")
            ColorRow(Swatches.solid, frame.color, onSelect = { c ->
                onEdit { it.withFrame { f -> f.copy(color = c) } }
            })
        }
        if (frame.style == FrameStyle.Double) {
            SectionLabel("内线颜色")
            ColorRow(Swatches.solid, frame.edgeColor, onSelect = { c ->
                onEdit { it.withFrame { f -> f.copy(edgeColor = c) } }
            })
        }
        if (frame.matWidth > 0f) {
            SectionLabel("卡纸颜色")
            ColorRow(Swatches.solid, frame.matColor, onSelect = { c ->
                onEdit { it.withFrame { f -> f.copy(matColor = c) } }
            })
        }
    }
}

@Composable
fun BackgroundPanel(state: EditorState, onEdit: ((EditorState) -> EditorState) -> Unit) {
    val background = state.background

    Panel {
        SectionLabel("类型")
        ChipRow(BackgroundType.entries, background.type, { it.label }) { type ->
            onEdit { it.withBackground { b -> b.copy(type = type) } }
        }

        when (background.type) {
            BackgroundType.Solid -> {
                SectionLabel("颜色")
                ColorRow(Swatches.solid, background.colors.firstOrNull(), onSelect = { c ->
                    onEdit { it.withBackground { b -> b.copy(colors = listOf(c)) } }
                })
            }

            BackgroundType.Gradient -> {
                SectionLabel("渐变")
                GradientRow(Swatches.gradients, background.colors.takeIf { it.size > 1 }) { stops ->
                    onEdit { it.withBackground { b -> b.copy(colors = stops) } }
                }
                ChipRow(listOf(false, true), background.radial, { if (it) "径向" else "线性" }) { radial ->
                    onEdit { it.withBackground { b -> b.copy(radial = radial) } }
                }
                LabeledSlider("角度", background.angle, 0f..360f, { v ->
                    onEdit { it.withBackground { b -> b.copy(angle = v) } }
                }, valueLabel = "${background.angle.toInt()}°")
            }

            BackgroundType.Blurred -> LabeledSlider(
                label = "虚化程度",
                value = background.blurStrength,
                range = 0f..1f,
                onChange = { v -> onEdit { it.withBackground { b -> b.copy(blurStrength = v) } } },
                valueLabel = percent(background.blurStrength),
            )

            BackgroundType.None -> Unit
        }

        if (background.type != BackgroundType.None) {
            LabeledSlider("压暗", background.dim, 0f..0.7f, { v ->
                onEdit { it.withBackground { b -> b.copy(dim = v) } }
            }, valueLabel = percent(background.dim))
        }
    }
}

@Composable
fun CanvasPanel(state: EditorState, onEdit: ((EditorState) -> EditorState) -> Unit) {
    Panel {
        SectionLabel("画幅比例")
        ChipRow(Aspect.entries, state.aspect, { it.label }) { aspect ->
            onEdit { it.copy(aspect = aspect) }
        }
        SectionLabel("四周留白")
        LabeledSlider("留白", state.margin, 0f..0.2f, { v ->
            onEdit { it.copy(margin = v) }
        }, valueLabel = percent(state.margin))
    }
}

@Composable
fun ExportPanel(
    state: EditorState,
    format: ExportFormat,
    photoAspect: Float,
    photoLongEdge: Int,
    exporting: Boolean,
    onResolution: (Resolution) -> Unit,
    onFormat: (ExportFormat) -> Unit,
    onExport: () -> Unit,
) {
    val canvas = Layout.canvasSize(state, photoAspect, photoLongEdge)
    Panel {
        SectionLabel("输出尺寸")
        Text(
            text = "${canvas.width.toInt()} × ${canvas.height.toInt()} px",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionLabel("分辨率")
        ChipRow(Resolution.entries, state.resolution, { it.label }, onSelect = onResolution)
        SectionLabel("格式")
        ChipRow(ExportFormat.entries, format, { it.label }, onSelect = onFormat)
        PrimaryAction(
            text = if (exporting) "保存中…" else "保存到相册",
            onClick = onExport,
            enabled = !exporting,
            modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

private fun percent(value: Float) = "${(value * 100).toInt()}%"
