package com.dashdazz.light

import org.junit.Assert.*
import org.junit.Test

class AudioAllowlistTest {

    @Test
    fun testAllAudioModeAlwaysPermitsTrigger() {
        val playingApps = setOf("com.spotify.music")
        val allowlisted = setOf("com.google.android.apps.youtube.music")

        val allowed = MediaAllowlistManager.shouldAudioTriggerForPackages(
            activePlayingPackages = playingApps,
            filterMode = MediaAllowlistManager.MODE_ALL_AUDIO,
            allowlistedApps = allowlisted
        )
        assertTrue(allowed)
    }

    @Test
    fun testAllowlistOnlyModePermitsMatchedApp() {
        val playingApps = setOf("com.spotify.music")
        val allowlisted = setOf("com.spotify.music", "vimusic.app")

        val allowed = MediaAllowlistManager.shouldAudioTriggerForPackages(
            activePlayingPackages = playingApps,
            filterMode = MediaAllowlistManager.MODE_ALLOWLIST_ONLY,
            allowlistedApps = allowlisted
        )
        assertTrue(allowed)
    }

    @Test
    fun testAllowlistOnlyModeRejectsUnmatchedApp() {
        val playingApps = setOf("com.random.unwanted.sound")
        val allowlisted = setOf("com.spotify.music", "com.google.android.apps.youtube.music")

        val allowed = MediaAllowlistManager.shouldAudioTriggerForPackages(
            activePlayingPackages = playingApps,
            filterMode = MediaAllowlistManager.MODE_ALLOWLIST_ONLY,
            allowlistedApps = allowlisted
        )
        assertFalse(allowed)
    }

    @Test
    fun testDynamicColorShiftCalculation() {
        val initialHue = 100f
        val shiftedHue = MediaAllowlistManager.computeDynamicColorShift(
            currentHue = initialHue,
            amplitude = 0.8f,
            shiftSpeedMultiplier = 1.0f,
            stepDelta = 10f
        )
        assertTrue(shiftedHue > initialHue)
        assertTrue(shiftedHue <= 360f)
    }
}
