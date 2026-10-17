package com.dashdazz.light

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Task 1: Preset Palette Manager.
 * Allows creating, saving, loading, exporting, and importing custom color palettes.
 */
data class CustomPalette(
    val id: String,
    val name: String,
    val colors: List<Int>,
    val isDualTone: Boolean = false
)

object CustomPaletteManager {
    private const val PREFS_KEY_CUSTOM_PALETTES = "dashdazz_custom_palettes_json"

    fun loadCustomPalettes(context: Context): List<CustomPalette> {
        val prefs = context.getSharedPreferences("dashdazz_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREFS_KEY_CUSTOM_PALETTES, null) ?: return defaultCustomPalettes()
        return deserializePalettes(jsonStr)
    }

    fun saveCustomPalettes(context: Context, palettes: List<CustomPalette>) {
        val prefs = context.getSharedPreferences("dashdazz_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY_CUSTOM_PALETTES, serializePalettes(palettes)).apply()
    }

    fun serializePalettes(palettes: List<CustomPalette>): String {
        val array = JSONArray()
        for (p in palettes) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("isDualTone", p.isDualTone)
                val colorArr = JSONArray()
                p.colors.forEach { colorArr.put(it) }
                put("colors", colorArr)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializePalettes(jsonStr: String): List<CustomPalette> {
        val list = mutableListOf<CustomPalette>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val isDualTone = obj.optBoolean("isDualTone", false)
                val colorArr = obj.getJSONArray("colors")
                val colors = mutableListOf<Int>()
                for (j in 0 until colorArr.length()) {
                    colors.add(colorArr.getInt(j))
                }
                list.add(CustomPalette(id, name, colors, isDualTone))
            }
        } catch (_: Exception) {
            return defaultCustomPalettes()
        }
        return list
    }

    fun defaultCustomPalettes(): List<CustomPalette> = listOf(
        CustomPalette(
            id = "preset_hyper_cyan",
            name = "Hyper Neon Cyan",
            colors = listOf(0xFF00E5FF.toInt(), 0xFF00B0FF.toInt()),
            isDualTone = true
        ),
        CustomPalette(
            id = "preset_cyber_purple",
            name = "Cyberpunk Violet",
            colors = listOf(0xFF7928CA.toInt(), 0xFFFF007F.toInt()),
            isDualTone = true
        ),
        CustomPalette(
            id = "preset_electric_amber",
            name = "Electric Amber",
            colors = listOf(0xFFFF9100.toInt(), 0xFFFFD600.toInt()),
            isDualTone = true
        )
    )
}
