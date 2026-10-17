package com.dashdazz.light

import android.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Task 1: Extended Lighting Effects Library for DashDazz Hyper-Glow Engine.
 *
 * Provides algorithmic generators for:
 * - SPECTRUM_WAVE: Smooth rotational rainbow wave
 * - STROBE: High-energy flashing bursts
 * - CANDLE_FLICKER: Organic randomized warm candle simulation
 * - NEON_CYBERPUNK: Dual-tone alternating cyan/magenta cycles
 * - COMET_CHASER: Sweeping orbital segment around camera ring
 * - BREATHE_FADE: Gentle expanding pulse with smooth alpha transitions
 */
object ExtendedLightingEffects {
    const val EFFECT_SPECTRUM_WAVE = 0
    const val EFFECT_STROBE = 1
    const val EFFECT_CANDLE_FLICKER = 2
    const val EFFECT_NEON_CYBERPUNK = 3
    const val EFFECT_COMET_CHASER = 4
    const val EFFECT_BREATHE_FADE = 5

    data class EffectFrame(
        val colorA: Int,
        val colorB: Int = colorA,
        val brightness: Float = 1.0f,
        val delayMs: Long = 20L
    )

    private val random = Random(42)

    /**
     * Generates a frame for the Candle Flicker effect.
     * Uses baseline amber/warm gold with pseudo-random perlin-like turbulence.
     */
    fun getCandleFlickerFrame(tick: Long, baseBrightness: Float): EffectFrame {
        val flicker = 0.65f + 0.35f * sin(tick * 0.15f) * cos(tick * 0.08f) + (random.nextFloat() * 0.15f - 0.075f)
        val bright = (baseBrightness * flicker).coerceIn(0.05f, 1.0f)
        // Warm candle: 0xFFFF8C00 (DarkOrange) / 0xFFFFB300 (Amber)
        val warmColor = Palettes.interpolate(0xFFFF6F00.toInt(), 0xFFFFD54F.toInt(), flicker.coerceIn(0f, 1f), bright)
        return EffectFrame(warmColor, warmColor, bright, 25L)
    }

    /**
     * Generates a frame for the Neon Cyberpunk effect.
     * Alternates dynamically between Electric Neon Cyan (0xFF00E5FF) and Hot Magenta (0xFFFF007F).
     */
    fun getNeonCyberpunkFrame(phase: Float, baseBrightness: Float): EffectFrame {
        val t = (sin(phase.toDouble()).toFloat() + 1f) / 2f
        val colorA = Palettes.interpolate(0xFF00E5FF.toInt(), 0xFFFF007F.toInt(), t, baseBrightness)
        val colorB = Palettes.interpolate(0xFFFF007F.toInt(), 0xFF00E5FF.toInt(), t, baseBrightness)
        return EffectFrame(colorA, colorB, baseBrightness, 20L)
    }

    /**
     * Generates a high-intensity Strobe frame.
     */
    fun getStrobeFrame(step: Int, color: Int, baseBrightness: Float): EffectFrame {
        val on = (step % 2 == 0)
        val bright = if (on) baseBrightness else 0.0f
        val c = if (on) color else Color.BLACK
        return EffectFrame(c, c, bright, 35L)
    }

    /**
     * Generates a Comet / Chaser frame.
     */
    fun getCometChaserFrame(angleDeg: Float, primaryColor: Int, baseBrightness: Float): EffectFrame {
        val ratio = (angleDeg % 360f) / 360f
        val fadeColor = Palettes.interpolate(primaryColor, Color.BLACK, 0.7f, baseBrightness * 0.3f)
        return EffectFrame(primaryColor, fadeColor, baseBrightness, 18L)
    }
}
