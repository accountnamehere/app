package com.kbsuite.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import com.kbsuite.keyboard.clip.ClipLock
import kotlin.math.hypot

class MainActivity : Activity() {

    private class AppPalette(
        val bg: Int,
        val card: Int,
        val text: Int,
        val sub: Int,
        val accent: Int,
        val onAccent: Int,
        val line: Int,
        val lightBars: Boolean
    )

    private val palettes = listOf(
        AppPalette(0xFFF3F5F8.toInt(), 0xFFFFFFFF.toInt(), 0xFF111827.toInt(), 0xFF6B7280.toInt(), 0xFF0E7490.toInt(), 0xFFFFFFFF.toInt(), 0xFFD9DEE6.toInt(), true),
        AppPalette(0xFF12151A.toInt(), 0xFF1C2027.toInt(), 0xFFEDF1F7.toInt(), 0xFF9AA3AF.toInt(), 0xFF38BDF8.toInt(), 0xFF04121C.toInt(), 0xFF2B313B.toInt(), false),
        AppPalette(0xFF000000.toInt(), 0xFF0B0C0E.toInt(), 0xFFF5F5F5.toInt(), 0xFF8A8F98.toInt(), 0xFF22D3EE.toInt(), 0xFF00141A.toInt(), 0xFF1A1C20.toInt(), false)
    )

    private lateinit var pal: AppPalette
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var chips: LinearLayout
    private lateinit var chipScroll: HorizontalScrollView
    private lateinit var searchField: EditText
    private lateinit var themeButton: TextView
    private var currentTab = Registry.TAB_KEYS
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pal = palettes[Prefs.int(this, Prefs.APP_THEME).coerceIn(0, palettes.size - 1)]
        setContentView(buildShell())
        renderContent(false)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int = 0, strokeDp: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    private fun text(
        value: CharSequence,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun applyBars() {
        window.statusBarColor = pal.bg
        window.navigationBarColor = pal.bg
        var flags = 0
        if (pal.lightBars) {
            flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = flags
    }

    private fun buildShell(): View {
        applyBars()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pal.bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(12), dp(4))
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(text("KeySuite", 26f, pal.text, true))
        titleBox.addView(text("Your keyboard, every detail yours", 13f, pal.sub))
        header.addView(titleBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        themeButton = text("◐", 22f, pal.accent).apply {
            gravity = Gravity.CENTER
            background = rounded(pal.card, 22, pal.line, 1)
            setOnClickListener { cycleAppTheme() }
        }
        header.addView(themeButton, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)

        searchField = EditText(this).apply {
            hint = "Search every setting"
            setHintTextColor(pal.sub)
            setTextColor(pal.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = rounded(pal.card, 14, pal.line, 1)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            setText(query)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val next = s?.toString().orEmpty()
                    if (next != query) {
                        query = next
                        renderContent(true)
                    }
                }
            })
        }
        val searchLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        searchLp.setMargins(dp(14), dp(8), dp(14), dp(6))
        root.addView(searchField, searchLp)

        chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(4), dp(10), dp(8))
        }
        chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chips)
        }
        root.addView(chipScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(28))
        }
        scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun cycleAppTheme() {
        val next = (Prefs.int(this, Prefs.APP_THEME) + 1) % palettes.size
        Prefs.putInt(this, Prefs.APP_THEME, next)
        val frame = findViewById<ViewGroup>(android.R.id.content)
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0) {
            pal = palettes[next]
            setContentView(buildShell())
            renderContent(false)
            return
        }
        val snapshot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        frame.draw(Canvas(snapshot))
        val cx = (themeButton.left + themeButton.right) / 2 + (themeButton.parent as View).left
        val cy = (themeButton.top + themeButton.bottom) / 2 + (themeButton.parent as View).top
        pal = palettes[next]
        setContentView(buildShell())
        renderContent(false)
        val overlay = ImageView(this).apply {
            setImageBitmap(snapshot)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        frame.addView(overlay, FrameLayout.LayoutParams(w, h))
        val radius = hypot(w.toFloat(), h.toFloat())
        val animator = ViewAnimationUtils.createCircularReveal(overlay, cx, cy, radius, 0f)
        animator.duration = 620
        animator.interpolator = android.view.animation.DecelerateInterpolator(1.4f)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                frame.removeView(overlay)
            }
        })
        animator.start()
    }

    private fun renderChips() {
        chips.removeAllViews()
        chipScroll.visibility = if (query.isBlank()) View.VISIBLE else View.GONE
        for (tab in Registry.tabs) {
            val selected = tab == currentTab
            val chip = text(tab, 14f, if (selected) pal.onAccent else pal.text, selected).apply {
                background = rounded(if (selected) pal.accent else pal.card, 18, pal.line, if (selected) 0 else 1)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener {
                    if (currentTab != tab) {
                        currentTab = tab
                        renderContent(true)
                    }
                }
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(4), 0, dp(4), 0)
            chips.addView(chip, lp)
        }
    }

    private fun renderContent(animate: Boolean) {
        renderChips()
        content.removeAllViews()
        scroll.scrollTo(0, 0)
        val views = ArrayList<View>()
        if (query.isNotBlank()) {
            val found = Registry.search(query)
            if (found.isEmpty()) {
                views.add(text("Nothing matches \"${query.trim()}\"", 15f, pal.sub).apply { setPadding(dp(6), dp(20), dp(6), dp(20)) })
            } else {
                views.add(sectionLabel("${found.size} result" + if (found.size == 1) "" else "s"))
                found.forEach { views.add(settingRow(it, true)) }
            }
        } else {
            if (currentTab == Registry.TAB_KEYS) views.add(setupCard())
            var lastSection = ""
            for (def in Registry.forTab(currentTab)) {
                if (def.section != lastSection) {
                    views.add(sectionLabel(def.section))
                    lastSection = def.section
                }
                views.add(settingRow(def, false))
            }
            if (currentTab == Registry.TAB_CLIP) {
                views.add(sectionLabel("Lock"))
                views.add(lockCard())
            }
            if (currentTab == Registry.TAB_MODES) {
                views.add(sectionLabel("Repeat send"))
                views.add(infoCard("Open the 🔁 button on the keyboard toolbar while typing. Choose the message, how many times and the delay. A Stop button stays visible the whole time, and you start and stop it yourself."))
                views.add(sectionLabel("Clipboard panel"))
                views.add(infoCard("The 📋 button opens your clip history with search, pinning and delete. If you set a lock, it asks first."))
            }
            if (currentTab == Registry.TAB_KEYS) {
                views.add(sectionLabel("Try it"))
                views.add(testCard())
            }
        }
        views.forEachIndexed { index, view ->
            content.addView(view)
            if (animate) {
                view.alpha = 0f
                view.translationY = dp(14).toFloat()
                view.animate().alpha(1f).translationY(0f).setStartDelay((index * 28L).coerceAtMost(280L)).setDuration(240).start()
            }
        }
    }

    private fun sectionLabel(label: String): View =
        text(label.uppercase(), 12f, pal.accent, true).apply {
            letterSpacing = 0.08f
            setPadding(dp(6), dp(16), dp(6), dp(6))
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(pal.card, 16, pal.line, 1)
        setPadding(dp(14), dp(10), dp(10), dp(10))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(4), 0, dp(4))
        }
    }

    private fun highlighted(value: String): CharSequence {
        val q = query.trim()
        if (q.isEmpty()) return value
        val i = value.lowercase().indexOf(q.lowercase())
        if (i < 0) return value
        val span = SpannableString(value)
        span.setSpan(BackgroundColorSpan(withAlpha(pal.accent, 110)), i, i + q.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    private fun isDefault(def: Def): Boolean {
        return when (def.kind) {
            Kind.TOGGLE -> Prefs.bool(this, def.id) == (Prefs.default(def.id) as Boolean)
            else -> Prefs.int(this, def.id) == (Prefs.default(def.id) as Int)
        }
    }

    private fun settingRow(def: Def, showPath: Boolean): View {
        val card = card()
        if (showPath) {
            card.addView(text("${def.tab}  ›  ${def.section}", 11f, pal.sub))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            text(highlighted(def.title), 16f, pal.text),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val infoText = text(highlighted(def.info), 13f, pal.sub).apply {
            visibility = View.GONE
            setPadding(0, dp(8), dp(6), dp(2))
        }
        val reset = text("↺", 20f, pal.sub).apply {
            gravity = Gravity.CENTER
        }
        fun refreshReset() {
            val atDefault = isDefault(def)
            reset.setTextColor(if (atDefault) withAlpha(pal.sub, 110) else pal.accent)
            reset.animate().scaleX(if (atDefault) 1f else 1.15f).scaleY(if (atDefault) 1f else 1.15f).setDuration(160).start()
        }

        var syncing = false
        val control: View
        val sync: () -> Unit

        when (def.kind) {
            Kind.TOGGLE -> {
                val sw = Switch(this)
                sw.isChecked = Prefs.bool(this, def.id)
                sw.setOnCheckedChangeListener { _, checked ->
                    if (!syncing) {
                        Prefs.putBool(this, def.id, checked)
                        refreshReset()
                    }
                }
                control = sw
                sync = {
                    syncing = true
                    sw.isChecked = Prefs.bool(this, def.id)
                    syncing = false
                }
            }
            Kind.NUMBER -> {
                val field = EditText(this).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                    setTextColor(pal.text)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    gravity = Gravity.CENTER
                    background = rounded(withAlpha(pal.accent, 28), 10, pal.line, 1)
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    setSingleLine(true)
                    setText(Prefs.int(this@MainActivity, def.id).toString())
                }
                field.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (syncing) return
                        val parsed = s?.toString()?.trim()?.toIntOrNull()
                        if (parsed != null) {
                            Prefs.putInt(this@MainActivity, def.id, parsed)
                            refreshReset()
                        }
                    }
                })
                field.setOnFocusChangeListener { _, focused ->
                    if (!focused) {
                        syncing = true
                        field.setText(Prefs.int(this@MainActivity, def.id).toString())
                        syncing = false
                    }
                }
                control = field
                sync = {
                    syncing = true
                    field.setText(Prefs.int(this, def.id).toString())
                    syncing = false
                }
            }
            Kind.CHOICE -> {
                val spinner = Spinner(this)
                spinner.adapter = themedAdapter(def.choices)
                spinner.setSelection(Prefs.int(this, def.id).coerceIn(0, def.choices.size - 1))
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                        if (!syncing) {
                            Prefs.putInt(this@MainActivity, def.id, position)
                            refreshReset()
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                control = spinner
                sync = {
                    syncing = true
                    spinner.setSelection(Prefs.int(this, def.id).coerceIn(0, def.choices.size - 1))
                    syncing = false
                }
            }
        }

        val controlLp = if (def.kind == Kind.NUMBER) {
            LinearLayout.LayoutParams(dp(88), ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        controlLp.setMargins(dp(8), 0, dp(4), 0)
        top.addView(control, controlLp)

        val info = text("ⓘ", 18f, pal.sub).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                TransitionManager.beginDelayedTransition(card)
                val show = infoText.visibility != View.VISIBLE
                infoText.visibility = if (show) View.VISIBLE else View.GONE
                setTextColor(if (show) pal.accent else pal.sub)
            }
        }
        top.addView(info, LinearLayout.LayoutParams(dp(34), dp(36)))

        reset.setOnClickListener {
            Prefs.reset(this, def.id)
            sync()
            refreshReset()
        }
        top.addView(reset, LinearLayout.LayoutParams(dp(34), dp(36)))

        card.addView(top)
        card.addView(infoText)
        refreshReset()
        return card
    }

    private fun themedAdapter(options: List<String>): ArrayAdapter<String> {
        val p = pal
        return object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(p.text)
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(p.text)
                v.setBackgroundColor(p.card)
                v.setPadding(dp(16), dp(12), dp(16), dp(12))
                return v
            }
        }
    }

    private fun actionButton(label: String, onClick: () -> Unit): TextView =
        text(label, 15f, pal.onAccent, true).apply {
            gravity = Gravity.CENTER
            background = rounded(pal.accent, 12)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(6), dp(4), dp(2))
            }
        }

    private fun setupCard(): View {
        val card = card()
        card.addView(text("Get started", 17f, pal.text, true))
        card.addView(text("Two quick steps, then it appears whenever you type.", 13f, pal.sub))
        card.addView(actionButton("1  Enable KeySuite") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        card.addView(actionButton("2  Switch to KeySuite") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        return card
    }

    private fun infoCard(message: String): View {
        val card = card()
        card.addView(text(message, 14f, pal.sub))
        return card
    }

    private fun testCard(): View {
        val card = card()
        val field = EditText(this).apply {
            hint = "Tap here and type to try the keyboard"
            setHintTextColor(pal.sub)
            setTextColor(pal.text)
            minLines = 3
            gravity = Gravity.TOP
            background = null
        }
        card.addView(field)
        return card
    }

    private fun lockCard(): View {
        val card = card()
        card.addView(text("Protect the clipboard panel", 16f, pal.text))
        card.addView(text("Choose how the 📋 panel is unlocked. Changes apply the next time you open it.", 13f, pal.sub))

        val modes = listOf("None", "PIN", "Tap count", "Hidden spot")
        val modeValues = listOf(ClipLock.MODE_NONE, ClipLock.MODE_PIN, ClipLock.MODE_TAP_COUNT, ClipLock.MODE_HIDDEN_SPOT)
        val spinner = Spinner(this)
        spinner.adapter = themedAdapter(modes)
        spinner.setSelection(modeValues.indexOf(ClipLock.mode(this)).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                ClipLock.setMode(this@MainActivity, modeValues[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        card.addView(spinner)

        val pin = EditText(this).apply {
            hint = "New PIN"
            setHintTextColor(pal.sub)
            setTextColor(pal.text)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            background = rounded(withAlpha(pal.accent, 28), 10, pal.line, 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        card.addView(pin)
        card.addView(actionButton("Save PIN") {
            val value = pin.text.toString()
            if (value.isNotBlank()) {
                ClipLock.setSecret(this, value)
                pin.setText("")
                pin.hint = "PIN saved"
            }
        })

        val taps = EditText(this).apply {
            setTextColor(pal.text)
            inputType = InputType.TYPE_CLASS_NUMBER
            background = rounded(withAlpha(pal.accent, 28), 10, pal.line, 1)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setText(ClipLock.tapTarget(this@MainActivity).toString())
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    s?.toString()?.trim()?.toIntOrNull()?.let { ClipLock.setTapTarget(this@MainActivity, it) }
                }
            })
        }
        card.addView(text("Taps needed for Tap count mode", 13f, pal.sub).apply { setPadding(0, dp(10), 0, dp(2)) })
        card.addView(taps)
        return card
    }
}
