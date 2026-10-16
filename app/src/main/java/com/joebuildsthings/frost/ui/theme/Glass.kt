package com.joebuildsthings.frost.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

val BarHazeStyle = HazeStyle(
    backgroundColor = Color.White,
    tints = listOf(HazeTint(Color.White.copy(alpha = 0.50f))),
    blurRadius = 20.dp,
    noiseFactor = 0.02f,
    fallbackTint = HazeTint(Color.White.copy(alpha = 0.92f))
)

val PanelHazeStyle = HazeStyle(
    backgroundColor = Color.White,
    tints = listOf(HazeTint(Color.White.copy(alpha = 0.34f))),
    blurRadius = 28.dp,
    noiseFactor = 0.02f,
    fallbackTint = HazeTint(Color.White.copy(alpha = 0.88f))
)

private val RimBrush = Brush.linearGradient(
    listOf(Color(0xFFFFFFFF), Color(0x33FFFFFF), Color(0x99FFFFFF))
)

fun Modifier.glassEdge(shape: Shape): Modifier = this.border(1.dp, RimBrush, shape)

fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(14.dp, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xCCFFFFFF), Color(0x80FFFFFF))))
    .glassEdge(shape)

fun Modifier.glassFlat(shape: Shape = RoundedCornerShape(20.dp)): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xBFFFFFFF), Color(0x80FFFFFF))))
    .glassEdge(shape)

private fun DrawScope.blob(color: Color, cx: Float, cy: Float, r: Float) {
    val c = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = r),
        radius = r,
        center = c
    )
}

@Composable
fun Modifier.dynamicGlow(colors: List<Color>?): Modifier {
    val spec = tween<Color>(durationMillis = 900)
    val c0 by animateColorAsState(colors?.getOrNull(0) ?: BlobBlue, spec, label = "glow0")
    val c1 by animateColorAsState(colors?.getOrNull(1) ?: BlobMint, spec, label = "glow1")
    val c2 by animateColorAsState(colors?.getOrNull(2) ?: BlobPeach, spec, label = "glow2")
    val c3 by animateColorAsState(colors?.getOrNull(3) ?: BlobSky, spec, label = "glow3")
    return this
        .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
        .drawBehind {
            blob(c0, size.width * 0.10f, size.height * 0.08f, size.width * 0.90f)
            blob(c1, size.width * 0.98f, size.height * 0.42f, size.width * 0.75f)
            blob(c2, size.width * 0.15f, size.height * 0.70f, size.width * 0.80f)
            blob(c3, size.width * 0.90f, size.height * 0.96f, size.width * 0.75f)
        }
}
