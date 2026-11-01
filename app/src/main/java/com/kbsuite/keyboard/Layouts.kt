package com.kbsuite.keyboard

object Codes {
    const val SHIFT = -1
    const val SYMBOLS = -2
    const val ALPHA = -3
    const val ENTER = -4
    const val DELETE = -5
    const val SPACE = 32
    const val SYMBOLS_MORE = -6
    const val SWITCH_IME = -7
    const val SETTINGS = -8
    const val SPACER = 0
}

data class Key(
    val label: String,
    val code: Int,
    val weight: Float = 1f,
    val repeat: Boolean = false,
    val alt: String = "",
    val modifier: Boolean = false
)

object Layouts {

    private fun letters(chars: String, alts: String = ""): List<Key> =
        chars.mapIndexed { i, c ->
            Key(c.toString(), c.code, 1f, false, if (i < alts.length) alts[i].toString() else "")
        }

    fun numberRow(): List<Key> = "1234567890".map { Key(it.toString(), it.code) }

    fun alpha(numberRow: Boolean): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        if (numberRow) rows.add(numberRow())
        rows.add(letters("qwertyuiop", "1234567890"))
        rows.add(
            listOf(Key("", Codes.SPACER, 0.5f)) +
                letters("asdfghjkl") +
                listOf(Key("", Codes.SPACER, 0.5f))
        )
        rows.add(
            listOf(Key("⇧", Codes.SHIFT, 1.5f, modifier = true)) +
                letters("zxcvbnm") +
                listOf(Key("⌫", Codes.DELETE, 1.5f, repeat = true))
        )
        rows.add(
            listOf(
                Key("?123", Codes.SYMBOLS, 1.5f),
                Key(",", ','.code),
                Key("", Codes.SPACE, 5f),
                Key(".", '.'.code, alt = ",;:!?"),
                Key("↵", Codes.ENTER, 1.5f)
            )
        )
        return rows
    }

    fun symbols(numberRow: Boolean): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        if (numberRow) rows.add(numberRow())
        rows.add(letters("@#$%&-+()/"))
        rows.add(listOf(Key("=\\<", Codes.SYMBOLS_MORE, 1.5f)) + letters("*\"':;!?") + listOf(Key("⌫", Codes.DELETE, 1.5f, repeat = true)))
        rows.add(
            listOf(
                Key("ABC", Codes.ALPHA, 1.5f),
                Key(",", ','.code),
                Key("", Codes.SPACE, 5f),
                Key(".", '.'.code),
                Key("↵", Codes.ENTER, 1.5f)
            )
        )
        return rows
    }

    fun symbolsMore(numberRow: Boolean): List<List<Key>> {
        val rows = ArrayList<List<Key>>()
        if (numberRow) rows.add(numberRow())
        rows.add(letters("~`|•√π÷×¶∆"))
        rows.add(listOf(Key("?123", Codes.SYMBOLS, 1.5f)) + letters("£¢€¥^°={}") + listOf(Key("⌫", Codes.DELETE, 1.5f, repeat = true)))
        rows.add(
            listOf(
                Key("ABC", Codes.ALPHA, 1.5f),
                Key("\\", '\\'.code),
                Key("", Codes.SPACE, 5f),
                Key("[", '['.code),
                Key("]", ']'.code)
            )
        )
        return rows
    }
}
