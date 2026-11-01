# Plan

Legend: ✅ done · 🟡 partial · ⏭️ next · ⬜ pending · ⛔ blocked

## Setup
- ✅ Unpack sources (spam-em, spamit)
- ✅ Toolchain probe: JDK 21 + Gradle 8.14.3 present; dl.google.com / maven blocked
- ⛔ Running downloaded aapt2 / d8.jar was denied by permission check; needs user decision
- ⏭️ Build route: GitHub Actions workflow the user runs, or user approves local tools

## Docs and tools
- ✅ rules.md
- ⏭️ nuance.md
- ⏭️ tools/check_nocomments.py
- ⬜ tools/snapshot.js (Playwright)

## App
- ⬜ Gradle project skeleton, manifest, IME service
- ⬜ Keyboard views: qwerty, symbols, full PC layout, F1-F12
- ⬜ Clipboard: history, pins, lock, separate paste buffer
- ⬜ Scheduler with folders
- ⬜ Spamit / spamem merged modes
- ⬜ AI assist mode
- ⬜ Log, undo/redo, resize, backup/export
- ⬜ Settings app: tabs, search, reset icons, themes
