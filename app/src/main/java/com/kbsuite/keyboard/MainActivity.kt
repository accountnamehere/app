package com.kbsuite.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var column: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(24))
        }
        scroll.addView(column, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setContentView(scroll)
        build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun title(text: String) {
        column.addView(TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, dp(12))
        })
    }

    private fun section(text: String) {
        column.addView(TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.parseColor("#0E7490"))
            setPadding(0, dp(18), 0, dp(6))
        })
    }

    private fun row(label: String, control: View, reset: View?) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        line.addView(TextView(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#1F2328"))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        line.addView(control)
        if (reset != null) line.addView(reset)
        column.addView(line)
    }

    private fun resetButton(onReset: () -> Unit): View = Button(this).apply {
        text = "↺"
        minWidth = dp(44)
        minimumWidth = dp(44)
        setOnClickListener { onReset() }
    }

    private fun toggle(label: String, key: String) {
        val sw = Switch(this)
        sw.isChecked = Prefs.bool(this, key)
        sw.setOnCheckedChangeListener { _, checked -> Prefs.putBool(this, key, checked) }
        val reset = resetButton {
            Prefs.reset(this, key)
            sw.isChecked = Prefs.bool(this, key)
        }
        row(label, sw, reset)
    }

    private fun number(label: String, key: String) {
        val field = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(Prefs.int(this@MainActivity, key).toString())
            minEms = 4
            gravity = Gravity.END
            setOnFocusChangeListener { _, focused ->
                if (!focused) {
                    val parsed = text.toString().trim().toIntOrNull()
                    if (parsed != null) Prefs.putInt(this@MainActivity, key, parsed)
                    setText(Prefs.int(this@MainActivity, key).toString())
                }
            }
        }
        val reset = resetButton {
            Prefs.reset(this, key)
            field.setText(Prefs.int(this, key).toString())
        }
        row(label, field, reset)
    }

    private fun dropdown(label: String, key: String, options: List<String>) {
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(Prefs.int(this, key).coerceIn(0, options.size - 1))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                Prefs.putInt(this@MainActivity, key, position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val reset = resetButton {
            Prefs.reset(this, key)
            spinner.setSelection(Prefs.int(this, key).coerceIn(0, options.size - 1))
        }
        row(label, spinner, reset)
    }

    private fun build() {
        title("KeySuite")

        section("SETUP")
        column.addView(Button(this).apply {
            text = "1. Enable keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        column.addView(Button(this).apply {
            text = "2. Choose keyboard"
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })

        section("APPEARANCE")
        dropdown("Theme", Prefs.THEME, listOf("Light", "Dark", "Pitch black"))
        number("Key height (dp)", Prefs.KEY_HEIGHT_DP)
        toggle("Number row", Prefs.NUMBER_ROW)

        section("TYPING")
        toggle("Auto-capitalization", Prefs.AUTO_CAPS)
        toggle("Double space for full stop", Prefs.DOUBLE_SPACE_PERIOD)
        toggle("Enter key sends", Prefs.ENTER_SENDS)
        toggle("Key popup preview", Prefs.KEY_POPUP)
        number("Double tap window (ms)", Prefs.DOUBLE_TAP_MS)
        number("Key repeat delay (ms)", Prefs.REPEAT_DELAY_MS)
        number("Key repeat rate (ms)", Prefs.REPEAT_RATE_MS)
        number("Long press delay (ms)", Prefs.LONG_PRESS_MS)

        section("CLIPBOARD")
        number("Clip history max (0 = unlimited)", Prefs.CLIP_MAX)
        toggle("Capture copied images", Prefs.CLIP_CAPTURE_IMAGES)
        toggle("Show images in clipboard", Prefs.CLIP_SHOW_IMAGES)
        toggle("Show clip titles", Prefs.CLIP_SHOW_TITLE)

        section("FEEDBACK")
        toggle("Vibrate on keypress", Prefs.HAPTIC)
        toggle("Sound on keypress", Prefs.SOUND)

        section("REPEAT SEND")
        column.addView(TextView(this).apply {
            text = "Open the 🔁 button on the keyboard toolbar while typing to use it. You always control Start and Stop yourself."
            setTextColor(Color.parseColor("#6B7280"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, dp(8))
        })

        section("TEST")
        column.addView(EditText(this).apply {
            hint = "Tap here to try the keyboard"
            minLines = 3
            gravity = Gravity.TOP
        })
    }
}
