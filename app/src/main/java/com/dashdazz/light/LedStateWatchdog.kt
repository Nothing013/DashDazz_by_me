package com.dashdazz.light

import android.util.Log
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Task 2: Root cause fix & internal watchdog guard for stuck-on LED.
 *
 * 1. LedEventLogger: Circular in-memory audit log recording state transitions,
 *    trigger activations/deactivations, and hardware OFF dispatches.
 *
 * 2. LedStateWatchdog: Periodically and reactively verifies that if no triggers
 *    are active or screen policy demands OFF, the hardware LED is confirmed OFF.
 *    If an unhandled cancellation or dropped root shell command left the hardware
 *    in an ON state, the Watchdog immediately forces hardwareOff().
 */
object LedEventLogger {
    private const val MAX_LOGS = 100
    private val logs = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile
    var isEnabled: Boolean = false

    fun log(tag: String, message: String) {
        if (!isEnabled) return
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp][$tag] $message"
        Log.d("DashDazzWatchdog", entry)
        logs.add(entry)
        while (logs.size > MAX_LOGS) {
            logs.poll()
        }
    }

    fun getLogs(): List<String> = logs.toList()

    fun clear() {
        logs.clear()
    }
}

class LedStateWatchdog(
    private val isExpectedActive: () -> Boolean,
    private val onForceOffNeeded: () -> Unit
) {
    companion object {
        private const val TAG = "LedStateWatchdog"
        private const val CHECK_INTERVAL_MS = 2500L
    }

    private var watchdogJob: Job? = null

    fun start(scope: CoroutineScope) {
        stop()
        watchdogJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                verifyState()
            }
        }
        LedEventLogger.log(TAG, "Watchdog started")
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        LedEventLogger.log(TAG, "Watchdog stopped")
    }

    fun verifyState() {
        val expectedActive = isExpectedActive()
        val hwA = LightsBridge.lastHwColorA
        val hwB = LightsBridge.lastHwColorB
        val isHardwareLit = (hwA and 0x00FFFFFF != 0) || (hwB and 0x00FFFFFF != 0)

        if (!expectedActive && isHardwareLit) {
            LedEventLogger.log(
                TAG,
                "DISCREPANCY DETECTED! Expected=OFF, HW=LIT (A=0x${Integer.toHexString(hwA)}, B=0x${Integer.toHexString(hwB)}). Forcing OFF!"
            )
            onForceOffNeeded()
        }
    }
}
