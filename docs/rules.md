# Rules

1. No comments in any source file. Checked by tools/check_nocomments.py.
2. No hardcoded limits. Every count, size, delay and maximum is a textbox setting with a default and a reset button.
3. No telemetry. Network is used only when the user enables AI mode and sets a URL.
4. Textboxes everywhere, including beside sliders. Sort controls are dropdowns.
5. Every setting has an id, label, info text, default, reset icon and search keywords.
6. Tabs load lazily in chunks; hidden tabs release their views.
7. Key input uses down and up events per pointer so multi-touch and hold work.
8. Themes: light, dark, pitch black. Never same-colour text on background.
9. UI checks: headless Chromium snapshots through Playwright (PLAYWRIGHT_BROWSERS_PATH=/opt/pw-browsers).
