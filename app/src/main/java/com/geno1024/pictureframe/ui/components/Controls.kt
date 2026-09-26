package com.geno1024.pictureframe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: String = "%.2f".format(value),
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.height(28.dp),
        )
    }
}

@Composable
fun <T> ChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            ChoiceChip(
                label = label(item),
                selected = item == selected,
                onClick = { onSelect(item) },
            )
        }
    }
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "chipBackground",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "chipContent",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun SegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "tabBackground",
            )
            val content by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ColorRow(
    colors: List<Color>,
    selected: Color?,
    modifier: Modifier = Modifier,
    onSelect: (Color) -> Unit,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(colors) { color -> Swatch(color, color == selected) { onSelect(color) } }
    }
}

@Composable
fun GradientRow(
    gradients: List<List<Color>>,
    selected: List<Color>?,
    modifier: Modifier = Modifier,
    onSelect: (List<Color>) -> Unit,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(gradients) { stops -> Swatch(stops, stops == selected) { onSelect(stops) } }
    }
}

@Composable
private fun Swatch(stops: Any, selected: Boolean, onClick: () -> Unit) {
    val brush = when (stops) {
        is List<*> -> Brush.linearGradient(stops.filterIsInstance<Color>())
        is Color -> Brush.linearGradient(listOf(stops, stops))
        else -> Brush.linearGradient(listOf(Color.Gray, Color.Gray))
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(brush)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
fun OutlineAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}
