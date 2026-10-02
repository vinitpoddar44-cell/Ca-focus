package com.cafocus

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView

fun Context.tv(t: String, sp: Float, bold: Boolean) = TextView(this).apply {
    text = t; textSize = sp; setTextColor(Color.parseColor("#1B2A49")); setPadding(0, 16, 0, 16)
    if (bold) setTypeface(null, Typeface.BOLD)
}

fun Context.btn(t: String, f: () -> Unit) = Button(this).apply {
    text = t; setOnClickListener { f() }
}
