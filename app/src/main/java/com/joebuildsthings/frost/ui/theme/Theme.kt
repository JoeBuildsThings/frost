package com.joebuildsthings.frost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrostColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = BgTop,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = InkMuted
)

@Composable
fun FrostTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FrostColors, content = content)
}
