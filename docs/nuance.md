# Nuance

## Shift and caps
- Single tap: next letter only. Double tap inside the window: caps lock. Tap in lock: off.
- Auto-capitalise after sentence end follows the editor's own caps mode, never past lock.
- Shift returns to off after one letter, but not after a symbol-page switch.

## Clipboard versus paste buffer
- Paste buffer is separate from clipboard history. Removing a clip does not clear the last copied value.
- Pasting a clip does not reorder history. Item 1 stays item 1, item 2 stays item 2.
- A new copy replaces the paste buffer only.

## Enter
- Enter performs the editor action when one exists, else a real Enter key event.
- Long press Enter inserts a line feed that no field treats as send.

## Punctuation
- Long press full stop opens its symbol strip.
- Double space gives full stop plus space only after a letter or digit.
- Optional auto space after punctuation, optional return to letters after apostrophe.

## Input
- Key events fire on down and release per pointer, so several fingers and held keys work together.
- Backspace repeats with its own delay and rate; both are user settings.
- Selected text is replaced by delete, not left in place.

## Clipboard lock
- Lock gate choices: PIN, password, pattern, hidden tap spot, tap count.
- Hidden gate means the clip button is absent until the gate is performed.
- Leak paths to test: recents thumbnail, notification shade, share sheet, accessibility readers, screenshots.

## Failure fallbacks
- No input connection: ignore keys, never crash.
- Missing editor info: treat as plain text field.
- Preference parse failure: fall back to default and keep the field visible.
