package com.joebuildsthings.frost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FrostScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = BgDeep,
    background = BgDeep,
    onBackground = InkBright,
    surface = BgLift,
    onSurface = InkBright,
    outline = GlassBorderSoft,
)

@Composable
fun FrostTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FrostScheme, content = content)
}
