package com.cafocus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

// Plays the timer-end sound (the student's chosen ringtone, or the default alarm).
object Alarm {
    private var ringtone: Ringtone? = null
    private val h = Handler(Looper.getMainLooper())

    val ringing: Boolean
        get() = ringtone?.isPlaying == true

    fun play(c: Context, maxMs: Long) {
        stop()
        val saved = Store.ringtone(c)
        val uri = if (saved.isNotEmpty()) Uri.parse(saved) else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val r = RingtoneManager.getRingtone(c.applicationContext, uri) ?: return
        r.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        if (Build.VERSION.SDK_INT >= 28) r.isLooping = true
        r.play()
        ringtone = r
        h.postDelayed({ stop() }, maxMs)
    }

    fun stop() {
        h.removeCallbacksAndMessages(null)
        try { ringtone?.stop() } catch (e: Exception) { }
        ringtone = null
    }
}

class FocusService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private var last: String? = null

    private val loop = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() >= Store.endAt(this@FocusService)) { complete(); return }
            val fg = foreground()
            if (fg != null && fg in Store.blocked(this@FocusService)) {
                startActivity(Intent(this@FocusService, BlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            h.postDelayed(this, 700)
        }
    }

    private fun complete() {
        val secs = Store.finishSession(this)
        if (secs >= 0) {
            Alarm.play(this, 45_000)
            notifyDone(secs)
        }
        stopSelf()
    }

    private fun notifyDone(secs: Long) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("done", "Session complete", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "done")
            .setContentTitle("Study session complete")
            .setContentText("You studied ${fmtTime(secs)}. Well done!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(2, n)
    }

    @Suppress("DEPRECATION")
    private fun foreground(): String? {
        val um = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val ev = um.queryEvents(now - 10_000, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("focus", "Focus session", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "focus")
            .setContentTitle("Focus session on")
            .setContentText("Blocked apps are paused. Emergency calls still work.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        h.removeCallbacks(loop)
        h.post(loop)
        return START_STICKY
    }

    override fun onDestroy() { h.removeCallbacks(loop); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
