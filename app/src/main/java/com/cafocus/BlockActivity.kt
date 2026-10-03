package com.cafocus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

class BlockActivity : Activity() {

    // ---------------------------------------------------------
    // ACTIVITY CREATED
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = C.BG
        window.navigationBarColor = C.BG

        buildScreen()
    }

    // ---------------------------------------------------------
    // ACTIVITY RECEIVES NEW INTENT
    // ---------------------------------------------------------

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        buildScreen()
    }

    // ---------------------------------------------------------
    // BACK BUTTON
    // ---------------------------------------------------------

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    // ---------------------------------------------------------
    // DIRECT CALL PERMISSION RESULT
    // ---------------------------------------------------------

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        /*
         * callNow() and onCallPermission() are defined in Ui.kt.
         *
         * If the student grants CALL_PHONE permission,
         * the pending emergency number is called automatically.
         */
        onCallPermission(
            requestCode,
            grantResults
        )
    }

    // ---------------------------------------------------------
    // BUILD BLOCK SCREEN
    // ---------------------------------------------------------

    private fun buildScreen() {

        val remainingMillis =
            Store.endAt(this) -
                    System.currentTimeMillis()

        val remainingMinutes =
            if (remainingMillis > 0) {
                ((remainingMillis + 59_999L) / 60_000L)
                    .coerceAtLeast(0L)
            } else {
                0L
            }

        val remainingSeconds =
            if (remainingMillis > 0) {
                ((remainingMillis + 999L) / 1_000L)
                    .coerceAtLeast(0L)
            } else {
                0L
            }

        val subject =
            Store.subject(this)

        val contacts =
            Store.contacts(this)

        // -----------------------------------------------------
        // MAIN CONTENT
        // -----------------------------------------------------

        val content = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setBackgroundColor(C.BG)

            setPadding(
                dp(28),
                dp(40),
                dp(28),
                dp(40)
            )
        }

        // -----------------------------------------------------
        // LOCK ICON
        // -----------------------------------------------------

        content.addView(
            tv(
                "🔒",
                54f
            ).apply {
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }
        )

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        content.addView(
            tv(
                "This app is blocked",
                27f,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        // -----------------------------------------------------
        // SUBJECT
        // -----------------------------------------------------

        if (subject.isNotBlank()) {

            content.addView(
                tv(
                    "Currently studying",
                    13f,
                    color = C.MUT
                ).apply {

                    gravity = Gravity.CENTER

                    setPadding(
                        0,
                        dp(18),
                        0,
                        dp(3)
                    )
                }
            )

            content.addView(
                tv(
                    subject,
                    17f,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    setPadding(
                        0,
                        0,
                        0,
                        dp(10)
                    )
                }
            )
        }

        // -----------------------------------------------------
        // REMAINING TIME
        // -----------------------------------------------------

        val timeText =
            if (remainingSeconds >= 60) {

                val hours =
                    remainingSeconds / 3600

                val minutes =
                    (remainingSeconds % 3600) / 60

                val seconds =
                    remainingSeconds % 60

                if (hours > 0) {
                    String.format(
                        "%02d:%02d:%02d",
                        hours,
                        minutes,
                        seconds
                    )
                } else {
                    String.format(
                        "%02d:%02d",
                        minutes,
                        seconds
                    )
                }

            } else {

                String.format(
                    "00:%02d",
                    remainingSeconds
                )
            }

        content.addView(
            tv(
                timeText,
                38f,
                true
            ).apply {

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(3)
                )
            }
        )

        content.addView(
            tv(
                if (remainingMinutes > 0) {
                    "Focus session is running"
                } else {
                    "Focus session is finishing"
                },
                14f,
                color = C.MUT
            ).apply {

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }
        )

        // -----------------------------------------------------
        // BACK TO STUDY BUTTON
        // -----------------------------------------------------

        content.addView(
            button(
                "Back to study",
                C.INK
            ) {
                goHome()
            }
        )

        // -----------------------------------------------------
        // EMERGENCY CONTACT SECTION
        // -----------------------------------------------------

        if (contacts.isNotEmpty()) {

            content.addView(
                tv(
                    "Emergency contacts",
                    17f,
                    true
                ).apply {

                    gravity = Gravity.CENTER

                    setPadding(
                        0,
                        dp(22),
                        0,
                        dp(4)
                    )
                }
            )

            content.addView(
                tv(
                    "Need help? Call a saved emergency contact directly.",
                    13f,
                    color = C.MUT
                ).apply {

                    gravity = Gravity.CENTER

                    setPadding(
                        0,
                        0,
                        0,
                        dp(6)
                    )
                }
            )

            contacts.forEach { contact ->

                val name =
                    contact.first

                val number =
                    contact.second

                content.addView(
                    button(
                        "Call $name",
                        C.RED
                    ) {
                        /*
                         * This uses ACTION_CALL through
                         * callNow(), so there is no dialler
                         * confirmation screen after permission
                         * has been granted.
                         */
                        callNow(number)
                    }
                )
            }
        } else {

            // -------------------------------------------------
            // NO CONTACTS
            // -------------------------------------------------

            content.addView(
                tv(
                    "No emergency contacts have been added.",
                    13f,
                    color = C.MUT
                ).apply {

                    gravity = Gravity.CENTER

                    setPadding(
                        0,
                        dp(18),
                        0,
                        0
                    )
                }
            )
        }

        // -----------------------------------------------------
        // HOME / PHONE BUTTON
        // -----------------------------------------------------

        content.addView(
            button(
                "Open Phone",
                C.INK,
                outline = true
            ) {

                /*
                 * This opens the phone application.
                 * Emergency contact buttons above use
                 * direct calling instead.
                 */
                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_DIAL
                        )
                    )

                } catch (e: Exception) {
                    // Ignore if the device has no dialer.
                }
            }
        )

        // -----------------------------------------------------
        // SCROLL VIEW
        // -----------------------------------------------------

        val scroll =
            ScrollView(this).apply {

                isFillViewport = true

                setBackgroundColor(C.BG)

                addView(content)
            }

        setContentView(scroll)
    }

    // ---------------------------------------------------------
    // RETURN TO HOME SCREEN
    // ---------------------------------------------------------

    private fun goHome() {

        /*
         * The blocked app should not remain visible.
         * We send the student to the Android home screen.
         */
        try {

            val intent =
                Intent(
                    Intent.ACTION_MAIN
                ).apply {

                    addCategory(
                        Intent.CATEGORY_HOME
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(intent)

        } catch (e: Exception) {
            // Ignore.
        }

        finish()
    }
}
