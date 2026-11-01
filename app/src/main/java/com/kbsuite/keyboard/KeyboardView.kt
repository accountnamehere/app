package com.kbsuite.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class KeyboardView(context: Context) : View(context) {

    interface Listener {
        fun onKeyDown(key: Key)
        fun onKeyUp(key: Key)
        fun onKeyRepeat(key: Key)
        fun onKeyLongPress(key: Key, index: Int): Boolean
    }

    class Palette(
        val background: Int,
        val key: Int,
        val keyPressed: Int,
        val special: Int,
        val text: Int,
        val hint: Int,
        val accent: Int
    )

    var listener: Listener? = null

    var rows: List<List<Key>> = emptyList()
        set(value) {
            field = value
            rebuild()
            requestLayout()
            invalidate()
        }

    var shiftState: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var palette: Palette = palettes[1]
        set(value) {
            field = value
            invalidate()
        }

    var rowHeightPx: Int = (52 * resources.displayMetrics.density).toInt()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var haptic: Boolean = true
    var repeatDelayMs: Long = 350L
    var repeatRateMs: Long = 45L
    var longPressMs: Long = 350L
    var popupEnabled: Boolean = true

    private val handler = Handler(Looper.getMainLooper())
    private val laid = ArrayList<Pair<Key, RectF>>()
    private val active = HashMap<Int, Key>()
    private val repeaters = HashMap<Int, Runnable>()
    private val longPressed = HashSet<Int>()
    private val longPressRunnables = HashMap<Int, Runnable>()
    private var previewKey: Key? = null
    private var previewRect: RectF? = null
    private var stripPointer = -1
    private var stripIndex = 0
    private var stripLeft = 0f
    private var stripCell = 0f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }
    var gap: Float = 3f * resources.displayMetrics.density
        set(value) {
            field = value
            rebuild()
            invalidate()
        }

    var radius: Float = 7f * resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    private fun rebuild() {
        laid.clear()
        if (width == 0 || rows.isEmpty()) return
        val rowH = height.toFloat() / rows.size
        rows.forEachIndexed { r, row ->
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            var x = 0f
            row.forEach { key ->
                val w = width * (key.weight / total)
                if (key.code != Codes.SPACER) {
                    laid.add(
                        key to RectF(x + gap, r * rowH + gap, x + w - gap, (r + 1) * rowH - gap)
                    )
                }
                x += w
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, rowHeightPx * maxOf(rows.size, 1))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    private fun labelFor(key: Key): String {
        if (key.label.length == 1 && key.label[0].isLetter()) {
            return if (shiftState != 0) key.label.uppercase() else key.label
        }
        if (key.code == Codes.SHIFT) {
            return when (shiftState) {
                2 -> "⇪"
                1 -> "⇧"
                else -> "⇧"
            }
        }
        return key.label
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        val pressedKeys = active.values
        for ((key, rect) in laid) {
            val pressed = pressedKeys.contains(key)
            fill.color = when {
                pressed -> palette.keyPressed
                key.code == Codes.SHIFT && shiftState != 0 -> palette.accent
                key.modifier || key.code < 0 && key.code != Codes.SPACE -> palette.special
                else -> palette.key
            }
            canvas.drawRoundRect(rect, radius, radius, fill)
            val label = labelFor(key)
            if (label.isNotEmpty()) {
                textPaint.color = if (key.code == Codes.SHIFT && shiftState != 0) palette.background else palette.text
                textPaint.textSize = if (label.length > 1) rect.height() * 0.30f else rect.height() * 0.42f
                val y = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(label, rect.centerX(), y, textPaint)
            }
            if (key.alt.isNotEmpty()) {
                hintPaint.color = palette.hint
                hintPaint.textSize = rect.height() * 0.24f
                canvas.drawText(key.alt.take(1), rect.right - gap * 2f, rect.top + rect.height() * 0.30f, hintPaint)
            }
        }
        val pk = previewKey
        val pr = previewRect
        if (pk != null && pr != null) {
            if (stripPointer >= 0 && pk.alt.length > 1) {
                val n = pk.alt.length
                val strip = RectF(stripLeft, maxOf(pr.top - pr.height() * 1.4f, 0f), stripLeft + stripCell * n, maxOf(pr.top - pr.height() * 1.4f, 0f) + pr.height() * 1.2f)
                fill.color = palette.special
                canvas.drawRoundRect(strip, radius, radius, fill)
                for (i in 0 until n) {
                    val cell = RectF(strip.left + i * stripCell + gap, strip.top + gap, strip.left + (i + 1) * stripCell - gap, strip.bottom - gap)
                    if (i == stripIndex) {
                        fill.color = palette.accent
                        canvas.drawRoundRect(cell, radius, radius, fill)
                    }
                    textPaint.color = if (i == stripIndex) palette.background else palette.text
                    textPaint.textSize = cell.height() * 0.5f
                    val ty = cell.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(pk.alt[i].toString(), cell.centerX(), ty, textPaint)
                }
            } else {
                val bubbleW = pr.width() * 1.3f
                val bubbleH = pr.height() * 1.6f
                val cx = pr.centerX()
                val bubble = RectF(cx - bubbleW / 2f, pr.top - bubbleH, cx + bubbleW / 2f, pr.top)
                fill.color = palette.keyPressed
                canvas.drawRoundRect(bubble, radius, radius, fill)
                val text = if (longPressed.isNotEmpty() && pk.alt.isNotEmpty()) pk.alt.take(1) else labelFor(pk)
                textPaint.color = palette.text
                textPaint.textSize = bubble.height() * 0.5f
                val y = bubble.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(text, bubble.centerX(), y, textPaint)
            }
        }
    }

    private fun keyAt(x: Float, y: Float): Key? {
        var best: Key? = null
        var bestDist = Float.MAX_VALUE
        for ((key, rect) in laid) {
            if (rect.contains(x, y)) return key
            val dx = maxOf(rect.left - x, 0f, x - rect.right)
            val dy = maxOf(rect.top - y, 0f, y - rect.bottom)
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = key
            }
        }
        return if (bestDist < (gap * 4f) * (gap * 4f)) best else null
    }

    private fun press(pointerId: Int, x: Float, y: Float) {
        val key = keyAt(x, y) ?: return
        active[pointerId] = key
        if (haptic) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        listener?.onKeyDown(key)
        if (popupEnabled && key.code != Codes.SPACE && key.code != Codes.SPACER) {
            previewKey = key
            previewRect = laid.firstOrNull { it.first == key }?.second
        }
        if (key.repeat) {
            val runnable = object : Runnable {
                override fun run() {
                    listener?.onKeyRepeat(key)
                    handler.postDelayed(this, repeatRateMs)
                }
            }
            repeaters[pointerId] = runnable
            handler.postDelayed(runnable, repeatDelayMs)
        }
        if (key.alt.isNotEmpty() || key.code == Codes.ENTER) {
            val runnable = Runnable {
                longPressed.add(pointerId)
                if (key.alt.length > 1) {
                    val rect = laid.firstOrNull { it.first == key }?.second
                    if (rect != null) {
                        stripPointer = pointerId
                        stripIndex = 0
                        stripCell = rect.height() * 0.95f
                        val total = stripCell * key.alt.length
                        stripLeft = (rect.centerX() - total / 2f).coerceIn(0f, maxOf(width - total, 0f))
                    }
                }
                invalidate()
            }
            longPressRunnables[pointerId] = runnable
            handler.postDelayed(runnable, longPressMs)
        }
        invalidate()
    }

    private fun release(pointerId: Int, cancelled: Boolean) {
        repeaters.remove(pointerId)?.let { handler.removeCallbacks(it) }
        longPressRunnables.remove(pointerId)?.let { handler.removeCallbacks(it) }
        val wasLong = longPressed.remove(pointerId)
        val chosen = if (stripPointer == pointerId) stripIndex else 0
        if (stripPointer == pointerId) stripPointer = -1
        val key = active.remove(pointerId)
        if (active.isEmpty()) {
            previewKey = null
            previewRect = null
        }
        if (key != null && !cancelled) {
            if (wasLong) {
                if (listener?.onKeyLongPress(key, chosen) != true) listener?.onKeyUp(key)
            } else {
                listener?.onKeyUp(key)
            }
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                press(event.getPointerId(i), event.getX(i), event.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                release(event.getPointerId(event.actionIndex), false)
            }
            MotionEvent.ACTION_MOVE -> {
                if (stripPointer >= 0) {
                    val index = event.findPointerIndex(stripPointer)
                    val key = active[stripPointer]
                    if (index >= 0 && key != null && key.alt.length > 1 && stripCell > 0f) {
                        val raw = ((event.getX(index) - stripLeft) / stripCell).toInt()
                        val next = raw.coerceIn(0, key.alt.length - 1)
                        if (next != stripIndex) {
                            stripIndex = next
                            invalidate()
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                stripPointer = -1
                active.keys.toList().forEach { release(it, true) }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        repeaters.values.forEach { handler.removeCallbacks(it) }
        repeaters.clear()
        longPressRunnables.values.forEach { handler.removeCallbacks(it) }
        longPressRunnables.clear()
        longPressed.clear()
        active.clear()
        previewKey = null
        previewRect = null
    }

    companion object {
        val palettes: List<Palette> = listOf(
            Palette(0xFFE8EAED.toInt(), 0xFFFFFFFF.toInt(), 0xFFC4C7CC.toInt(), 0xFFD3D6DB.toInt(), 0xFF1F2328.toInt(), 0xFF6B7280.toInt(), 0xFF1A73E8.toInt()),
            Palette(0xFF1B1D21.toInt(), 0xFF2E3138.toInt(), 0xFF454A54.toInt(), 0xFF24272D.toInt(), 0xFFECEFF4.toInt(), 0xFF9AA3AF.toInt(), 0xFF5AA9FF.toInt()),
            Palette(0xFF000000.toInt(), 0xFF111114.toInt(), 0xFF26262C.toInt(), 0xFF0A0A0C.toInt(), 0xFFF5F5F5.toInt(), 0xFF8A8F98.toInt(), 0xFF22D3EE.toInt())
        )
    }
}
