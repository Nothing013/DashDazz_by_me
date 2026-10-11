package com.dashdazz.light

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallLightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) !=
            TelephonyManager.EXTRA_STATE_RINGING
        ) {
            return
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        when (Prefs.getCallMode(context)) {
            Prefs.NOTIF_MODE_BLINK -> {
                Log.d(TAG, "Incoming call: pulsing ring")
                DashDazzService.pulseNotification(context, Prefs.getColor(context))
            }
            Prefs.NOTIF_MODE_ACTIVATE -> {
                val durationSec = Prefs.getNotificationActivateDuration(context)
                Log.d(TAG, "Incoming call: activating lights for ${durationSec}s")
                DashDazzService.activateForNotification(context, durationSec)
            }
            else -> {}
        }
    }

    companion object {
        private const val TAG = "DashDazzCall"
    }
}
