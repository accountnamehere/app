package com.kbsuite.keyboard

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class KeyboardService : InputMethodService(), KeyboardView.Listener {

    private var view: KeyboardView? = null
    private var page = PAGE_ALPHA
    private var shift = SHIFT_OFF
    private var lastShiftTap = 0L
    private var lastSpaceTap = 0L

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        view = v
        applyPrefs()
        applyPage()
        return v
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
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        if (shift == SHIFT_ONCE) setShift(SHIFT_OFF)
        if (text.length == 1 && !text[0].isLetter()) autoCapitalize()
    }

    override fun onKeyDown(key: Key) {
        click()
    }

    override fun onKeyRepeat(key: Key) {
        if (key.code == Codes.DELETE) doDelete()
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
