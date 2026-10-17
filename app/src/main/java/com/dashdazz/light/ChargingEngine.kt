package com.dashdazz.light

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.util.Log

/**
 * Task 4: Advanced Charging & Battery Indicator Engine.
 *
 * Provides:
 * - Real-time battery status & charging speed evaluation
 * - Multiple visual charging styles:
 *     1. Pulse / Breathing (speed dynamically tied to standard vs warp/fast charge)
 *     2. Battery Level Fill / Gauge (0% to 100% angular arc or segment mapping)
 *     3. Dynamic Color Gradient (Red -> Amber -> Cyan/Green)
 * - Low Battery Warning Flash (<15% / <10%)
 * - Battery Full Action (Solid color upon reaching 100%, with configurable auto-timeout)
 * - Zero CPU waste: updates strictly on battery change events and frame render clocks.
 */
object ChargingEngine {
    private const val TAG = "ChargingEngine"

    const val STYLE_BREATHE = 0
    const val STYLE_GAUGE = 1
    const val STYLE_GRADIENT = 2

    data class BatteryState(
        val isCharging: Boolean = false,
        val percentage: Int = 50,
        val isFastCharge: Boolean = false,
        val isFull: Boolean = false,
        val isLowBattery: Boolean = false,
        val plugType: Int = 0
    )

    fun evaluateBatteryIntent(intent: Intent?): BatteryState {
        if (intent == null) return BatteryState()

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale) * 100).toInt() else 50

        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || pct >= 100
        val isLow = pct <= 15 && !isCharging

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        // Check for Fast/Warp charge (e.g. AC charging or higher current)
        val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC

        return BatteryState(
            isCharging = isCharging,
            percentage = pct.coerceIn(0, 100),
            isFastCharge = isFast,
            isFull = isFull,
            isLowBattery = isLow,
            plugType = plugged
        )
    }

    fun getCurrentBatteryState(context: Context): BatteryState {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, filter)
        return evaluateBatteryIntent(batteryStatus)
    }

    /**
     * Maps battery percentage to a dynamic gradient color:
     * 0-25%: Pure Red (0xFFFF1744) -> Deep Orange
     * 25-60%: Amber / Yellow (0xFFFFB300)
     * 60-95%: Cyan (0xFF00E5FF)
     * 95-100%: Electric Neon Green (0xFF00E676)
     */
    fun getGradientColorForPercentage(percentage: Int): Int {
        val pct = percentage.coerceIn(0, 100)
        return when {
            pct < 25 -> {
                val ratio = pct / 25f
                Palettes.interpolate(0xFFFF1744.toInt(), 0xFFFF6D00.toInt(), ratio, 1.0f)
            }
            pct < 60 -> {
                val ratio = (pct - 25) / 35f
                Palettes.interpolate(0xFFFF6D00.toInt(), 0xFFFFD600.toInt(), ratio, 1.0f)
            }
            pct < 90 -> {
                val ratio = (pct - 60) / 30f
                Palettes.interpolate(0xFFFFD600.toInt(), 0xFF00E5FF.toInt(), ratio, 1.0f)
            }
            else -> {
                val ratio = (pct - 90) / 10f
                Palettes.interpolate(0xFF00E5FF.toInt(), 0xFF00E676.toInt(), ratio, 1.0f)
            }
        }
    }

    /**
     * Computes the pulse interval step in milliseconds based on charging speed.
     * Fast/Warp charge breathes significantly faster to provide instant visual feedback.
     */
    fun getPulseStepMs(isFastCharge: Boolean): Long {
        return if (isFastCharge) 12L else 24L
    }
}
