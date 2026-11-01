package com.kbsuite.keyboard.repeat

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class RepeatPanel(
    context: Context,
    private val heightPx: Int,
    private val connectionProvider: () -> InputConnection?,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var remaining = 0
    private var runnable: Runnable? = null

    private val messageField = EditText(context)
    private val countField = EditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER }
    private val delayField = EditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER }
    private val lineFeedToggle = android.widget.Switch(context)
    private val statusText = TextView(context)
    private val startStopButton = Button(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#14181C"))
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        setPadding(dp(10), dp(6), dp(10), dp(6))

        val header = LinearLayout(context).apply { orientation = HORIZONTAL }
        header.addView(
            TextView(context).apply {
                text = "Repeat send (your own device, you control Stop)"
                setTextColor(Color.WHITE)
            },
            LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        header.addView(Button(context).apply { text = "Close"; setOnClickListener { stop(); onClose() } })
        addView(header)

        messageField.hint = "Message to repeat"
        messageField.setTextColor(Color.WHITE)
        addView(messageField, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row1 = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row1.addView(TextView(context).apply { text = "Times"; setTextColor(Color.WHITE) })
        countField.setText("1")
        row1.addView(countField, LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT))
        row1.addView(TextView(context).apply { text = "Delay ms"; setTextColor(Color.WHITE) })
        delayField.setText("1000")
        row1.addView(delayField, LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(row1)

        val row2 = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row2.addView(TextView(context).apply { text = "Line-feed mode (no send key)"; setTextColor(Color.WHITE) }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(lineFeedToggle)
        addView(row2)

        statusText.setTextColor(Color.parseColor("#8A8F98"))
        addView(statusText)

        startStopButton.text = "Start"
        startStopButton.setOnClickListener {
            if (running) stop() else start()
        }
        addView(startStopButton)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun start() {
        val message = messageField.text.toString()
        val count = countField.text.toString().toIntOrNull() ?: 1
        val delay = delayField.text.toString().toLongOrNull() ?: 1000L
        if (message.isBlank() || count <= 0) return
        running = true
        remaining = count
        startStopButton.text = "Stop"
        statusText.text = "Running, $remaining left"
        val step = object : Runnable {
            override fun run() {
                if (!running || remaining <= 0) {
                    stop()
                    return
                }
                val ic = connectionProvider()
                if (ic != null) {
                    ic.commitText(message, 1)
                    if (lineFeedToggle.isChecked) {
                        ic.commitText("\n", 1)
                    } else {
                        val now = SystemClock.uptimeMillis()
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
                    }
                }
                remaining -= 1
                statusText.text = "Running, $remaining left"
                if (remaining > 0 && running) {
                    handler.postDelayed(this, delay)
                } else {
                    stop()
                }
            }
        }
        runnable = step
        handler.post(step)
    }

    private fun stop() {
        running = false
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
        startStopButton.text = "Start"
        statusText.text = "Stopped"
    }

    fun onDetach() {
        stop()
    }
}
