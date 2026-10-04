package com.cafocus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

class BlockActivity : Activity() {
    override fun onCreate(b: Bundle?) { super.onCreate(b); build() }
    override fun onNewIntent(i: Intent?) { super.onNewIntent(i); build() }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = home()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onCallPermission(requestCode, grantResults)
    }

    private fun build() {
        window.statusBarColor = C.BG
        val mins = ((Store.endAt(this) - System.currentTimeMillis()) / 60000 + 1).coerceAtLeast(0)
        val subject = Store.subject(this)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(C.BG)
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        l.addView(tv("🔒", 48f).apply { gravity = Gravity.CENTER })
        l.addView(tv("This app is blocked", 26f, true).apply { gravity = Gravity.CENTER })
        if (subject.isNotEmpty()) l.addView(tv("Studying: $subject", 14f, color = C.MUT).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        l.addView(tv("Focus ends in $mins min. You can do this.", 15f, color = C.MUT).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(18))
        })
        l.addView(button("Back to study", C.INK) { home() })
        Store.contacts(this).forEach { (name, num) ->
            l.addView(button("Call $name", C.RED) { callNow(num) })
        }
        l.addView(button("Open Phone", C.INK, outline = true) { startActivity(Intent(Intent.ACTION_DIAL)) })
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(C.BG)
            addView(l)
        })
    }

    private fun home() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
