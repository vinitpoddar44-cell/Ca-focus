package com.cafocus

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var mins = 45
    private var ring: RingView? = null

    private val tick = object : Runnable {
        override fun run() { if (update()) h.postDelayed(this, 1000) }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = C.BG
        window.navigationBarColor = C.CARD
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    }

    override fun onResume() { super.onResume(); build() }
    override fun onPause() { h.removeCallbacks(tick); super.onPause() }

    @Suppress("DEPRECATION")
    private fun hasUsage(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun update(): Boolean {
        val left = Store.endAt(this) - System.currentTimeMillis()
        if (left <= 0) { build(); return false }
        val sec = (left + 999) / 1000
        ring?.set(left.toFloat() / Store.total(this).coerceAtLeast(1L), String.format("%02d:%02d", sec / 60, sec % 60), "left")
        return true
    }

    @Suppress("DEPRECATION")
    private fun build() {
        h.removeCallbacks(tick)
        ring = null
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(16))
        }
        body.addView(tv("CA Focus", 30f, true))
        body.addView(tv("Study without distractions", 14f, color = C.MUT).apply { setPadding(0, 0, 0, dp(16)) })
        var bar: LinearLayout? = null
        val left = Store.endAt(this) - System.currentTimeMillis()

        if (left > 0) {
            val r = RingView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(250), dp(250)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(16); bottomMargin = dp(16)
                }
            }
            ring = r
            body.addView(r)
            body.addView(tv("${Store.blocked(this).size} apps blocked. Phone calls still work.", 14f, color = C.MUT).apply { gravity = Gravity.CENTER })
            val n = Store.emergency(this).filter { it.isDigit() || it == '+' }
            if (n.isNotEmpty()) body.addView(button("Call emergency contact", C.RED) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))
            })
            body.addView(button("Hold to end session", C.INK, outline = true) {
                Toast.makeText(this, "Press and hold to end", Toast.LENGTH_SHORT).show()
            }.apply {
                setOnLongClickListener {
                    Store.setEndAt(this@MainActivity, 0)
                    stopService(Intent(this@MainActivity, FocusService::class.java))
                    build(); true
                }
            })
        } else {
            val usage = hasUsage()
            val overlay = Settings.canDrawOverlays(this)
            if (!usage || !overlay) {
                val pc = card()
                pc.addView(tv("Set up permissions", 16f, true))
                pc.addView(tv("CA Focus needs these to block apps.", 13f, color = C.MUT))
                if (!usage) pc.addView(button("Allow usage access", C.INK) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
                if (!overlay) pc.addView(button("Allow display over other apps", C.INK) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                })
                body.addView(pc)
            }

            val dc = card()
            dc.addView(tv("Focus time", 16f, true))
            val row = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
            val pills = mutableListOf<Pair<Int, TextView>>()
            listOf(25, 45, 60, 90).forEach { m ->
                val t = pill("$m min", m == mins) {
                    mins = m
                    pills.forEach { (v, tt) -> stylePill(tt, v == m) }
                }
                pills.add(m to t)
                row.addView(t)
            }
            dc.addView(row)
            body.addView(dc)

            val ec = card()
            ec.addView(tv("Emergency contact", 16f, true))
            ec.addView(tv("One tap opens the dialler with this number.", 13f, color = C.MUT))
            val emg = EditText(this).apply {
                hint = "Phone number"
                inputType = InputType.TYPE_CLASS_PHONE
                setText(Store.emergency(this@MainActivity))
                setTextColor(C.INK)
                background = shape(android.graphics.Color.WHITE, 10, C.LINE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            ec.addView(emg, LinearLayout.LayoutParams(MP, WC).apply { topMargin = dp(8) })
            body.addView(ec)

            val ac = card()
            ac.addView(tv("Apps to block", 16f, true))
            val pm = packageManager
            val dial = pm.queryIntentActivities(Intent(Intent.ACTION_DIAL), 0).map { it.activityInfo.packageName }.toSet()
            val saved = Store.blocked(this)
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .filter { it.activityInfo.packageName != packageName && it.activityInfo.packageName !in dial }
                .distinctBy { it.activityInfo.packageName }
                .sortedBy { it.loadLabel(pm).toString().lowercase() }
                .forEach { info ->
                    val pkg = info.activityInfo.packageName
                    val line = LinearLayout(this).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dp(6), 0, dp(6))
                    }
                    line.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(pm)) }, LinearLayout.LayoutParams(dp(36), dp(36)))
                    val cb = CheckBox(this).apply {
                        text = info.loadLabel(pm).toString()
                        setTextColor(C.INK)
                        isChecked = pkg in saved
                        setOnCheckedChangeListener { _, on ->
                            val s = Store.blocked(this@MainActivity).toMutableSet()
                            if (on) s.add(pkg) else s.remove(pkg)
                            Store.setBlocked(this@MainActivity, s)
                        }
                    }
                    line.addView(cb, LinearLayout.LayoutParams(0, WC, 1f).apply { leftMargin = dp(10) })
                    ac.addView(line)
                }
            body.addView(ac)

            if (usage && overlay) {
                bar = LinearLayout(this).apply {
                    setBackgroundColor(C.CARD)
                    setPadding(dp(18), dp(8), dp(18), dp(14))
                }
                bar.addView(button("Start focus", C.INK) {
                    Store.setEmergency(this, emg.text.toString().trim())
                    if (Store.blocked(this).isEmpty()) {
                        Toast.makeText(this, "Choose at least one app to block", Toast.LENGTH_SHORT).show()
                    } else {
                        val ms = mins * 60000L
                        Store.setEndAt(this, System.currentTimeMillis() + ms)
                        Store.setTotal(this, ms)
                        startForegroundService(Intent(this, FocusService::class.java))
                        build()
                    }
                }, LinearLayout.LayoutParams(MP, dp(54)))
            }
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.BG)
        }
        page.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(MP, 0, 1f))
        bar?.let { page.addView(it) }
        setContentView(page)
        if (left > 0 && update()) h.postDelayed(tick, 1000)
    }
}
