package com.kbsuite.keyboard

enum class Kind { TOGGLE, NUMBER, CHOICE }

data class Def(
    val id: String,
    val tab: String,
    val section: String,
    val title: String,
    val info: String,
    val kind: Kind,
    val choices: List<String> = emptyList(),
    val keywords: String = ""
)

object Registry {

    const val TAB_KEYS = "Keys"
    const val TAB_TYPING = "Typing"
    const val TAB_CLIP = "Clipboard"
    const val TAB_LOOK = "Look"
    const val TAB_FEEDBACK = "Feedback"
    const val TAB_MODES = "Modes"

    val tabs = listOf(TAB_KEYS, TAB_TYPING, TAB_CLIP, TAB_LOOK, TAB_FEEDBACK, TAB_MODES)

    val themeChoices = listOf("Light", "Dark", "Pitch black")

    val defs: List<Def> = listOf(
        Def(Prefs.NUMBER_ROW, TAB_KEYS, "Layout", "Number row", "Shows a dedicated row of digits above the letters. When off, hold a top-row letter to type its digit.", Kind.TOGGLE, keywords = "digits numbers 1234567890"),
        Def(Prefs.KEY_HEIGHT_DP, TAB_KEYS, "Layout", "Key height (dp)", "Height of every key row. Type any number; the keyboard reflows next time it opens.", Kind.NUMBER, keywords = "size resize taller shorter"),
        Def(Prefs.KEY_GAP_DP, TAB_KEYS, "Layout", "Key gap (dp)", "Space between keys. Zero makes keys touch; larger values give a floating look.", Kind.NUMBER, keywords = "spacing padding margin"),
        Def(Prefs.KEY_RADIUS_DP, TAB_KEYS, "Layout", "Key corner radius (dp)", "Roundness of key corners. Zero is square, large values make pills.", Kind.NUMBER, keywords = "round corners shape"),
        Def(Prefs.SHOW_TOOLBAR, TAB_KEYS, "Layout", "Show toolbar strip", "The strip above the keys with clipboard, repeat, undo, redo, settings and keyboard switcher.", Kind.TOGGLE, keywords = "top bar buttons"),
        Def(Prefs.KEY_POPUP, TAB_KEYS, "Touch", "Key popup preview", "A bubble above the pressed key showing the character. While held on a key with an alternate, it shows the alternate.", Kind.TOGGLE, keywords = "preview bubble magnify"),
        Def(Prefs.LONG_PRESS_MS, TAB_KEYS, "Touch", "Long press delay (ms)", "How long to hold a key before its alternate character is chosen.", Kind.NUMBER, keywords = "hold alternate symbols"),
        Def(Prefs.REPEAT_DELAY_MS, TAB_KEYS, "Touch", "Backspace repeat delay (ms)", "How long to hold backspace before it starts repeating.", Kind.NUMBER, keywords = "delete hold"),
        Def(Prefs.REPEAT_RATE_MS, TAB_KEYS, "Touch", "Backspace repeat rate (ms)", "Time between repeated deletes while backspace is held. Smaller is faster.", Kind.NUMBER, keywords = "delete speed"),

        Def(Prefs.AUTO_CAPS, TAB_TYPING, "Capitalisation", "Auto-capitalise sentences", "Shift turns on automatically at the start of a sentence, following the app's own caps rules.", Kind.TOGGLE, keywords = "shift capital uppercase"),
        Def(Prefs.DOUBLE_TAP_MS, TAB_TYPING, "Capitalisation", "Double tap window (ms)", "Two taps on shift or space inside this window count as a double tap: caps lock, or full stop.", Kind.NUMBER, keywords = "caps lock timing"),
        Def(Prefs.DOUBLE_SPACE_PERIOD, TAB_TYPING, "Punctuation", "Double space types full stop", "Tapping space twice after a letter or digit replaces the pair with a full stop and a space.", Kind.TOGGLE, keywords = "period dot"),
        Def(Prefs.ENTER_SENDS, TAB_TYPING, "Enter key", "Enter key sends", "On: Enter performs the field's action such as send or search. Off: Enter always inserts a line break. Holding Enter always inserts a line break.", Kind.TOGGLE, keywords = "return newline line feed submit"),

        Def(Prefs.CLIP_MAX, TAB_CLIP, "History", "Maximum unpinned clips (0 = no limit)", "No built-in limit. Set a number only if you want old unpinned clips trimmed. Pinned clips are never trimmed.", Kind.NUMBER, keywords = "limit history size count"),
        Def(Prefs.CLIP_SHOW_TITLE, TAB_CLIP, "History", "Show clip titles", "When a clip has a title, show it instead of the text preview.", Kind.TOGGLE, keywords = "rename label name"),
        Def(Prefs.CLIP_CAPTURE_IMAGES, TAB_CLIP, "Images", "Capture copied images (not active yet)", "Reserved for image clips. The setting is saved now; image capture is a later upgrade.", Kind.TOGGLE, keywords = "picture photo"),
        Def(Prefs.CLIP_SHOW_IMAGES, TAB_CLIP, "Images", "Show images in clipboard (not active yet)", "Reserved: whether captured images appear in the clipboard list.", Kind.TOGGLE, keywords = "picture photo"),

        Def(Prefs.THEME, TAB_LOOK, "Keyboard", "Keyboard theme", "Colour set used by the keyboard itself. Pitch black uses a true black background, which saves power on OLED screens.", Kind.CHOICE, themeChoices, keywords = "dark light amoled colours"),

        Def(Prefs.HAPTIC, TAB_FEEDBACK, "Touch feedback", "Vibrate on keypress", "Short haptic tick on every key press.", Kind.TOGGLE, keywords = "vibration haptic"),
        Def(Prefs.SOUND, TAB_FEEDBACK, "Touch feedback", "Sound on keypress", "Plays the system key click sound. Follows your phone's touch-sound setting.", Kind.TOGGLE, keywords = "click audio")
    )

    fun forTab(tab: String): List<Def> = defs.filter { it.tab == tab }

    fun search(query: String): List<Def> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return defs.filter {
            "${it.title} ${it.info} ${it.keywords} ${it.section} ${it.tab}".lowercase().contains(q)
        }
    }
}
