package com.kbsuite.keyboard.clip

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ClipPanel(
    context: Context,
    private val store: ClipStore,
    private val heightPx: Int,
    private val onPick: (ClipItem) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(context) {

    private val list = LinearLayout(context).apply { orientation = VERTICAL }
    private var query: String = ""

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#14181C"))
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val search = EditText(context).apply {
            hint = "Search clips"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8A8F98"))
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                query = s?.toString().orEmpty()
                refresh()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        header.addView(search, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(
            Button(context).apply {
                text = "Close"
                setOnClickListener { onClose() }
            }
        )
        addView(header)

        val scroll = ScrollView(context)
        scroll.addView(list, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(scroll, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        refresh()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    fun refresh() {
        list.removeAllViews()
        val items = store.all().filter {
            query.isBlank() || it.text.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
        }
        if (items.isEmpty()) {
            list.addView(
                TextView(context).apply {
                    text = "No clips yet"
                    setTextColor(Color.parseColor("#8A8F98"))
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                }
            )
            return
        }
        for (item in items) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setOnClickListener { onPick(item) }
            }
            val label = if (item.title.isNotBlank()) item.title else item.text
            row.addView(
                TextView(context).apply {
                    text = label.take(80)
                    setTextColor(Color.WHITE)
                    maxLines = 2
                },
                LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(
                Button(context).apply {
                    text = if (item.pinned) "★" else "☆"
                    setOnClickListener {
                        store.togglePin(item.id)
                        refresh()
                    }
                }
            )
            row.addView(
                Button(context).apply {
                    text = "✕"
                    setOnClickListener {
                        store.remove(item.id)
                        refresh()
                    }
                }
            )
            list.addView(row)
        }
    }
}
