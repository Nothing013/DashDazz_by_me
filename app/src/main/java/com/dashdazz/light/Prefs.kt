package com.dashdazz.light

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object Prefs {
    private const val PREFS_NAME = "dashdazz_prefs"

    private const val KEY_MODE = "key_mode"
    private const val KEY_COLOR = "key_color"
    private const val KEY_SPEED = "key_speed"
    private const val KEY_BRIGHTNESS = "key_brightness"
    private const val KEY_PAUSE_ON_SCREEN_OFF = "key_pause_screen_off"
    private const val KEY_SCREEN_MODE = "key_screen_mode"
    private const val KEY_BLINK_ON_NOTIFICATION = "key_blink_notification"
    private const val KEY_PALETTE = "key_palette"
    private const val KEY_DARK_MODE = "key_dark_mode"
    private const val KEY_SETUP_DONE = "key_setup_done"
    private const val KEY_DUAL_STYLE = "key_dual_style"
    private const val KEY_SPLIT_COLOR_A = "key_split_color_a"
    private const val KEY_SPLIT_COLOR_B = "key_split_color_b"
    private const val KEY_SPIN_COLOR_A = "key_spin_color_a"
    private const val KEY_SPIN_COLOR_B = "key_spin_color_b"
    private const val KEY_DUO_OFFSET = "key_duo_hue_offset"
    private const val KEY_CUSTOM_PRESETS = "key_custom_presets"
    private const val KEY_NOTIF_MODE = "key_notif_mode"
    private const val KEY_NOTIF_ACTIVATE_DURATION = "key_notif_activate_duration"
    private const val KEY_AUDIO_REACTIVE = "key_audio_reactive"

    private const val KEY_AUDIO_MODIFIER_MIGRATED = "key_audio_modifier_migrated"

    private const val KEY_AUDIO_SOURCE = "key_audio_source"
    private const val KEY_AUDIO_GAIN = "key_audio_gain"
    private const val KEY_AUDIO_THRESHOLD = "key_audio_beat_threshold"
    private const val KEY_AUDIO_MIN_HZ = "key_audio_min_hz"
    private const val KEY_AUDIO_MAX_HZ = "key_audio_max_hz"
    private const val KEY_AUDIO_BAND_MODE = "key_audio_band_mode"
    private const val KEY_AUDIO_DEGREES_PER_BEAT = "key_audio_degrees_per_beat"
    private const val KEY_AUDIO_AUTO_SENSITIVITY = "key_audio_auto_sensitivity"

    data class AudioConfig(
        val source: Int,
        val gain: Float,
        val threshold: Float,
        val minHz: Float,
        val maxHz: Float,
        val bandMode: Int,
        val degreesPerBeat: Float,
        val autoSensitivity: Boolean
    )

    fun audioConfig(context: Context): AudioConfig {
        val p = prefs(context)
        return AudioConfig(
            source = p.getInt(KEY_AUDIO_SOURCE, AudioReactiveEngine.SOURCE_GLOBAL_MIX),
            gain = p.getFloat(KEY_AUDIO_GAIN, 0.7f),
            threshold = p.getFloat(KEY_AUDIO_THRESHOLD, 1.18f),
            minHz = p.getFloat(KEY_AUDIO_MIN_HZ, AudioReactiveEngine.MIN_HZ_DEFAULT),
            maxHz = p.getFloat(KEY_AUDIO_MAX_HZ, AudioReactiveEngine.MAX_HZ_DEFAULT),
            bandMode = p.getInt(KEY_AUDIO_BAND_MODE, AudioReactiveEngine.BAND_MODE_AUTO),
            degreesPerBeat = p.getFloat(KEY_AUDIO_DEGREES_PER_BEAT, 30f),
            autoSensitivity = p.getBoolean(KEY_AUDIO_AUTO_SENSITIVITY, true)
        )
    }

    fun setAudioSource(context: Context, source: Int) {
        prefs(context).edit().putInt(KEY_AUDIO_SOURCE, source).apply()
    }
    fun setAudioGain(context: Context, gain: Float) {
        prefs(context).edit().putFloat(KEY_AUDIO_GAIN, gain).apply()
    }

    fun setAudioBeatThreshold(context: Context, threshold: Float) {
        prefs(context).edit().putFloat(KEY_AUDIO_THRESHOLD, threshold).apply()
    }

    fun setAudioBand(context: Context, minHz: Float, maxHz: Float) {
        prefs(context).edit()
            .putFloat(KEY_AUDIO_MIN_HZ, minHz)
            .putFloat(KEY_AUDIO_MAX_HZ, maxHz)
            .apply()
    }
    fun setAudioBandMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_AUDIO_BAND_MODE, mode).apply()
    }

    fun setAudioAutoSensitivity(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUDIO_AUTO_SENSITIVITY, enabled).apply()
    }

    fun setAudioDegreesPerBeat(context: Context, degrees: Float) {
        prefs(context).edit().putFloat(KEY_AUDIO_DEGREES_PER_BEAT, degrees).apply()
    }

    const val NOTIF_MODE_OFF = 0
    const val NOTIF_MODE_BLINK = 1
    const val NOTIF_MODE_ACTIVATE = 2

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getMode(context: Context, default: Int = DashDazzService.MODE_RAINBOW): Int {
        return prefs(context).getInt(KEY_MODE, default)
    }

    fun setMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply()
    }

    fun getColor(context: Context, default: Int = Color.RED): Int {
        return prefs(context).getInt(KEY_COLOR, default)
    }

    fun setColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_COLOR, color).apply()
    }

    fun getSpeedMs(context: Context, default: Long = 25L): Long {
        return prefs(context).getLong(KEY_SPEED, default)
    }

    fun setSpeedMs(context: Context, speed: Long) {
        prefs(context).edit().putLong(KEY_SPEED, speed).apply()
    }

    fun getBrightness(context: Context, default: Float = 1.0f): Float {
        return prefs(context).getFloat(KEY_BRIGHTNESS, default)
    }

    fun setBrightness(context: Context, brightness: Float) {
        prefs(context).edit().putFloat(KEY_BRIGHTNESS, brightness).apply()
    }

    fun isPauseOnScreenOff(context: Context, default: Boolean = true): Boolean {
        return prefs(context).getBoolean(KEY_PAUSE_ON_SCREEN_OFF, default)
    }

    fun setPauseOnScreenOff(context: Context, pause: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAUSE_ON_SCREEN_OFF, pause).apply()
    }

    fun getScreenMode(context: Context): Int {
        val p = prefs(context)
        if (p.contains(KEY_SCREEN_MODE)) return p.getInt(KEY_SCREEN_MODE, DashDazzService.SCREEN_ON_ONLY)
        return if (isPauseOnScreenOff(context)) DashDazzService.SCREEN_ON_ONLY else DashDazzService.SCREEN_ALWAYS
    }

    fun setScreenMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_SCREEN_MODE, mode).apply()
    }

    fun isSetupDone(context: Context): Boolean = prefs(context).getBoolean(KEY_SETUP_DONE, false)

    fun setSetupDone(context: Context, done: Boolean) {
        prefs(context).edit().putBoolean(KEY_SETUP_DONE, done).apply()
    }

    fun getDuoHueOffset(context: Context, default: Int = 90): Int {
        return prefs(context).getInt(KEY_DUO_OFFSET, default)
    }

    fun setDuoHueOffset(context: Context, degrees: Int) {
        prefs(context).edit().putInt(KEY_DUO_OFFSET, degrees).apply()
    }

    fun isBlinkOnNotification(context: Context, default: Boolean = false): Boolean {
        return prefs(context).getBoolean(KEY_BLINK_ON_NOTIFICATION, default)
    }

    fun setBlinkOnNotification(context: Context, blink: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLINK_ON_NOTIFICATION, blink).apply()
    }

    fun getPalette(context: Context, default: Int = Palettes.SPECTRUM): Int {
        return prefs(context).getInt(KEY_PALETTE, default)
    }

    fun setPalette(context: Context, palette: Int) {
        prefs(context).edit().putInt(KEY_PALETTE, palette).apply()
    }

    fun getDualStyle(context: Context, default: Int = DashDazzService.DUAL_HARDWARE_SPLIT): Int {
        return prefs(context).getInt(KEY_DUAL_STYLE, default)
    }

    fun setDualStyle(context: Context, style: Int) {
        prefs(context).edit().putInt(KEY_DUAL_STYLE, style).apply()
    }

    fun getSplitColorA(context: Context, default: Int = Color.RED): Int {
        return prefs(context).getInt(KEY_SPLIT_COLOR_A, default)
    }

    fun setSplitColorA(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_SPLIT_COLOR_A, color).apply()
    }

    fun getSplitColorB(context: Context, default: Int = Color.BLUE): Int {
        return prefs(context).getInt(KEY_SPLIT_COLOR_B, default)
    }

    fun setSplitColorB(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_SPLIT_COLOR_B, color).apply()
    }

    fun getSpinColorA(context: Context, default: Int = Color.RED): Int {
        return prefs(context).getInt(KEY_SPIN_COLOR_A, default)
    }

    fun setSpinColorA(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_SPIN_COLOR_A, color).apply()
    }

    fun getSpinColorB(context: Context, default: Int = Color.BLUE): Int {
        return prefs(context).getInt(KEY_SPIN_COLOR_B, default)
    }

    fun setSpinColorB(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_SPIN_COLOR_B, color).apply()
    }

    fun getDarkMode(context: Context, default: Int = 0): Int {
        return prefs(context).getInt(KEY_DARK_MODE, default)
    }

    fun setDarkMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_DARK_MODE, mode).apply()
    }

    fun getCustomPresets(context: Context): List<Pair<Float, Float>> {
        val raw = prefs(context).getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        return raw.split(";").filter { it.contains(",") }.mapNotNull {
            val parts = it.split(",")
            val h = parts.getOrNull(0)?.toFloatOrNull()
            val s = parts.getOrNull(1)?.toFloatOrNull()
            if (h != null && s != null) Pair(h, s) else null
        }
    }

    fun saveCustomPreset(context: Context, hue: Float, saturation: Float) {
        val current = getCustomPresets(context).toMutableList()
        if (current.any { Math.abs(it.first - hue) < 2f && Math.abs(it.second - saturation) < 0.03f }) {
            return
        }
        current.add(Pair(hue, saturation))
        val encoded = current.joinToString(";") { "${it.first},${it.second}" }
        prefs(context).edit().putString(KEY_CUSTOM_PRESETS, encoded).apply()
    }

    fun removeCustomPreset(context: Context, index: Int) {
        val current = getCustomPresets(context).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val encoded = current.joinToString(";") { "${it.first},${it.second}" }
            prefs(context).edit().putString(KEY_CUSTOM_PRESETS, encoded).apply()
        }
    }

    fun getNotificationMode(context: Context, default: Int = NOTIF_MODE_OFF): Int {

        val p = prefs(context)
        if (!p.contains(KEY_NOTIF_MODE)) {
            if (p.getBoolean(KEY_BLINK_ON_NOTIFICATION, false)) {
                p.edit().putInt(KEY_NOTIF_MODE, NOTIF_MODE_BLINK).apply()
                return NOTIF_MODE_BLINK
            }
            return default
        }
        return p.getInt(KEY_NOTIF_MODE, default)
    }

    fun setNotificationMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_MODE, mode).apply()
    }

    fun getNotificationActivateDuration(context: Context, default: Int = 5): Int {
        return prefs(context).getInt(KEY_NOTIF_ACTIVATE_DURATION, default)
    }

    fun setNotificationActivateDuration(context: Context, seconds: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_ACTIVATE_DURATION, seconds).apply()
    }

    fun isAudioReactive(context: Context, default: Boolean = false): Boolean =
        prefs(context).getBoolean(KEY_AUDIO_REACTIVE, default)

    fun setAudioReactive(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUDIO_REACTIVE, enabled).apply()
    }

    fun needsAudioModifierMigration(context: Context): Int? {
        val p = prefs(context)
        if (p.getBoolean(KEY_AUDIO_MODIFIER_MIGRATED, false)) return null
        val saved = p.getInt(KEY_MODE, -1)
        if (saved != DashDazzService.MODE_AUDIO_REACTIVE) return null
        p.edit().putBoolean(KEY_AUDIO_MODIFIER_MIGRATED, true).apply()
        return DashDazzService.MODE_RAINBOW
    }

    fun markAudioModifierMigrated(context: Context) {
        prefs(context).edit().putBoolean(KEY_AUDIO_MODIFIER_MIGRATED, true).apply()
    }

    fun loadState(context: Context): DashDazzState {
        return DashDazzState(
            mode = getMode(context),
            color = getColor(context),
            speedMs = getSpeedMs(context),
            brightness = getBrightness(context),
            screenMode = getScreenMode(context),
            blinkOnNotification = isBlinkOnNotification(context),
            palette = getPalette(context),
            dualStyle = getDualStyle(context),
            splitColorA = getSplitColorA(context),
            splitColorB = getSplitColorB(context),
            spinColorA = getSpinColorA(context),
            spinColorB = getSpinColorB(context),
            duoHueOffset = getDuoHueOffset(context),
            notifMode = getNotificationMode(context),
            notifActivateDuration = getNotificationActivateDuration(context),
            audioReactive = isAudioReactive(context)
        )
    }
}
