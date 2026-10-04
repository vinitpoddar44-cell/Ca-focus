package com.cafocus

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

const val MP = -1
const val WC = -2

object C {
    val BG = Color.parseColor("#E9F0E4")
    val CARD = Color.parseColor("#F8FAF4")
    val INK = Color.parseColor("#1B2A49")
    val MUT = Color.parseColor("#5C6B7A")
    val LINE = Color.parseColor("#C9D6C3")
    val RED = Color.parseColor("#C0392B")
}

fun fmtTime(sec: Long): String {
    if (sec < 60) return "${sec}s"
    val m = sec / 60
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

// Calls the number directly. Asks for the phone permission the first time, then calls.
fun Activity.callNow(number: String) {
    if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
        Store.setPendingCall(this, number)
        requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 77)
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
    } catch (e: SecurityException) {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }
}

fun Activity.onCallPermission(code: Int, res: IntArray) {
    if (code == 77 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
        val n = Store.pendingCall(this)
        if (n.isNotEmpty()) {
            Store.setPendingCall(this, "")
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$n")))
        }
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.shape(fill: Int, radius: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = dp(radius).toFloat()
    if (stroke != 0) setStroke(dp(1), stroke)
}

fun Context.tv(t: String, sp: Float, bold: Boolean = false, color: Int = C.INK): TextView = TextView(this).apply {
    text = t
    textSize = sp
    setTextColor(color)
    if (bold) setTypeface(null, Typeface.BOLD)
}

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = shape(C.CARD, 14, C.LINE)
    setPadding(dp(16), dp(14), dp(16), dp(14))
    layoutParams = LinearLayout.LayoutParams(MP, WC).apply { bottomMargin = dp(12) }
}

fun Context.button(t: String, fill: Int, outline: Boolean = false, f: () -> Unit): Button = Button(this).apply {
    text = t
    textSize = 16f
    transformationMethod = null
    setTypeface(null, Typeface.BOLD)
    setTextColor(if (outline) fill else Color.WHITE)
    background = if (outline) shape(Color.TRANSPARENT, 12, fill) else shape(fill, 12)
    stateListAnimator = null
    layoutParams = LinearLayout.LayoutParams(MP, dp(52)).apply { topMargin = dp(10) }
    setOnClickListener { f() }
}

fun Context.stylePill(t: TextView, on: Boolean) {
    t.setTextColor(if (on) Color.WHITE else C.INK)
    t.background = shape(if (on) C.INK else Color.TRANSPARENT, 20, if (on) C.INK else C.LINE)
}

fun Context.pill(t: String, on: Boolean, f: () -> Unit): TextView = TextView(this).apply {
    text = t
    textSize = 14f
    gravity = Gravity.CENTER
    setPadding(0, dp(10), 0, dp(10))
    layoutParams = LinearLayout.LayoutParams(0, WC, 1f).apply { rightMargin = dp(8) }
    stylePill(this, on)
    setOnClickListener { f() }
}

fun Context.tag(t: String): TextView = TextView(this).apply {
    text = t
    textSize = 13f
    setTextColor(Color.WHITE)
    background = shape(C.INK, 20)
    setPadding(dp(14), dp(7), dp(14), dp(7))
    layoutParams = LinearLayout.LayoutParams(WC, WC).apply { bottomMargin = dp(8) }
}

fun Context.iconBtn(sym: String, f: () -> Unit): TextView = TextView(this).apply {
    text = sym
    textSize = 20f
    gravity = Gravity.CENTER
    setTextColor(C.INK)
    background = shape(C.CARD, 21, C.LINE)
    layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
    setOnClickListener { f() }
}

fun Context.topBar(title: String, back: (() -> Unit)?, gear: (() -> Unit)?, report: (() -> Unit)? = null): LinearLayout {
    val bar = LinearLayout(this)
    bar.gravity = Gravity.CENTER_VERTICAL
    bar.setPadding(0, 0, 0, dp(16))
    if (back != null) bar.addView(iconBtn("←", back))
    val t = tv(title, if (back == null) 28f else 20f, true)
    t.setPadding(if (back == null) 0 else dp(12), 0, 0, 0)
    bar.addView(t, LinearLayout.LayoutParams(0, WC, 1f))
    if (report != null) {
        val r = iconBtn("📊", report)
        (r.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(8)
        bar.addView(r)
    }
    if (gear != null) {
        val g = iconBtn("⚙", gear)
        (g.layoutParams as LinearLayout.LayoutParams).leftMargin = dp(8)
        bar.addView(g)
    }
    return bar
}

fun Context.option(title: String, sub: String?, f: () -> Unit): LinearLayout {
    val row = LinearLayout(this)
    row.gravity = Gravity.CENTER_VERTICAL
    row.background = shape(C.CARD, 12, C.LINE)
    row.setPadding(dp(16), dp(16), dp(16), dp(16))
    row.layoutParams = LinearLayout.LayoutParams(MP, WC).apply { bottomMargin = dp(10) }
    row.setOnClickListener { f() }
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.addView(tv(title, 17f, true))
    if (sub != null) col.addView(tv(sub, 13f, color = C.MUT))
    row.addView(col, LinearLayout.LayoutParams(0, WC, 1f))
    row.addView(tv("›", 26f, color = C.MUT))
    return row
}

class RingView(c: Context) : View(c) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = C.LINE; strokeCap = Paint.Cap.ROUND }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = C.INK; strokeCap = Paint.Cap.ROUND }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.INK; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.MUT; textAlign = Paint.Align.CENTER }
    private var frac = 1f
    private var label = "00:00"
    private var caption = ""

    fun set(f: Float, l: String, cap: String) {
        frac = f; label = l; caption = cap
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat()
        val s = w * 0.045f
        track.strokeWidth = s
        arc.strokeWidth = s
        val r = RectF(s, s, w - s, w - s)
        cv.drawArc(r, 0f, 360f, false, track)
        cv.drawArc(r, -90f, 360f * frac, false, arc)
        txt.textSize = w * 0.22f
        sub.textSize = w * 0.07f
        val y = w / 2 + txt.textSize * 0.15f
        cv.drawText(label, w / 2, y, txt)
        cv.drawText(caption, w / 2, y + sub.textSize * 2f, sub)
    }
}
