package com.joebuildsthings.frost.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
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
    tints = listOf(HazeTint(Color.White.copy(alpha = 0.55f))),
    blurRadius = 26.dp,
    noiseFactor = 0.04f,
    fallbackTint = HazeTint(Color.White.copy(alpha = 0.92f))
)

fun Modifier.glassEdge(shape: Shape): Modifier =
    this.border(1.dp, Brush.linearGradient(listOf(GlassEdgeStrong, GlassEdgeFade)), shape)

fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(14.dp, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
    .glassEdge(shape)

fun Modifier.glassFlat(shape: Shape = RoundedCornerShape(20.dp)): Modifier = this
    .clip(shape)
    .background(GlassFlat)
    .border(1.dp, GlassEdgeFade, shape)

private fun DrawScope.blob(color: Color, cx: Float, cy: Float, r: Float) {
    val c = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = r),
        radius = r,
        center = c
    )
}

fun Modifier.glowBackground(): Modifier = this
    .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    .drawBehind {
        blob(BlobBlue, size.width * 0.10f, size.height * 0.08f, size.width * 0.85f)
        blob(BlobMint, size.width * 0.98f, size.height * 0.42f, size.width * 0.70f)
        blob(BlobPeach, size.width * 0.15f, size.height * 0.70f, size.width * 0.75f)
        blob(BlobSky, size.width * 0.90f, size.height * 0.96f, size.width * 0.70f)
    }
