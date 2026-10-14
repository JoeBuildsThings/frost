package com.joebuildsthings.frost.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(14.dp, shape, ambientColor = GlassShadow, spotColor = GlassShadow)
    .clip(shape)
    .background(Brush.verticalGradient(listOf(GlassTop, GlassBottom)))
    .border(1.dp, Brush.linearGradient(listOf(GlassEdgeStrong, GlassEdgeFade)), shape)

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

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .drawBehind {
                blob(BlobBlue, size.width * 0.10f, size.height * 0.10f, size.width * 0.80f)
                blob(BlobMint, size.width * 0.95f, size.height * 0.45f, size.width * 0.65f)
                blob(BlobPeach, size.width * 0.20f, size.height * 0.95f, size.width * 0.75f)
            },
        content = content
    )
}
