package com.kbsuite.keyboard.clip

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class LockPrompt(
    context: Context,
    heightPx: Int,
    private val onUnlocked: () -> Unit,
    private val onCancel: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#14181C"))
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        setPadding(dp(16), dp(10), dp(16), dp(10))

        val mode = ClipLock.mode(context)
        if (mode == ClipLock.MODE_TAP_COUNT) {
            var taps = 0
            val target = ClipLock.tapTarget(context)
            val status = TextView(context).apply {
                text = "Tap $target times to open the clipboard"
                setTextColor(Color.WHITE)
            }
            addView(status)
            addView(Button(context).apply {
                text = "Tap"
                setOnClickListener {
                    taps += 1
                    status.text = "$taps / $target"
                    if (taps >= target) onUnlocked()
                }
            })
        } else if (mode == ClipLock.MODE_HIDDEN_SPOT) {
            addView(TextView(context).apply {
                text = "."
                setTextColor(Color.parseColor("#14181C"))
                setOnClickListener { onUnlocked() }
            })
        } else {
            addView(TextView(context).apply {
                text = "Enter clipboard PIN"
                setTextColor(Color.WHITE)
            })
            val field = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setTextColor(Color.WHITE)
            }
            addView(field)
            addView(Button(context).apply {
                text = "Unlock"
                setOnClickListener {
                    if (ClipLock.check(context, field.text.toString())) onUnlocked()
                    else field.setText("")
                }
            })
        }
        addView(Button(context).apply {
            text = "Cancel"
            setOnClickListener { onCancel() }
        })
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
