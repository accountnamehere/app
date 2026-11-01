package com.kbsuite.keyboard

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "keysuite_prefs"

    const val THEME = "theme"
    const val KEY_HEIGHT_DP = "key_height_dp"
    const val HAPTIC = "haptic"
    const val SOUND = "sound"
    const val AUTO_CAPS = "auto_caps"
    const val DOUBLE_SPACE_PERIOD = "double_space_period"
    const val NUMBER_ROW = "number_row"
    const val DOUBLE_TAP_MS = "double_tap_ms"
    const val REPEAT_DELAY_MS = "repeat_delay_ms"
    const val REPEAT_RATE_MS = "repeat_rate_ms"

    private val defaults: Map<String, Any> = mapOf(
        THEME to 1,
        KEY_HEIGHT_DP to 52,
        HAPTIC to true,
        SOUND to false,
        AUTO_CAPS to true,
        DOUBLE_SPACE_PERIOD to true,
        NUMBER_ROW to true,
        DOUBLE_TAP_MS to 300,
        REPEAT_DELAY_MS to 350,
        REPEAT_RATE_MS to 45
    )

    fun store(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun int(context: Context, key: String): Int =
        store(context).getInt(key, defaults[key] as Int)

    fun bool(context: Context, key: String): Boolean =
        store(context).getBoolean(key, defaults[key] as Boolean)

    fun putInt(context: Context, key: String, value: Int) {
        store(context).edit().putInt(key, value).apply()
    }

    fun putBool(context: Context, key: String, value: Boolean) {
        store(context).edit().putBoolean(key, value).apply()
    }

    fun reset(context: Context, key: String) {
        store(context).edit().remove(key).apply()
    }

    fun default(key: String): Any = defaults.getValue(key)
}
