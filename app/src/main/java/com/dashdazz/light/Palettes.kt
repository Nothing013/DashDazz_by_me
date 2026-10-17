package com.dashdazz.light

import androidx.annotation.StringRes

data class RainbowPalette(
    val id: Int,
    @StringRes val nameRes: Int,
    val colors: IntArray?
)

object Palettes {
    const val SPECTRUM = 0
    const val CYBERPUNK = 1
    const val SUNSET = 2
    const val OCEAN = 3
    const val PASTEL = 4
    const val FIRE = 5

    val ALL = listOf(
        RainbowPalette(
            id = SPECTRUM,
            nameRes = R.string.palette_spectrum,
            colors = null
        ),
        RainbowPalette(
            id = CYBERPUNK,
            nameRes = R.string.palette_cyberpunk,
            colors = intArrayOf(
                0xFFFF007F.toInt(),
                0xFF7928CA.toInt(),
                0xFF0070F3.toInt(),
                0xFF00DFD8.toInt()
            )
        ),
        RainbowPalette(
            id = SUNSET,
            nameRes = R.string.palette_sunset,
            colors = intArrayOf(
                0xFFFF1744.toInt(),
                0xFFFF6D00.toInt(),
                0xFFFFD600.toInt(),
                0xFFE040FB.toInt()
            )
        ),
        RainbowPalette(
            id = OCEAN,
            nameRes = R.string.palette_ocean,
            colors = intArrayOf(
                0xFF00E5FF.toInt(),
                0xFF1DE9B6.toInt(),
                0xFF00E676.toInt(),
                0xFF2979FF.toInt()
            )
        ),
        RainbowPalette(
            id = PASTEL,
            nameRes = R.string.palette_pastel,
            colors = intArrayOf(
                0xFFFF4081.toInt(),
                0xFF7C4DFF.toInt(),
                0xFF00E5FF.toInt(),
                0xFF00E676.toInt(),
                0xFFFF6E40.toInt()
            )
        ),
        RainbowPalette(
            id = FIRE,
            nameRes = R.string.palette_fire,
            colors = intArrayOf(
                0xFFFF0000.toInt(),
                0xFFFF3D00.toInt(),
                0xFFFF9100.toInt(),
                0xFFFFEA00.toInt()
            )
        )
    )

    private fun sanitize(argb: Int): Int =
        if (argb == -1) 0xFFFEFEFE.toInt() else argb

    fun interpolate(
        c1: Int,
        c2: Int,
        ratio: Float,
        brightness: Float,
        saturationScale: Float = 1f
    ): Int {
        val t = ratio.coerceIn(0f, 1f)
        val bright = if (brightness.isNaN()) 1f else brightness
        val satScale = if (saturationScale.isNaN()) 1f else saturationScale
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        android.graphics.Color.colorToHSV(c1, hsv1)
        android.graphics.Color.colorToHSV(c2, hsv2)

        val achromatic = hsv1[1] < 0.02f || hsv2[1] < 0.02f
        var h: Float
        if (achromatic) {
            h = if (hsv1[1] >= hsv2[1]) hsv1[0] else hsv2[0]
        } else {

            var diff = hsv2[0] - hsv1[0]
            if (diff > 180f) diff -= 360f
            else if (diff < -180f) diff += 360f
            h = (hsv1[0] + diff * t) % 360f
            if (h < 0f) h += 360f
        }

        val s = (((hsv1[1] + (hsv2[1] - hsv1[1]) * t) * satScale)).coerceIn(0f, 1f)
        val v = ((hsv1[2] + (hsv2[2] - hsv1[2]) * t) * bright).coerceIn(0f, 1f)

        val out = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))

        return sanitize(out)
    }
}
