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
    val BG = Color.parseColor("#E9F1E5")
    val CARD = Color.parseColor("#F9FBF6")
    val WHITE = Color.WHITE
    val INK = Color.parseColor("#153F38")
    val INK2 = Color.parseColor("#1F5B4E")
    val MUT = Color.parseColor("#68756F")
    val LINE = Color.parseColor("#CFDCCF")
    val GREEN = Color.parseColor("#16866B")
    val LIGHT_GREEN = Color.parseColor("#DCEDE3")
    val RED = Color.parseColor("#C0392B")
}

fun fmtTime(sec: Long): String {
    if (sec < 60) return "${sec}s"

    val m = sec / 60

    return if (m >= 60) {
        "${m / 60}h ${m % 60}m"
    } else {
        "${m}m"
    }
}

/*
 * Direct calling.
 * There is intentionally NO fallback to ACTION_DIAL.
 */
fun Activity.callNow(number: String) {

    if (checkSelfPermission(
            Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Store.setPendingCall(this, number)

        requestPermissions(
            arrayOf(Manifest.permission.CALL_PHONE),
            77
        )

        return
    }

    try {
        val intent = Intent(
            Intent.ACTION_CALL,
            Uri.parse("tel:$number")
        )

        startActivity(intent)

    } catch (e: Exception) {
        android.widget.Toast.makeText(
            this,
            "Unable to make the call",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

fun Activity.onCallPermission(
    code: Int,
    res: IntArray
) {

    if (code != 77) return

    if (
        res.isNotEmpty() &&
        res[0] == PackageManager.PERMISSION_GRANTED
    ) {

        val number = Store.pendingCall(this)

        Store.setPendingCall(this, "")

        if (number.isNotEmpty()) {
            callNow(number)
        }

    } else {

        Store.setPendingCall(this, "")

        android.widget.Toast.makeText(
            this,
            "Direct calling permission was not granted.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

fun Context.dp(v: Int): Int {
    return (v * resources.displayMetrics.density).toInt()
}

fun Context.shape(
    fill: Int,
    radius: Int,
    stroke: Int = 0
): GradientDrawable {

    return GradientDrawable().apply {

        setColor(fill)

        cornerRadius =
            dp(radius).toFloat()

        if (stroke != 0) {
            setStroke(
                dp(1),
                stroke
            )
        }
    }
}

fun Context.tv(
    t: String,
    sp: Float,
    bold: Boolean = false,
    color: Int = C.INK
): TextView {

    return TextView(this).apply {

        text = t

        textSize = sp

        setTextColor(color)

        if (bold) {
            setTypeface(
                null,
                Typeface.BOLD
            )
        }
    }
}

fun Context.card(): LinearLayout {

    return LinearLayout(this).apply {

        orientation =
            LinearLayout.VERTICAL

        background =
            shape(
                C.CARD,
                18,
                C.LINE
            )

        setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        layoutParams =
            LinearLayout.LayoutParams(
                MP,
                WC
            ).apply {
                bottomMargin =
                    dp(14)
            }
    }
}

fun Context.button(
    t: String,
    fill: Int,
    outline: Boolean = false,
    f: () -> Unit
): Button {

    return Button(this).apply {

        text = t

        textSize = 15f

        transformationMethod = null

        setTypeface(
            null,
            Typeface.BOLD
        )

        setTextColor(
            if (outline)
                fill
            else
                Color.WHITE
        )

        background =
            if (outline) {

                shape(
                    Color.TRANSPARENT,
                    14,
                    fill
                )

            } else {

                shape(
                    fill,
                    14
                )
            }

        stateListAnimator = null

        layoutParams =
            LinearLayout.LayoutParams(
                MP,
                dp(52)
            ).apply {
                topMargin =
                    dp(10)
            }

        setOnClickListener {
            f()
        }
    }
}

fun Context.stylePill(
    t: TextView,
    on: Boolean
) {

    t.setTextColor(
        if (on)
            Color.WHITE
        else
            C.INK
    )

    t.background =
        shape(
            if (on)
                C.INK
            else
                Color.TRANSPARENT,
            20,
            if (on)
                C.INK
            else
                C.LINE
        )
}

fun Context.pill(
    t: String,
    on: Boolean,
    f: () -> Unit
): TextView {

    return TextView(this).apply {

        text = t

        textSize = 14f

        gravity =
            Gravity.CENTER

        setTypeface(
            null,
            Typeface.BOLD
        )

        setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        layoutParams =
            LinearLayout.LayoutParams(
                0,
                WC,
                1f
            ).apply {
                rightMargin =
                    dp(7)
            }

        stylePill(
            this,
            on
        )

        setOnClickListener {
            f()
        }
    }
}

fun Context.tag(
    t: String
): TextView {

    return TextView(this).apply {

        text = "•  $t"

        textSize = 13f

        setTypeface(
            null,
            Typeface.BOLD
        )

        setTextColor(
            C.INK
        )

        background =
            shape(
                C.LIGHT_GREEN,
                20
            )

        setPadding(
            dp(14),
            dp(8),
            dp(14),
            dp(8)
        )

        layoutParams =
            LinearLayout.LayoutParams(
                WC,
                WC
            ).apply {
                bottomMargin =
                    dp(10)
            }
    }
}

fun Context.iconBtn(
    sym: String,
    f: () -> Unit
): TextView {

    return TextView(this).apply {

        text = sym

        textSize = 20f

        gravity =
            Gravity.CENTER

        setTextColor(
            C.INK
        )

        background =
            shape(
                C.CARD,
                22,
                C.LINE
            )

        layoutParams =
            LinearLayout.LayoutParams(
                dp(44),
                dp(44)
            )

        setOnClickListener {
            f()
        }
    }
}

fun Context.topBar(
    title: String,
    back: (() -> Unit)?,
    gear: (() -> Unit)?,
    report: (() -> Unit)? = null
): LinearLayout {

    val bar =
        LinearLayout(this)

    bar.gravity =
        Gravity.CENTER_VERTICAL

    bar.setPadding(
        0,
        0,
        0,
        dp(18)
    )

    if (back != null) {

        bar.addView(
            iconBtn(
                "‹",
                back
            )
        )
    }

    val titleView =
        tv(
            title,
            if (back == null)
                28f
            else
                21f,
            true
        )

    titleView.setPadding(
        if (back == null)
            0
        else
            dp(10),
        0,
        0,
        0
    )

    bar.addView(
        titleView,
        LinearLayout.LayoutParams(
            0,
            WC,
            1f
        )
    )

    if (report != null) {

        val r =
            iconBtn(
                "▥",
                report
            )

        (r.layoutParams as LinearLayout.LayoutParams)
            .leftMargin =
            dp(7)

        bar.addView(r)
    }

    if (gear != null) {

        val g =
            iconBtn(
                "⚙",
                gear
            )

        (g.layoutParams as LinearLayout.LayoutParams)
            .leftMargin =
            dp(7)

        bar.addView(g)
    }

    return bar
}

fun Context.option(
    title: String,
    sub: String?,
    f: () -> Unit
): LinearLayout {

    val row =
        LinearLayout(this)

    row.gravity =
        Gravity.CENTER_VERTICAL

    row.background =
        shape(
            C.CARD,
            17,
            C.LINE
        )

    row.setPadding(
        dp(15),
        dp(15),
        dp(15),
        dp(15)
    )

    row.layoutParams =
        LinearLayout.LayoutParams(
            MP,
            WC
        ).apply {
            bottomMargin =
                dp(12)
        }

    row.setOnClickListener {
        f()
    }

    val circle =
        TextView(this).apply {

            text = "›"

            textSize = 22f

            gravity =
                Gravity.CENTER

            setTextColor(
                C.INK
            )

            background =
                shape(
                    C.LIGHT_GREEN,
                    24
                )
        }

    row.addView(
        circle,
        LinearLayout.LayoutParams(
            dp(42),
            dp(42)
        )
    )

    val col =
        LinearLayout(this)

    col.orientation =
        LinearLayout.VERTICAL

    col.setPadding(
        dp(13),
        0,
        dp(8),
        0
    )

    col.addView(
        tv(
            title,
            16f,
            true
        )
    )

    if (sub != null) {

        col.addView(
            tv(
                sub,
                12.5f,
                false,
                C.MUT
            )
        )
    }

    row.addView(
        col,
        LinearLayout.LayoutParams(
            0,
            WC,
            1f
        )
    )

    row.addView(
        tv(
            "›",
            27f,
            false,
            C.MUT
        )
    )

    return row
}

class RingView(
    c: Context
) : View(c) {

    private val track =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            style =
                Paint.Style.STROKE

            color =
                C.LINE

            strokeCap =
                Paint.Cap.ROUND
        }

    private val arc =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            style =
                Paint.Style.STROKE

            color =
                C.GREEN

            strokeCap =
                Paint.Cap.ROUND
        }

    private val txt =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color =
                C.INK

            textAlign =
                Paint.Align.CENTER

            typeface =
                Typeface.DEFAULT_BOLD
        }

    private val sub =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color =
                C.MUT

            textAlign =
                Paint.Align.CENTER
        }

    private var frac = 1f
    private var label = "00:00"
    private var caption = ""

    fun set(
        f: Float,
        l: String,
        cap: String
    ) {

        frac = f.coerceIn(
            0f,
            1f
        )

        label = l
        caption = cap

        invalidate()
    }

    override fun onDraw(
        cv: Canvas
    ) {

        val w =
            width.toFloat()

        val stroke =
            w * 0.045f

        track.strokeWidth =
            stroke

        arc.strokeWidth =
            stroke

        val r =
            RectF(
                stroke,
                stroke,
                w - stroke,
                w - stroke
            )

        cv.drawArc(
            r,
            0f,
            360f,
            false,
            track
        )

        cv.drawArc(
            r,
            -90f,
            360f * frac,
            false,
            arc
        )

        txt.textSize =
            w * 0.21f

        sub.textSize =
            w * 0.07f

        val y =
            w / 2 +
                    txt.textSize *
                    0.15f

        cv.drawText(
            label,
            w / 2,
            y,
            txt
        )

        cv.drawText(
            caption,
            w / 2,
            y +
                    sub.textSize *
                    2f,
            sub
        )
    }
}
