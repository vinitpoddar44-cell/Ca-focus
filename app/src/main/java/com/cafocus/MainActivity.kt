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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

private val FOUNDATION = listOf(
    "Accounting",
    "Business Laws",
    "Quantitative Aptitude",
    "Business Economics and BCK"
)

private val PAPERS = mapOf(
    "Intermediate|Group I" to listOf("Advanced Accounting", "Corporate and Other Laws", "Taxation"),
    "Intermediate|Group II" to listOf("Cost and Management Accounting", "Auditing and Ethics", "Financial Management and Strategic Management"),
    "Final|Group I" to listOf("Financial Reporting", "Advanced Financial Management", "Advanced Auditing, Assurance and Professional Ethics"),
    "Final|Group II" to listOf("Direct Tax Laws and International Taxation", "Indirect Tax Laws", "Integrated Business Solutions")
)

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var screen = "level"
    private var settingsFrom = "level"
    private var level = ""
    private var group = ""
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

    override fun onResume() {
        super.onResume()
        if (running()) screen = "timer"
        build()
    }

    override fun onPause() { h.removeCallbacks(tick); super.onPause() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (screen) {
            "level" -> super.onBackPressed()
            "group" -> go("level")
            "subject" -> go(if (level == "Foundation") "level" else "group")
            "timer" -> if (running()) moveTaskToBack(true) else go("subject")
            "settings" -> go(settingsFrom)
        }
    }

    private fun running() = Store.endAt(this) > System.currentTimeMillis()
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    private fun go(s: String) { screen = s; build() }

    private fun openSettings() {
        if (running()) { toast("Finish or end the session to change apps"); return }
        settingsFrom = screen
        go("settings")
    }

    @Suppress("DEPRECATION")
    private fun hasUsage(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun update(): Boolean {
        val left = Store.endAt(this) - System.currentTimeMillis()
        if (left <= 0) { toast("Session complete. Well done!"); build(); return false }
        val sec = (left + 999) / 1000
        ring?.set(left.toFloat() / Store.total(this).coerceAtLeast(1L), String.format("%02d:%02d", sec / 60, sec % 60), "left")
        return true
    }

    private fun build() {
        h.removeCallbacks(tick)
        ring = null
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(16))
        }
        var dock: View? = null
        when (screen) {
            "level" -> levelScreen(body)
            "group" -> groupScreen(body)
            "subject" -> subjectScreen(body)
            "timer" -> dock = timerScreen(body)
            "settings" -> settingsScreen(body)
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.BG)
        }
        page.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(MP, 0, 1f))
        dock?.let { page.addView(it) }
        setContentView(page)
        if (screen == "timer" && running() && update()) h.postDelayed(tick, 1000)
    }

    // ---------- Page 1: choose level, group, subject ----------

    private fun levelScreen(b: LinearLayout) {
        b.addView(topBar("CA Focus", null, { openSettings() }))
        b.addView(tv("Choose your level", 15f, color = C.MUT).apply { setPadding(0, 0, 0, dp(12)) })
        b.addView(option("Foundation", "4 subjects") { pickLevel("Foundation") })
        b.addView(option("Intermediate", "Group I and Group II") { pickLevel("Intermediate") })
        b.addView(option("Final", "Group I and Group II") { pickLevel("Final") })
    }

    private fun pickLevel(l: String) {
        level = l
        group = ""
        go(if (l == "Foundation") "subject" else "group")
    }

    private fun groupScreen(b: LinearLayout) {
        b.addView(topBar(level, { go("level") }, { openSettings() }))
        b.addView(tv("Choose a group", 15f, color = C.MUT).apply { setPadding(0, 0, 0, dp(12)) })
        b.addView(option("Group I", "3 subjects") { pickGroup("Group I") })
        b.addView(option("Group II", "3 subjects") { pickGroup("Group II") })
    }

    private fun pickGroup(g: String) {
        group = g
        go("subject")
    }

    private fun subjectScreen(b: LinearLayout) {
        val foundation = level == "Foundation"
        val title = if (foundation) "Foundation" else "$level · $group"
        b.addView(topBar(title, { go(if (foundation) "level" else "group") }, { openSettings() }))
        b.addView(tv("Choose a subject", 15f, color = C.MUT).apply { setPadding(0, 0, 0, dp(12)) })
        val list = if (foundation) FOUNDATION else PAPERS["$level|$group"].orEmpty()
        list.forEachIndexed { i, s ->
            b.addView(option("${i + 1}. $s", null) { pickSubject(s) })
        }
    }

    private fun pickSubject(s: String) {
        Store.setSubject(this, if (level == "Foundation") "Foundation · $s" else "$level · $group · $s")
        go("timer")
    }

    // ---------- Page 2: timer, emergency contact, start ----------

    private fun timerScreen(b: LinearLayout): View? {
        val subject = Store.subject(this)

        if (running()) {
            b.addView(topBar("Study timer", null, { openSettings() }))
            if (subject.isNotEmpty()) b.addView(tag(subject))
            val r = RingView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(250), dp(250)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(16); bottomMargin = dp(16)
                }
            }
            ring = r
            b.addView(r)
            b.addView(tv("${Store.blocked(this).size} apps blocked. Phone calls still work.", 14f, color = C.MUT).apply { gravity = Gravity.CENTER })
            val n = Store.emergency(this).filter { it.isDigit() || it == '+' }
            if (n.isNotEmpty()) b.addView(button("Call emergency contact", C.RED) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))
            })
            b.addView(button("Hold to end session", C.INK, outline = true) {
                toast("Press and hold to end")
            }.apply {
                setOnLongClickListener {
                    Store.setEndAt(this@MainActivity, 0)
                    stopService(Intent(this@MainActivity, FocusService::class.java))
                    build(); true
                }
            })
            return null
        }

        b.addView(topBar("Study timer", { go("subject") }, { openSettings() }))
        if (subject.isNotEmpty()) b.addView(tag(subject))

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
            b.addView(pc)
        }

        val pre = RingView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(210), dp(210)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(8); bottomMargin = dp(16)
            }
        }
        pre.set(1f, "$mins:00", "minutes")
        b.addView(pre)

        val dc = card()
        dc.addView(tv("Focus time", 16f, true))
        val row = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
        val pills = mutableListOf<Pair<Int, TextView>>()
        listOf(25, 45, 60, 90).forEach { m ->
            val t = pill("$m min", m == mins) {
                mins = m
                pills.forEach { (v, tt) -> stylePill(tt, v == m) }
                pre.set(1f, "$m:00", "minutes")
            }
            pills.add(m to t)
            row.addView(t)
        }
        dc.addView(row)
        b.addView(dc)

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
        b.addView(ec)

        val dock = LinearLayout(this).apply {
            setBackgroundColor(C.CARD)
            setPadding(dp(18), dp(8), dp(18), dp(14))
        }
        dock.addView(button("Start", C.INK) { startSession(emg.text.toString().trim()) }, LinearLayout.LayoutParams(MP, dp(54)))
        return dock
    }

    private fun startSession(num: String) {
        Store.setEmergency(this, num)
        when {
            !hasUsage() || !Settings.canDrawOverlays(this) -> toast("Allow the two permissions above first")
            Store.blocked(this).isEmpty() -> toast("Choose apps to block in Settings (gear icon)")
            else -> {
                val ms = mins * 60000L
                Store.setEndAt(this, System.currentTimeMillis() + ms)
                Store.setTotal(this, ms)
                startForegroundService(Intent(this, FocusService::class.java))
                build()
            }
        }
    }

    // ---------- Settings: apps that must not disturb during study ----------

    private fun settingsScreen(b: LinearLayout) {
        b.addView(topBar("Settings", { go(settingsFrom) }, null))
        b.addView(tv("Apps that must not disturb you during study", 16f, true))
        b.addView(tv("Switch on every app you want blocked while the timer runs. Phone calls are never blocked.", 13f, color = C.MUT).apply {
            setPadding(0, dp(4), 0, dp(12))
        })
        val search = EditText(this).apply {
            hint = "Search apps"
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setTextColor(C.INK)
            background = shape(android.graphics.Color.WHITE, 10, C.LINE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        b.addView(search, LinearLayout.LayoutParams(MP, WC).apply { bottomMargin = dp(10) })

        val ac = card()
        b.addView(ac)
        val pm = packageManager
        val dial = pm.queryIntentActivities(Intent(Intent.ACTION_DIAL), 0).map { it.activityInfo.packageName }.toSet()
        val on = Store.blocked(this).toMutableSet()
        val rows = mutableListOf<Pair<String, View>>()
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != packageName && it.activityInfo.packageName !in dial }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .forEach { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val line = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                }
                line.addView(ImageView(this).apply { setImageDrawable(info.loadIcon(pm)) }, LinearLayout.LayoutParams(dp(38), dp(38)))
                line.addView(tv(label, 16f).apply { setPadding(dp(12), 0, dp(8), 0) }, LinearLayout.LayoutParams(0, WC, 1f))
                line.addView(Switch(this).apply {
                    isChecked = pkg in on
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) on.add(pkg) else on.remove(pkg)
                        Store.setBlocked(this@MainActivity, on)
                    }
                })
                ac.addView(line)
                rows.add(label.lowercase() to line)
            }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val q = (s?.toString() ?: "").trim().lowercase()
                rows.forEach { (label, v) -> v.visibility = if (label.contains(q)) View.VISIBLE else View.GONE }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
