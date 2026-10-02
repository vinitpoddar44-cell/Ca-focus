package com.cafocus

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout

class BlockActivity : Activity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); build() }
    override fun onNewIntent(i: Intent?) { super.onNewIntent(i); build() }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = home()

    private fun build() {
        val mins = ((Store.endAt(this) - System.currentTimeMillis()) / 60000 + 1).coerceAtLeast(0)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60); setBackgroundColor(Color.parseColor("#E9F0E4"))
        }
        l.addView(tv("This app is blocked", 26f, true))
        l.addView(tv("Focus ends in $mins min.", 16f, false))
        l.addView(btn("Back to study") { home() })
        val n = Store.emergency(this).filter { it.isDigit() || it == '+' }
        if (n.isNotEmpty()) l.addView(btn("Call emergency contact") { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) })
        l.addView(btn("Open Phone") { startActivity(Intent(Intent.ACTION_DIAL)) })
        setContentView(l)
    }

    private fun home() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
