package com.kbsuite.keyboard.clip

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object ClipLock {

    const val MODE_NONE = 0
    const val MODE_PIN = 1
    const val MODE_PATTERN = 2
    const val MODE_TAP_COUNT = 3
    const val MODE_HIDDEN_SPOT = 4

    private const val FILE = "keysuite_clip_lock"

    private fun store(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun mode(context: Context): Int = store(context).getInt("mode", MODE_NONE)

    fun setMode(context: Context, mode: Int) {
        store(context).edit().putInt("mode", mode).apply()
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setSecret(context: Context, secret: String) {
        store(context).edit().putString("secret", hash(secret)).apply()
    }

    fun check(context: Context, attempt: String): Boolean {
        val saved = store(context).getString("secret", null) ?: return true
        return saved == hash(attempt)
    }

    fun tapTarget(context: Context): Int = store(context).getInt("tap_count", 5)

    fun setTapTarget(context: Context, count: Int) {
        store(context).edit().putInt("tap_count", count).apply()
    }

    fun showTitleInShade(context: Context): Boolean = store(context).getBoolean("show_in_shade", false)

    fun hideWhenLocked(context: Context): Boolean = mode(context) != MODE_NONE
}
