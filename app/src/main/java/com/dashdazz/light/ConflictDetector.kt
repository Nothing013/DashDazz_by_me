package com.dashdazz.light

import android.content.Context
import android.provider.Settings
import android.util.Log

object ConflictDetector {
    private const val TAG = "DashDazz"

    fun isStockMusicLightActive(context: Context): Boolean {

        try {
            val clazz = Class.forName("android.app.ColorLightManager")
            val ctor = clazz.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            val mgr = ctor.newInstance(context)
            val method = clazz.getMethod("isMusicLightEnable")
            val result = method.invoke(mgr) as? Boolean
            if (result == true) {
                return true
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Reflection check on ColorLightManager not available: ${e.message}")
        }

        try {
            val cr = context.contentResolver
            if (Settings.System.getInt(cr, "music_light_enable", 0) == 1) return true
            if (Settings.Global.getInt(cr, "settings_strip_light_music_enable", 0) == 1) return true
            if (Settings.System.getInt(cr, "settings_strip_light_music_enable", 0) == 1) return true
        } catch (e: Throwable) {
            Log.d(TAG, "Settings check for music light failed: ${e.message}")
        }

        return false
    }
}
