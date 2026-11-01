package com.kbsuite.keyboard.clip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ClipStore(private val context: Context) {

    private val file = File(context.filesDir, "clips.json")
    private var cache: MutableList<ClipItem>? = null
    private var pasteBuffer: ClipItem? = null
    private var nextId = 1L

    private fun load(): MutableList<ClipItem> {
        cache?.let { return it }
        val items = ArrayList<ClipItem>()
        if (file.exists()) {
            val text = file.readText()
            if (text.isNotBlank()) {
                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    items.add(
                        ClipItem(
                            id = o.getLong("id"),
                            text = o.getString("text"),
                            title = o.optString("title", ""),
                            pinned = o.optBoolean("pinned", false),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isImage = o.optBoolean("isImage", false),
                            imagePath = o.optString("imagePath", "")
                        )
                    )
                }
            }
        }
        nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
        cache = items
        return items
    }

    private fun persist() {
        val array = JSONArray()
        for (item in load()) {
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("title", item.title)
                    put("pinned", item.pinned)
                    put("createdAt", item.createdAt)
                    put("isImage", item.isImage)
                    put("imagePath", item.imagePath)
                }
            )
        }
        file.writeText(array.toString())
    }

    fun all(): List<ClipItem> = load().sortedWith(
        compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.createdAt }
    )

    fun add(text: String, maxItems: Int): ClipItem {
        val items = load()
        val item = ClipItem(nextId++, text, "", false, System.currentTimeMillis(), false, "")
        items.add(0, item)
        if (maxItems > 0) {
            val unpinnedOverflow = items.filter { !it.pinned }.drop(maxItems)
            if (unpinnedOverflow.isNotEmpty()) items.removeAll(unpinnedOverflow)
        }
        pasteBuffer = item
        persist()
        return item
    }

    fun setPasteBuffer(item: ClipItem) {
        pasteBuffer = item
    }

    fun pasteBuffer(): ClipItem? = pasteBuffer

    fun remove(id: Long) {
        load().removeAll { it.id == id }
        persist()
    }

    fun togglePin(id: Long) {
        val items = load()
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val item = items[index]
            items[index] = item.copy(pinned = !item.pinned)
            persist()
        }
    }

    fun rename(id: Long, title: String) {
        val items = load()
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(title = title)
            persist()
        }
    }

    fun clearUnpinned() {
        load().removeAll { !it.pinned }
        persist()
    }

    fun clearAll() {
        load().clear()
        pasteBuffer = null
        persist()
    }
}
