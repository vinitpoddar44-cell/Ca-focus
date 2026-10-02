package com.cafocus

import android.content.Context

object Store {
    private fun p(c: Context) = c.getSharedPreferences("cafocus", Context.MODE_PRIVATE)
    fun blocked(c: Context): Set<String> = HashSet(p(c).getStringSet("blocked", emptySet()) ?: emptySet())
    fun setBlocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("blocked", HashSet(s)).apply()
    fun emergency(c: Context): String = p(c).getString("emg", "") ?: ""
    fun setEmergency(c: Context, n: String) = p(c).edit().putString("emg", n).apply()
    fun endAt(c: Context): Long = p(c).getLong("end", 0L)
    fun setEndAt(c: Context, t: Long) = p(c).edit().putLong("end", t).apply()
    fun total(c: Context): Long = p(c).getLong("total", 0L)
    fun setTotal(c: Context, t: Long) = p(c).edit().putLong("total", t).apply()
}
