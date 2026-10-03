package com.cafocus

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.ContactsContract
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
import java.util.Calendar

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

private const val REQ_CONTACT = 11
private const val REQ_RINGTONE = 12
private const val REQ_CALL_ONLY = 78
private const val MAX_CONTACTS = 5

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var screen = "level"
    private var returnTo = "level"
    private var level = ""
    private var group = ""
    private var mins = 45
    private var range = "day"
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
        // If the phone stopped our service before the session was saved, save it now.
        if (Store.startAt(this) != 0L && System.currentTimeMillis() > Store.endAt(this) + 5000) Store.finishSession(this)
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
            "settings" -> go(returnTo)
            "report" -> go(returnTo)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onCallPermission(requestCode, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        if (requestCode == REQ_CONTACT) {
            val uri = data.data ?: return
            val cols = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            contentResolver.query(uri, cols, null, null, null)?.use { c ->
                if (c.moveToFirst()) saveContact(c.getString(1) ?: "Contact", c.getString(0) ?: "")
            }
        }
        if (requestCode == REQ_RINGTONE) {
            val u = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            Store.setRingtone(this, u?.toString() ?: "")
        }
    }

    private fun running() = Store.endAt(this) > System.currentTimeMillis()
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
    private fun go(s: String) { screen = s; build() }

    private fun openSettings() {
        if (running()) { toast("Finish or end the session to change apps"); return }
        returnTo = screen
        go("settings")
    }

    private fun openReport() {
        returnTo = screen
        go("report")
    }

    @Suppress("DEPRECATION")
    private fun hasUsage(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun hasCall() = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    private fun update(): Boolean {
        val left = Store.endAt(this) - System.currentTimeMillis()
        if (left <= 0) { h.postDelayed({ build() }, 900); return false }
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
            "report" -> reportScreen(body)
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
        b.addView(topBar("CA Focus", null, { openSettings() }, { openReport() }))
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
        b.addView(topBar(level, { go("level") }, { openSettings() }, { openReport() }))
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
        b.addView(topBar(title, { go(if (foundation) "level" else "group") }, { openSettings() }, { openReport() }))
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

    // ---------- Page 2: timer, sound, emergency contacts, start ----------

    private fun timerScreen(b: LinearLayout): View? {
        val subject = Store.subject(this)

        if (running()) {
            b.addView(topBar("Study timer", null, { openSettings() }, { openReport() }))
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
            Store.contacts(this).forEach { (name, num) ->
                b.addView(button("Call $name", C.RED) { callNow(num) })
            }
            b.addView(button("Hold to end session", C.INK, outline = true) {
                toast("Press and hold to end")
            }.apply {
                setOnLongClickListener {
                    val secs = Store.finishSession(this@MainActivity)
                    stopService(Intent(this@MainActivity, FocusService::class.java))
                    toast(if (secs >= 30) "Session ended. ${fmtTime(secs)} saved." else "Session ended. Too short to save.")
                    build(); true
                }
            })
            return null
        }

        b.addView(topBar("Study timer", { go("subject") }, { openSettings() }, { openReport() }))
        if (subject.isNotEmpty()) b.addView(tag(subject))

        if (Alarm.ringing) {
            val bc = card()
            bc.addView(tv("Session complete. Well done!", 16f, true))
            bc.addView(button("Stop sound", C.RED) { Alarm.stop(); build() })
            b.addView(bc)
        }

        val usage = hasUsage()
        val overlay = Settings.canDrawOverlays(this)
        val call = hasCall()
        if (!usage || !overlay || !call) {
            val pc = card()
            pc.addView(tv("Set up permissions", 16f, true))
            pc.addView(tv("CA Focus needs these to block apps and to call your emergency contacts directly.", 13f, color = C.MUT))
            if (!usage) pc.addView(button("Allow usage access", C.INK) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
            if (!overlay) pc.addView(button("Allow display over other apps", C.INK) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            })
            if (!call) pc.addView(button("Allow direct calling", C.INK) {
                requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL_ONLY)
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

        // Focus time: quick buttons plus a custom box
        val presets = listOf(25, 45, 60, 90)
        val dc = card()
        dc.addView(tv("Focus time", 16f, true))
        val custom = EditText(this).apply {
            hint = "Or type your own minutes (1 to 600)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(C.INK)
            background = shape(android.graphics.Color.WHITE, 10, C.LINE)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            if (mins !in presets) setText("$mins")
        }
        val row = LinearLayout(this).apply { setPadding(0, dp(10), 0, dp(10)) }
        val pills = mutableListOf<Pair<Int, TextView>>()
        presets.forEach { m ->
            val t = pill("$m min", m == mins) {
                mins = m
                pills.forEach { (v, tt) -> stylePill(tt, v == m) }
                pre.set(1f, "$m:00", "minutes")
                custom.setText("")
            }
            pills.add(m to t)
            row.addView(t)
        }
        dc.addView(row)
        dc.addView(custom)
        custom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v in 1..600) {
                    mins = v
                    pre.set(1f, "$v:00", "minutes")
                    pills.forEach { (pv, tt) -> stylePill(tt, pv == v) }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        b.addView(dc)

        // Timer end sound
        val sc = card()
        sc.addView(tv("Timer end sound", 16f, true))
        sc.addView(tv(soundName(), 13f, color = C.MUT))
        sc.addView(button("Choose ringtone", C.INK, outline = true) { pickRingtone() })
        sc.addView(button(if (Alarm.ringing) "Stop sound" else "Play sound", C.INK, outline = true) {
            if (Alarm.ringing) Alarm.stop() else Alarm.play(this, 6000)
            build()
        })
        b.addView(sc)

        // Emergency contacts
        val cc = card()
        cc.addView(tv("Emergency contacts", 16f, true))
        cc.addView(tv("During study, tap Call to ring them directly. You can add up to $MAX_CONTACTS.", 13f, color = C.MUT).apply {
            setPadding(0, 0, 0, dp(6))
        })
        val list = Store.contacts(this)
        list.forEachIndexed { i, pair ->
            val line = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.addView(tv(pair.first, 16f, true))
            col.addView(tv(pair.second, 13f, color = C.MUT))
            line.addView(col, LinearLayout.LayoutParams(0, WC, 1f))
            line.addView(iconBtn("✕") {
                Store.setContacts(this, list.filterIndexed { j, _ -> j != i })
                build()
            })
            cc.addView(line)
        }
        if (list.size < MAX_CONTACTS) {
            val nameEt = EditText(this).apply {
                hint = "Name"
                inputType = InputType.TYPE_CLASS_TEXT
                setTextColor(C.INK)
                background = shape(android.graphics.Color.WHITE, 10, C.LINE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            val numEt = EditText(this).apply {
                hint = "Phone number"
                inputType = InputType.TYPE_CLASS_PHONE
                setTextColor(C.INK)
                background = shape(android.graphics.Color.WHITE, 10, C.LINE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
            }
            cc.addView(nameEt, LinearLayout.LayoutParams(MP, WC).apply { topMargin = dp(8) })
            cc.addView(numEt, LinearLayout.LayoutParams(MP, WC).apply { topMargin = dp(8) })
            cc.addView(button("Add contact", C.INK) {
                if (saveContact(nameEt.text.toString(), numEt.text.toString())) build()
            })
            cc.addView(button("Choose from phonebook", C.INK, outline = true) {
                startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), REQ_CONTACT)
            })
        }
        b.addView(cc)

        val dock = LinearLayout(this).apply {
            setBackgroundColor(C.CARD)
            setPadding(dp(18), dp(8), dp(18), dp(14))
        }
        dock.addView(button("Start", C.INK) { startSession(custom.text.toString().trim()) }, LinearLayout.LayoutParams(MP, dp(54)))
        return dock
    }

    private fun saveContact(name: String, number: String): Boolean {
        val num = number.filter { it.isDigit() || it == '+' }
        val nm = name.replace("\t", " ").replace("\n", " ").trim().ifEmpty { "Contact" }
        val list = Store.contacts(this)
        if (num.length < 3) { toast("Enter a valid phone number"); return false }
        if (list.size >= MAX_CONTACTS) { toast("You can add up to $MAX_CONTACTS contacts"); return false }
        if (list.any { it.second == num }) { toast("That number is already added"); return false }
        Store.setContacts(this, list + (nm to num))
        return true
    }

    private fun soundName(): String {
        val s = Store.ringtone(this)
        if (s.isEmpty()) return "Default alarm"
        return try {
            RingtoneManager.getRingtone(this, Uri.parse(s))?.getTitle(this) ?: "Custom ringtone"
        } catch (e: Exception) {
            "Custom ringtone"
        }
    }

    private fun pickRingtone() {
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_NOTIFICATION)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Timer end sound")
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        val cur = Store.ringtone(this)
        if (cur.isNotEmpty()) i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(cur))
        startActivityForResult(i, REQ_RINGTONE)
    }

    private fun startSession(custom: String) {
        if (custom.isNotEmpty()) {
            val v = custom.toIntOrNull()
            if (v == null || v < 1 || v > 600) { toast("Custom time must be 1 to 600 minutes"); return }
            mins = v
        }
        when {
            !hasUsage() || !Settings.canDrawOverlays(this) -> toast("Allow usage access and display over other apps first")
            Store.blocked(this).isEmpty() -> toast("Choose apps to block in Settings (gear icon)")
            else -> {
                Alarm.stop()
                val now = System.currentTimeMillis()
                val ms = mins * 60000L
                Store.setStartAt(this, now)
                Store.setEndAt(this, now + ms)
                Store.setTotal(this, ms)
                startForegroundService(Intent(this, FocusService::class.java))
                build()
            }
        }
    }

    // ---------- Report: day, week, month, per subject ----------

    private fun reportScreen(b: LinearLayout) {
        b.addView(topBar("Study report", { go(returnTo) }, null))
        val tabs = LinearLayout(this).apply { setPadding(0, 0, 0, dp(14)) }
        listOf("day" to "Today", "week" to "This week", "month" to "This month").forEach { (k, label) ->
            tabs.addView(pill(label, k == range) { range = k; build() })
        }
        b.addView(tabs)

        val cal = Calendar.getInstance()
        when (range) {
            "week" -> cal.add(Calendar.DAY_OF_YEAR, -((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            "month" -> cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        val since = Store.dateStr(cal.timeInMillis)
        val rows = Store.logs(this).filter { it.
