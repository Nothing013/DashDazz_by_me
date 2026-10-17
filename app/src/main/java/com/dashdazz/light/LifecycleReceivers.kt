package com.dashdazz.light

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

object LifecycleReceivers {

    private const val TAG = "DashDazz"

    @Volatile
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: return
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> DashDazzService.onScreenEvent(ctx, false)
                Intent.ACTION_SCREEN_ON -> DashDazzService.onScreenEvent(ctx, true)
                Intent.ACTION_POWER_CONNECTED -> DashDazzService.onPowerEvent(ctx, true)
                Intent.ACTION_POWER_DISCONNECTED -> DashDazzService.onPowerEvent(ctx, false)
            }
        }
    }

    fun register(context: Context) {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    receiver, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.applicationContext.registerReceiver(receiver, filter)
            }
            registered = true
        } catch (e: Throwable) {
            Log.w(TAG, "Lifecycle receiver registration failed: ${e.message}")
        }
    }
}

fun registerLifecycleReceivers(context: Context) {
    LifecycleReceivers.register(context)
}