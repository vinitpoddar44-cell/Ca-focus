package com.cafocus

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private fun p(c: Context) = c.getSharedPreferences("cafocus", Context.MODE_PRIVATE)

    fun blocked(c: Context): Set<String> = HashSet(p(c).getStringSet("blocked", emptySet()) ?: emptySet())
    fun setBlocked(c: Context, s: Set<String>) = p(c).edit().putStringSet("blocked", HashSet(s)).apply()

    fun endAt(c: Context): Long = p(c).getLong("end", 0L)
    fun setEndAt(c: Context, t: Long) = p(c).edit().putLong("end", t).apply()
    fun startAt(c: Context): Long = p(c).getLong("start", 0L)
    fun setStartAt(c: Context, t: Long) = p(c).edit().putLong("start", t).apply()
    fun total(c: Context): Long = p(c).getLong("total", 0L)
    fun setTotal(c: Context, t: Long) = p(c).edit().putLong("total", t).apply()

    fun subject(c: Context): String = p(c).getString("subject", "") ?: ""
    fun setSubject(c: Context, s: String) = p(c).edit().putString("subject", s).apply()

    fun ringtone(c: Context): String = p(c).getString("ringtone", "") ?: ""
    fun setRingtone(c: Context, s: String) = p(c).edit().putString("ringtone", s).apply()

    fun pendingCall(c: Context): String = p(c).getString("pending", "") ?: ""
    fun setPendingCall(c: Context, s: String) = p(c).edit().putString("pending", s).apply()

    fun contacts(c: Context): List<Pair<String, String>> {
        val raw = p(c).getString("contacts", "") ?: ""
        return raw.split("\n").filter { it.contains("\t") }.map {
            val i = it.indexOf('\t')
            it.substring(0, i) to it.substring(i + 1)
        }
    }

    fun setContacts(c: Context, list: List<Pair<String, String>>) =
        p(c).edit().putString("contacts", list.joinToString("\n") { it.first + "\t" + it.second }).apply()

    fun dateStr(t: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(t))

    private fun addLog(c: Context, date: String, subject: String, secs: Long) {
        val old = p(c).getString("log", "") ?: ""
        val line = date + "\t" + subject + "\t" + secs
        p(c).edit().putString("log", if (old.isEmpty()) line else old + "\n" + line).apply()
    }

    fun logs(c: Context): List<Triple<String, String, Long>> {
        val raw = p(c).getString("log", "") ?: ""
        return raw.split("\n").mapNotNull { line ->
            val a = line.split("\t")
            val secs = if (a.size == 3) a[2].toLongOrNull() else null
            if (secs == null) null else Triple(a[0], a[1], secs)
        }
    }

    // Ends the running session, saves the time studied, and returns the seconds studied (-1 if no session was active).
    fun finishSession(c: Context): Long {
        val start = startAt(c)
        if (start == 0L) return -1L
        val end = minOf(System.currentTimeMillis(), endAt(c))
        val secs = ((end - start) / 1000).coerceAtLeast(0L)
        if (secs >= 30) addLog(c, dateStr(start), subject(c).ifEmpty { "General" }, secs)
        setStartAt(c, 0L)
        setEndAt(c, 0L)
        return secs
    }
}
