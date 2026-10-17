package com.dashdazz.light

import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Task 6: System Automations & Triggers.
 *
 * Includes:
 * 1. Quiet Hours (Night Schedule) Engine with overnight wrap-around.
 * 2. DND / Silent Mode detection & dim/mute policy.
 * 3. Pocket / Proximity Guard to suppress LED when device is face-down or covered.
 */
object SystemAutomationsManager {
    private const val TAG = "SystemAutomations"

    /**
     * Checks if current time is within configured quiet hours.
     * E.g. startHour=23, startMinute=0, endHour=7, endMinute=0
     * Handles overnight periods cleanly.
     */
    fun isWithinQuietHours(
        currentHour: Int,
        currentMinute: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes < endMinutes) {
            // Same day range (e.g. 09:00 to 17:00)
            currentMinutes in startMinutes until endMinutes
        } else if (startMinutes > endMinutes) {
            // Overnight range (e.g. 23:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        } else {
            // Same start and end means disabled
            false
        }
    }

    fun isQuietHoursActiveNow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val cal = Calendar.getInstance()
        val curHour = cal.get(Calendar.HOUR_OF_DAY)
        val curMin = cal.get(Calendar.MINUTE)
        return isWithinQuietHours(curHour, curMin, startHour, startMinute, endHour, endMinute)
    }

    /**
     * Evaluates if DND or Silent Mode is active.
     */
    fun isDndOrSilentActive(context: Context): Boolean {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val ringer = audioManager.ringerMode
                if (ringer == AudioManager.RINGER_MODE_SILENT || ringer == AudioManager.RINGER_MODE_VIBRATE) {
                    return true
                }
            }

            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notifManager != null) {
                val filter = notifManager.currentInterruptionFilter
                if (filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                ) {
                    return true
                }
            }

            val zenMode = Settings.Global.getInt(context.contentResolver, "zen_mode", 0)
            if (zenMode != 0) return true
        } catch (e: Throwable) {
            Log.d(TAG, "DND check warning: ${e.message}")
        }
        return false
    }

    class PocketGuard(
        context: Context,
        private val onStateChanged: (isCovered: Boolean) -> Unit
    ) : SensorEventListener {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        var isCovered: Boolean = false
            private set

        fun start() {
            proximitySensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        fun stop() {
            sensorManager?.unregisterListener(this)
            isCovered = false
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val covered = distance < maxRange && distance < 5.0f
            if (covered != isCovered) {
                isCovered = covered
                Log.d(TAG, "Proximity covered state changed: $isCovered")
                onStateChanged(isCovered)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
