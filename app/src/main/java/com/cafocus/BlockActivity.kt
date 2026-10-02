package com.cafocus

import android.app.Activity
import android.content.Intent
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
        window.statusBarColor = C.BG
        val mins = ((Store.endAt(this) - System.currentTimeMillis()) / 60000 + 1).coerceAtLeast(0)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(C.BG)
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        l.addView(tv("🔒", 48f).apply { gravity = Gravity.CENTER })
        l.addView(tv("This app is blocked", 26f, true).apply { gravity = Gravity.CENTER })
        l.addView(tv("Focus ends in $mins min. You can do this.", 15f, color = C.MUT).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(18))
        })
        l.addView(button("Back to study", C.INK) { home() })
        val n = Store.emergency(this).filter { it.isDigit() || it == '+' }
        if (n.isNotEmpty()) l.addView(button("Call emergency contact", C.RED) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))
        })
        l.addView(button("Open Phone", C.INK, outline = true) { startActivity(Intent(Intent.ACTION_DIAL)) })
        setContentView(l)
    }

    private fun home() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
