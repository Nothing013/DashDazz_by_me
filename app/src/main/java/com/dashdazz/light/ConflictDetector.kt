package com.dashdazz.light

import android.content.Context
import android.provider.Settings
import android.util.Log

object ConflictDetector {
    private const val TAG = "DashDazz"

    fun isStockMusicLightActive(context: Context): Boolean {

        try {
            val clazz = Class.forName("android.app.ColorLightManager")
            val appCtx = context.applicationContext ?: context
            val ctor = clazz.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            val mgr = ctor.newInstance(appCtx)
            for (name in arrayOf("isMusicLightEnable", "isMusicLightEnabled", "getMusicLightEnable")) {
                try {
                    val method = clazz.getMethod(name)
                    val result = method.invoke(mgr) as? Boolean
                    if (result == true) return true
                } catch (_: NoSuchMethodException) {
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Reflection check on ColorLightManager not available: ${e.message}")
        }

        try {
            val cr = context.contentResolver
            fun truthy(ns: String, key: String): Boolean {
                return try {
                    when (ns) {
                        "system" -> {
                            try {
                                Settings.System.getInt(cr, key, 0) != 0
                            } catch (_: Settings.SettingNotFoundException) {
                                Settings.System.getString(cr, key)?.lowercase() == "true"
                            }
                        }
                        "global" -> {
                            try {
                                Settings.Global.getInt(cr, key, 0) != 0
                            } catch (_: Settings.SettingNotFoundException) {
                                Settings.Global.getString(cr, key)?.lowercase() == "true"
                            }
                        }
                        else -> {
                            try {
                                Settings.Secure.getInt(cr, key, 0) != 0
                            } catch (_: Settings.SettingNotFoundException) {
                                Settings.Secure.getString(cr, key)?.lowercase() == "true"
                            }
                        }
                    }
                } catch (_: Throwable) {
                    false
                }
            }
            if (truthy("global", "settings_strip_light_enable")) {

                val rhythmicApp = try {
                    Settings.Global.getString(cr, "musical_rhythm_app")
                        ?: Settings.System.getString(cr, "musical_rhythm_app")
                } catch (_: Throwable) {
                    null
                }
                if (!rhythmicApp.isNullOrEmpty()) return true
            }
            val legacyKeys = arrayOf(
                "system|music_light_enable",
                "global|settings_strip_light_music_enable",
                "system|settings_strip_light_music_enable",
                "system|strip_light_music_enable",
                "secure|strip_light_music_enable",
                "global|strip_light_music_enable"
            )
            for (entry in legacyKeys) {
                val ns = entry.substringBefore("|")
                val key = entry.substringAfter("|")
                if (truthy(ns, key)) return true
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Settings check for music light failed: ${e.message}")
        }

        return false
    }
}
