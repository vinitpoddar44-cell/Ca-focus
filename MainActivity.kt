package com.cafocus

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    }

    override fun onResume() { super.onResume(); build() }

    @Suppress("DEPRECATION")
    private fun hasUsage(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 80); setBackgroundColor(Color.parseColor("#E9F0E4"))
        }
        root.addView(tv("CA Focus", 30f, true))
        val left = Store.endAt(this) - System.currentTimeMillis()
        if (left > 0) {
            root.addView(tv("Focus is on. ${left / 60000 + 1} min left.", 18f, false))
            root.addView(tv("Blocked apps are paused. Emergency calls still work.", 14f, false))
            root.addView(btn("Hold to end session") {}.apply {
                setOnLongClickListener {
                    Store.setEndAt(this@MainActivity, 0)
                    stopService(Intent(this@MainActivity, FocusService::class.java))
                    build(); true
                }
            })
        } else {
            val usage = hasUsage(); val overlay = Settings.canDrawOverlays(this)
            if (!usage) root.addView(btn("1. Allow usage access") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
            if (!overlay) root.addView(btn("2. Allow display over other apps") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            })
            val emg = EditText(this).apply {
                hint = "Emergency contact number"; inputType = InputType.TYPE_CLASS_PHONE; setText(Store.emergency(this@MainActivity))
            }
            val dur = EditText(this).apply { hint = "Minutes"; inputType = InputType.TYPE_CLASS_NUMBER; setText("45") }
            root.addView(tv("Emergency contact", 14f, true)); root.addView(emg)
            root.addView(tv("Focus minutes", 14f, true)); root.addView(dur)
            val checks = mutableMapOf<String, CheckBox>()
            if (usage && overlay) {
                root.addView(btn("Start focus") {
                    Store.setBlocked(this, checks.filter { it.value.isChecked }.keys)
                    Store.setEmergency(this, emg.text.toString().trim())
                    val m = dur.text.toString().toLongOrNull()?.coerceIn(1, 600) ?: 45
                    Store.setEndAt(this, System.currentTimeMillis() + m * 60000)
                    startForegroundService(Intent(this, FocusService::class.java))
                    build()
                })
            } else root.addView(tv("Allow both permissions above to start.", 14f, false))
            root.addView(tv("Apps to block", 14f, true))
            val pm = packageManager
            val dial = pm.queryIntentActivities(Intent(Intent.ACTION_DIAL), 0).map { it.activityInfo.packageName }.toSet()
            val saved = Store.blocked(this)
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }.distinct()
                .filter { it.first != packageName && it.first !in dial }.sortedBy { it.second.lowercase() }
                .forEach { (pkg, label) ->
                    val cb = CheckBox(this).apply { text = label; isChecked = pkg in saved }
                    checks[pkg] = cb; root.addView(cb)
                }
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
