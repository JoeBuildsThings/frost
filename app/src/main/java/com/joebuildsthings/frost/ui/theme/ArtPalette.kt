package com.joebuildsthings.frost.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.joebuildsthings.frost.data.ArtCache

object ArtPalette {

    private val cache = HashMap<Long, List<Color>>()

    fun colorsFor(context: Context, albumId: Long): List<Color>? {
        synchronized(cache) {
            cache[albumId]?.let { return it }
        }
        val bitmap = ArtCache.load(context, albumId, 96) ?: return null
        val small = Bitmap.createScaledBitmap(bitmap, 2, 2, true)
        val out = ArrayList<Color>(4)
        for (y in 0 until 2) {
            for (x in 0 until 2) {
                out.add(soften(small.getPixel(x, y)))
            }
        }
        if (small !== bitmap) small.recycle()
        synchronized(cache) { cache[albumId] = out }
        return out
    }

    private fun soften(argb: Int): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = hsv[1].coerceIn(0.18f, 0.55f)
        hsv[2] = hsv[2].coerceAtLeast(0.88f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}
