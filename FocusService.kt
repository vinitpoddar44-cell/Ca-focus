package com.cafocus

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*

class FocusService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private var last: String? = null

    private val loop = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() >= Store.endAt(this@FocusService)) { stopSelf(); return }
            val fg = foreground()
            if (fg != null && fg in Store.blocked(this@FocusService)) {
                startActivity(Intent(this@FocusService, BlockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            h.postDelayed(this, 700)
        }
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
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        h.removeCallbacks(loop); h.post(loop)
        return START_STICKY
    }

    override fun onDestroy() { h.removeCallbacks(loop); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
