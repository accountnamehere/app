# Plan

Legend: ✅ done · 🟡 partial · ⏭️ next · ⬜ pending · ⛔ blocked

## Setup
- ✅ Unpack sources, toolchain probe
- ✅ GitHub Actions cloud build (Gradle + Android SDK on GitHub's runners)
- ✅ Claude GitHub App installed on accountnamehere/app, pushes now work

## Docs and tools
- ✅ rules.md, plan.md, nuance.md
- ✅ tools/check_nocomments.py, run every build via CI step

## Core keyboard
- ✅ QWERTY + number row toggle, two symbol pages
- ✅ Shift: tap / double-tap lock, auto-capitalise after sentence end
- ✅ Double-space-to-period, long-press alt char, long-press number on letter when number row is off
- ✅ Backspace repeat (down/up based, per-pointer)
- ✅ Enter: editor action by default, optional "enter sends" off = line feed, long-press enter always line feed
- ✅ Themes: light / dark / pitch black palette objects
- 🟡 Full stop long-press shows only one alt char, not a strip of several — needs a real popup strip
- ⬜ Key preview bubble only (no multi-char popup UI), glide typing, swipe actions
- ⬜ Spell check / grammar check
- ⬜ Image keyboard background, spacebar text/transparency customization
- ⬜ Resize / floating keyboard
- ⬜ Sound effects wiring beyond the toggle stub
- ⬜ Full PC-style layer (Ctrl/Alt/F1-F12/combos)

## Clipboard
- ✅ History store (JSON file), pins, rename groundwork, separate paste buffer, system-clipboard capture
- ✅ Search panel, unlimited-by-default max count
- ✅ Lock: PIN implemented and gates the panel; pattern/hidden-spot/tap-count scaffolded, pattern UI not built
- ⬜ Image copy support, "copied images show in clipboard" toggle
- ⬜ Address/phone auto-detect
- ⬜ Leak-path hardening (recents thumbnail, notif shade) — not yet tested on device

## Repeat-send (merged spamit/spamem)
- ✅ One panel, user-entered message/count/delay, visible Start/Stop, line-feed vs send-key modes
- ⬜ Scheduler mode (multiple saved clips, folders, drag sort, per-item delay)
- ⬜ Saved-clip library separate from static clipboard

## Settings app
- ✅ Flat native screen with sections, reset buttons, textboxes, theme dropdown
- ⬜ Real tabs/sub-tabs, universal search with highlight, info icons, lazy loading
- ⬜ Smooth theme-switch animation, per-item reset-icon-lights-up-when-clicked state

## Not started
- ⬜ AI assist mode (URL config, OpenAI-compatible, tool calling, vision)
- ⬜ Learning/typing-pattern engine
- ⬜ Backup/export (single + export-all)
- ⬜ Emoji + 17k unicode character table
- ⬜ Languages beyond English
- ⬜ Playwright snapshot script for the settings UI (blocked: settings app is native Android, not web; would need screenshot via device/emulator, which this container does not have — Chromium snapshot only applies if we ever add a WebView-based panel)

## Known risk
- No device/emulator testing has happened. Every file here compiled on GitHub's Gradle but has not been run on a phone. Expect rough edges on first real use — please report back concretely so fixes are targeted, not guessed.
