package com.geno1024.pictureframe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Paper,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Ink,
    secondary = InkSoft,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkSoft,
    outline = InkLine,
    outlineVariant = PaperDim,
    error = Danger,
    onError = Paper,
)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Paper,
    primaryContainer = InkSoft,
    onPrimaryContainer = AccentSoft,
    secondary = PaperDim,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkSoft,
    onSurfaceVariant = PaperDim,
    outline = InkLine,
    outlineVariant = InkSoft,
    error = Danger,
    onError = Paper,
)

private val AppTypography = Typography(
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun PictureFrameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
