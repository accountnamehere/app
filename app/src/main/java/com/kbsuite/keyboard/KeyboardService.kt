package com.kbsuite.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import com.kbsuite.keyboard.clip.ClipItem
import com.kbsuite.keyboard.clip.ClipLock
import com.kbsuite.keyboard.clip.ClipPanel
import com.kbsuite.keyboard.clip.ClipStore
import com.kbsuite.keyboard.clip.LockPrompt
import com.kbsuite.keyboard.repeat.RepeatPanel

class KeyboardService : InputMethodService(), KeyboardView.Listener {

    private var view: KeyboardView? = null
    private var root: LinearLayout? = null
    private var body: FrameLayout? = null
    private var clipPanel: ClipPanel? = null
    private var repeatPanel: RepeatPanel? = null
    private var toolbarView: LinearLayout? = null
    private var page = PAGE_ALPHA
    private var shift = SHIFT_OFF
    private var lastShiftTap = 0L
    private var lastSpaceTap = 0L
    private var lastInserted = ""
    private var lastDeleted = ""
    private val clipStore by lazy { ClipStore(this) }
    private var ignoreNextClipChange = false

    private val systemClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (ignoreNextClipChange) {
            ignoreNextClipChange = false
            return@OnPrimaryClipChangedListener
        }
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return@OnPrimaryClipChangedListener
        if (clip.itemCount == 0) return@OnPrimaryClipChangedListener
        val text = clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank()) {
            clipStore.add(text, Prefs.int(this, Prefs.CLIP_MAX))
            clipPanel?.refresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(systemClipListener)
    }

    override fun onDestroy() {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(systemClipListener)
        super.onDestroy()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toolbarButton(label: String, action: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            background = null
            setPadding(dp(10), dp(6), dp(10), dp(6))
            contentDescription = label
            setImageDrawable(null)
            setOnClickListener { action() }
        }
    }

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        view = v

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0C0E10"))
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        toolbar.addView(android.widget.TextView(this).apply {
            text = "📋"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { toggleClipPanel() }
        })
        toolbar.addView(android.widget.TextView(this).apply {
            text = "🔁"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { toggleRepeatPanel() }
        })
        toolbar.addView(android.widget.TextView(this).apply {
            text = "↶"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { undo() }
        })
        toolbar.addView(android.widget.TextView(this).apply {
            text = "↷"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { redo() }
        })
        toolbar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        toolbar.addView(android.widget.TextView(this).apply {
            text = "⚙"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                val intent = android.content.Intent(this@KeyboardService, MainActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        })
        toolbar.addView(android.widget.TextView(this).apply {
            text = "⌨"
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })

        toolbarView = toolbar
        val bodyFrame = FrameLayout(this)
        bodyFrame.addView(v)
        body = bodyFrame

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        column.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(32)))
        column.addView(bodyFrame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root = column

        applyPrefs()
        applyPage()
        return column
    }

    private fun closeAllPanels() {
        val bodyFrame = body ?: return
        clipPanel?.let { bodyFrame.removeView(it) }
        clipPanel = null
        repeatPanel?.let { it.onDetach(); bodyFrame.removeView(it) }
        repeatPanel = null
    }

    private fun toggleClipPanel() {
        val bodyFrame = body ?: return
        if (clipPanel != null) {
            closeAllPanels()
            return
        }
        closeAllPanels()
        if (ClipLock.mode(this) != ClipLock.MODE_NONE) {
            val lock = LockPrompt(
                this,
                view?.height ?: dp(220),
                onUnlocked = { openClipPanel() },
                onCancel = { closeAllPanels() }
            )
            bodyFrame.addView(lock)
            return
        }
        openClipPanel()
    }

    private fun openClipPanel() {
        val bodyFrame = body ?: return
        closeAllPanels()
        val panel = ClipPanel(
            this,
            clipStore,
            view?.height ?: dp(220),
            onPick = { item -> insertClip(item) },
            onClose = { toggleClipPanel() }
        )
        clipPanel = panel
        bodyFrame.addView(panel)
    }

    private fun toggleRepeatPanel() {
        val bodyFrame = body ?: return
        if (repeatPanel != null) {
            closeAllPanels()
            return
        }
        closeAllPanels()
        val panel = RepeatPanel(
            this,
            view?.height ?: dp(220),
            connectionProvider = { currentInputConnection },
            onClose = { toggleRepeatPanel() }
        )
        repeatPanel = panel
        bodyFrame.addView(panel)
    }

    private fun insertClip(item: ClipItem) {
        clipStore.setPasteBuffer(item)
        currentInputConnection?.commitText(item.text, 1)
    }

    private fun undo() {
        val ic = currentInputConnection ?: return
        if (lastInserted.isNotEmpty()) {
            ic.deleteSurroundingText(lastInserted.length, 0)
            lastDeleted = lastInserted
            lastInserted = ""
        }
    }

    private fun redo() {
        val ic = currentInputConnection ?: return
        if (lastDeleted.isNotEmpty()) {
            ic.commitText(lastDeleted, 1)
            lastInserted = lastDeleted
            lastDeleted = ""
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyPrefs()
        applyPage()
        if (!restarting) {
            shift = SHIFT_OFF
            autoCapitalize()
        }
    }

    private fun applyPrefs() {
        val v = view ?: return
        val theme = Prefs.int(this, Prefs.THEME).coerceIn(0, KeyboardView.palettes.size - 1)
        v.palette = KeyboardView.palettes[theme]
        v.rowHeightPx = (Prefs.int(this, Prefs.KEY_HEIGHT_DP).coerceAtLeast(20) * resources.displayMetrics.density).toInt()
        v.haptic = Prefs.bool(this, Prefs.HAPTIC)
        v.repeatDelayMs = Prefs.int(this, Prefs.REPEAT_DELAY_MS).toLong()
        v.repeatRateMs = Prefs.int(this, Prefs.REPEAT_RATE_MS).toLong().coerceAtLeast(5L)
        v.longPressMs = Prefs.int(this, Prefs.LONG_PRESS_MS).toLong().coerceAtLeast(100L)
        v.popupEnabled = Prefs.bool(this, Prefs.KEY_POPUP)
        val density = resources.displayMetrics.density
        v.gap = Prefs.int(this, Prefs.KEY_GAP_DP).coerceAtLeast(0) * density
        v.radius = Prefs.int(this, Prefs.KEY_RADIUS_DP).coerceAtLeast(0) * density
        toolbarView?.visibility = if (Prefs.bool(this, Prefs.SHOW_TOOLBAR)) View.VISIBLE else View.GONE
    }

    private fun applyPage() {
        val v = view ?: return
        val numbers = Prefs.bool(this, Prefs.NUMBER_ROW)
        v.rows = when (page) {
            PAGE_SYMBOLS -> Layouts.symbols(numbers)
            PAGE_MORE -> Layouts.symbolsMore(numbers)
            else -> Layouts.alpha(numbers)
        }
        v.shiftState = shift
    }

    private fun setShift(state: Int) {
        shift = state
        view?.shiftState = state
    }

    private fun autoCapitalize() {
        if (!Prefs.bool(this, Prefs.AUTO_CAPS)) return
        val info = currentInputEditorInfo ?: return
        if ((info.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_NULL) return
        if (shift == SHIFT_LOCK) return
        val caps = currentInputConnection?.getCursorCapsMode(info.inputType) ?: 0
        setShift(if (caps != 0) SHIFT_ONCE else SHIFT_OFF)
    }

    private fun click() {
        if (!Prefs.bool(this, Prefs.SOUND)) return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
    }

    private fun sendKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun doEnter() {
        val ic = currentInputConnection ?: return
        if (!Prefs.bool(this, Prefs.ENTER_SENDS)) {
            ic.commitText("\n", 1)
            return
        }
        val options = currentInputEditorInfo?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendKey(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun doDelete() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected != null && selected.isNotEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        lastInserted = ""
        lastDeleted = ""
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        lastInserted += text
        lastDeleted = ""
        if (shift == SHIFT_ONCE) setShift(SHIFT_OFF)
        if (text.length == 1 && !text[0].isLetter()) autoCapitalize()
    }

    override fun onKeyDown(key: Key) {
        click()
    }

    override fun onKeyRepeat(key: Key) {
        if (key.code == Codes.DELETE) doDelete()
    }

    override fun onKeyLongPress(key: Key): Boolean {
        if (key.code == Codes.ENTER) {
            currentInputConnection?.commitText("\n", 1)
            return true
        }
        if (key.alt.isEmpty()) return false
        commit(key.alt.take(1))
        return true
    }

    override fun onKeyUp(key: Key) {
        when (key.code) {
            Codes.SHIFT -> {
                val now = SystemClock.uptimeMillis()
                val window = Prefs.int(this, Prefs.DOUBLE_TAP_MS).toLong()
                val next = when {
                    shift == SHIFT_LOCK -> SHIFT_OFF
                    shift == SHIFT_ONCE && now - lastShiftTap <= window -> SHIFT_LOCK
                    shift == SHIFT_ONCE -> SHIFT_OFF
                    else -> SHIFT_ONCE
                }
                lastShiftTap = now
                setShift(next)
            }
            Codes.DELETE -> doDelete()
            Codes.ENTER -> doEnter()
            Codes.SYMBOLS -> {
                page = PAGE_SYMBOLS
                applyPage()
            }
            Codes.SYMBOLS_MORE -> {
                page = PAGE_MORE
                applyPage()
            }
            Codes.ALPHA -> {
                page = PAGE_ALPHA
                applyPage()
            }
            Codes.SWITCH_IME -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            Codes.SPACE -> handleSpace()
            else -> {
                val label = key.label
                val text = if (shift != SHIFT_OFF && label.length == 1 && label[0].isLetter()) label.uppercase() else label
                commit(text)
            }
        }
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        val window = Prefs.int(this, Prefs.DOUBLE_TAP_MS).toLong()
        if (Prefs.bool(this, Prefs.DOUBLE_SPACE_PERIOD) && now - lastSpaceTap <= window) {
            val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                lastSpaceTap = 0L
                autoCapitalize()
                return
            }
        }
        lastSpaceTap = now
        commit(" ")
    }

    companion object {
        private const val PAGE_ALPHA = 0
        private const val PAGE_SYMBOLS = 1
        private const val PAGE_MORE = 2
        private const val SHIFT_OFF = 0
        private const val SHIFT_ONCE = 1
        private const val SHIFT_LOCK = 2
    }
}
