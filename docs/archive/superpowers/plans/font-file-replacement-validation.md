# Font File Replacement Validation

## Automated

- `./gradlew :app:testAllDebugUnitTests`
- `./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`

## Real Device

1. Install `app/build/outputs/apk/modern101/debug/app-modern101-debug.apk`.
2. Enable DPIS for a normal Java Android app in LSPosed.
3. Open DPIS settings, import a valid `.ttf` or `.otf` in Font library.
4. Open the target app config and select the imported font under Font file.
5. Leave DP width and font size empty.
6. Save and restart the target app.
7. Confirm ordinary `TextView` text changes font.
8. Check logcat for `DPIS_FONT_STYLE hook ready`.
9. Confirm the target app does not crash.
10. Clear the app font file selection, save, restart the target app, and confirm system font behavior returns.

## Out of Scope

- HyperOS Gallery/Weather Flutter/native text is not expected to change in this MVP.
- Backup JSON carries the selected font ID preference, but does not include font binary files.
