package com.dashdazz.light

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log

/**
 * Task 5: Per-App Media Allowlist & Audio Visualizer Engine enhancements.
 *
 * Allows React to Audio to trigger either globally for all system media
 * or exclusively for allowlisted music/video apps (e.g. Spotify, YouTube Music, ViMusic).
 */
object MediaAllowlistManager {
    private const val TAG = "MediaAllowlist"

    const val MODE_ALL_AUDIO = 0
    const val MODE_ALLOWLIST_ONLY = 1

    /**
     * Determines whether audio reactive effects should be enabled based on
     * active media controllers and user preference.
     */
    fun shouldAudioTriggerForPackages(
        activePlayingPackages: Set<String>,
        filterMode: Int,
        allowlistedApps: Set<String>
    ): Boolean {
        // If mode is all audio, allow any playing media or fallback
        if (filterMode == MODE_ALL_AUDIO) {
            return true
        }

        // Allowlist only: at least one currently active playing package must be in allowlistedApps
        if (allowlistedApps.isEmpty()) {
            return false
        }

        return activePlayingPackages.any { it in allowlistedApps }
    }

    /**
     * Computes color shift degrees based on volume amplitude and smoothing factor.
     */
    fun computeDynamicColorShift(
        currentHue: Float,
        amplitude: Float,
        shiftSpeedMultiplier: Float,
        stepDelta: Float
    ): Float {
        val speed = shiftSpeedMultiplier.coerceIn(0.2f, 3.0f)
        val delta = (stepDelta * speed * (0.5f + amplitude * 1.5f))
        return (currentHue + delta) % 360f
    }
}
