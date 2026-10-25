package com.odialer.pro

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("odialer", Context.MODE_PRIVATE)

    fun autoRecord(c: Context) = sp(c).getBoolean("auto", true)
    fun setAutoRecord(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()

    fun speakerBoost(c: Context) = sp(c).getBoolean("boost", false)
    fun setSpeakerBoost(c: Context, v: Boolean) = sp(c).edit().putBoolean("boost", v).apply()

    fun source(c: Context) = sp(c).getInt("src", 0)
    fun setSource(c: Context, v: Int) = sp(c).edit().putInt("src", v).apply()

    fun pending(c: Context): String? = sp(c).getString("pending", null)
    fun setPending(c: Context, v: String?) = sp(c).edit().putString("pending", v).apply()
}
