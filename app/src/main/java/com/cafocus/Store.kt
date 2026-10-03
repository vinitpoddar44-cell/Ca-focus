package com.cafocus

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {

    private const val PREFS = "cafocus"

    private fun p(c: Context) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // =========================================================
    // BLOCKED APPS
    // =========================================================

    fun blocked(c: Context): Set<String> {
        return HashSet(
            p(c).getStringSet(
                "blocked",
                emptySet()
            ) ?: emptySet()
        )
    }

    fun setBlocked(
        c: Context,
        apps: Set<String>
    ) {
        p(c)
            .edit()
            .putStringSet(
                "blocked",
                HashSet(apps)
            )
            .apply()
    }

    // =========================================================
    // TIMER
    // =========================================================

    fun startAt(c: Context): Long {
        return p(c).getLong(
            "start",
            0L
        )
    }

    fun setStartAt(
        c: Context,
        time: Long
    ) {
        p(c)
            .edit()
            .putLong(
                "start",
                time
            )
            .apply()
    }

    fun endAt(c: Context): Long {
        return p(c).getLong(
            "end",
            0L
        )
    }

    fun setEndAt(
        c: Context,
        time: Long
    ) {
        p(c)
            .edit()
            .putLong(
                "end",
                time
            )
            .apply()
    }

    fun total(c: Context): Long {
        return p(c).getLong(
            "total",
            0L
        )
    }

    fun setTotal(
        c: Context,
        time: Long
    ) {
        p(c)
            .edit()
            .putLong(
                "total",
                time
            )
            .apply()
    }

    // =========================================================
    // SUBJECT
    // =========================================================

    fun subject(c: Context): String {
        return p(c).getString(
            "subject",
            ""
        ) ?: ""
    }

    fun setSubject(
        c: Context,
        subject: String
    ) {
        p(c)
            .edit()
            .putString(
                "subject",
                subject
            )
            .apply()
    }

    // =========================================================
    // RINGTONE
    // =========================================================

    fun ringtone(c: Context): String {
        return p(c).getString(
            "ringtone",
            ""
        ) ?: ""
    }

    fun setRingtone(
        c: Context,
        ringtoneUri: String
    ) {
        p(c)
            .edit()
            .putString(
                "ringtone",
                ringtoneUri
            )
            .apply()
    }

    // =========================================================
    // EMERGENCY CONTACTS
    // =========================================================

    fun contacts(
        c: Context
    ): List<Pair<String, String>> {

        val raw =
            p(c).getString(
                "contacts",
                ""
            ) ?: ""

        if (raw.isBlank()) {
            return emptyList()
        }

        return raw
            .split("\n")
            .mapNotNull { line ->

                if (!line.contains("\t")) {
                    return@mapNotNull null
                }

                val index =
                    line.indexOf('\t')

                if (
                    index <= 0 ||
                    index >= line.length - 1
                ) {
                    return@mapNotNull null
                }

                val name =
                    line.substring(
                        0,
                        index
                    ).trim()

                val number =
                    line.substring(
                        index + 1
                    ).trim()

                if (
                    name.isEmpty() ||
                    number.isEmpty()
                ) {
                    null
                } else {
                    name to number
                }
            }
    }

    fun setContacts(
        c: Context,
        contacts: List<Pair<String, String>>
    ) {

        val cleaned =
            contacts.mapNotNull { contact ->

                val name =
                    contact.first
                        .replace(
                            "\t",
                            " "
                        )
                        .replace(
                            "\n",
                            " "
                        )
                        .trim()

                val number =
                    contact.second
                        .replace(
                            "\n",
                            ""
                        )
                        .replace(
                            "\t",
                            ""
                        )
                        .trim()

                if (
                    name.isEmpty() ||
                    number.isEmpty()
                ) {
                    null
                } else {
                    name to number
                }
            }

        val raw =
            cleaned.joinToString("\n") {
                "${it.first}\t${it.second}"
            }

        p(c)
            .edit()
            .putString(
                "contacts",
                raw
            )
            .apply()
    }

    // =========================================================
    // PENDING DIRECT CALL
    //
    // These functions are REQUIRED by Ui.kt.
    // =========================================================

    fun pendingCall(
        c: Context
    ): String {

        return p(c).getString(
            "pending_call",
            ""
        ) ?: ""
    }

    fun setPendingCall(
        c: Context,
        number: String
    ) {

        p(c)
            .edit()
            .putString(
                "pending_call",
                number
            )
            .apply()
    }

    fun clearPendingCall(
        c: Context
    ) {

        p(c)
            .edit()
            .remove(
                "pending_call"
            )
            .apply()
    }

    // =========================================================
    // STUDY DATE
    // =========================================================

    fun dateStr(
        time: Long
    ): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Date(time)
        )
    }

    // =========================================================
    // STUDY LOG
    // =========================================================

    private fun addLog(
        c: Context,
        date: String,
        subject: String,
        seconds: Long
    ) {

        if (seconds <= 0L) {
            return
        }

        val prefs = p(c)

        val old =
            prefs.getString(
                "log",
                ""
            ) ?: ""

        val safeSubject =
            subject
                .replace(
                    "\t",
                    " "
                )
                .replace(
                    "\n",
                    " "
                )
                .trim()
                .ifEmpty {
                    "General"
                }

        val line =
            "$date\t$safeSubject\t$seconds"

        val updated =
            if (old.isBlank()) {
                line
            } else {
                "$old\n$line"
            }

        prefs
            .edit()
            .putString(
                "log",
                updated
            )
            .apply()
    }

    // =========================================================
    // READ STUDY LOG
    // =========================================================

    fun logs(
        c: Context
    ): List<Triple<String, String, Long>> {

        val raw =
            p(c).getString(
                "log",
                ""
            ) ?: ""

        if (raw.isBlank()) {
            return emptyList()
        }

        return raw
            .split("\n")
            .mapNotNull { line ->

                val parts =
                    line.split("\t")

                if (parts.size != 3) {
                    return@mapNotNull null
                }

                val date =
                    parts[0]

                val subject =
                    parts[1]

                val seconds =
                    parts[2].toLongOrNull()

                if (
                    date.isBlank() ||
                    subject.isBlank() ||
                    seconds == null ||
                    seconds <= 0L
                ) {
                    null
                } else {
                    Triple(
                        date,
                        subject,
                        seconds
                    )
                }
            }
    }

    // =========================================================
    // FINISH STUDY SESSION
    // =========================================================

    fun finishSession(
        c: Context
    ): Long {

        val start =
            startAt(c)

        if (start <= 0L) {
            return -1L
        }

        val end =
            endAt(c)

        val now =
            System.currentTimeMillis()

        val actualEnd =
            if (end > 0L) {
                minOf(
                    now,
                    end
                )
            } else {
                now
            }

        val seconds =
            (
                (actualEnd - start) /
                    1000L
                )
                .coerceAtLeast(0L)

        // Only save sessions of 30 seconds or more.
        if (seconds >= 30L) {

            val savedSubject =
                subject(c)
                    .ifBlank {
                        "General"
                    }

            addLog(
                c,
                dateStr(start),
                savedSubject,
                seconds
            )
        }

        // Clear active timer.
        setStartAt(
            c,
            0L
        )

        setEndAt(
            c,
            0L
        )

        setTotal(
            c,
            0L
        )

        return seconds
    }

    // =========================================================
    // CLEAR LOGS
    // =========================================================

    fun clearLogs(
        c: Context
    ) {

        p(c)
            .edit()
            .remove("log")
            .apply()
    }
}
