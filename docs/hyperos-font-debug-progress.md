# HyperOS Rust Font Debug Progress

This document tracks the HyperOS Rust/Flutter font investigation. New experiments, logs, and conclusions should be appended here.

## Current conclusions

- Normal Java apps mainly use app-process Java/Xposed hooks. Restarting the target app usually reloads the new app-process hooks.
- HyperOS Gallery and Weather also depend on the system_server `RustProcessImpl.startRustProcess` hook.
- Gallery text is mainly controlled through native paragraph hooks in `libhyper_os_flutter.so`, not Java `TextView` or `Paint`.
- Reinstalling DPIS does not reload already-running system_server module code. This limitation is specific to system_server/HyperOS Rust paths, not all apps.
- Runtime font values can be hot-published through root `setprop debug.dpis.font.<hash> <percent>` once the native proxy is already deployed and used by Gallery.

## Main runtime chain

- system_server hook: `HyperOsRustProcessHookInstaller` hooks `android.os.RustProcessImpl.startRustProcess`.
- Rust proxy: Gallery startup binary is redirected from `libapp_gallery.so` to sibling `libdpis_native.so`.
- Original binary path: `debug.dpis.rustbin.<hash>` points back to the original `libapp_gallery.so`.
- Font config: `debug.dpis.font.<hash>` or `debug.dpis.forcefont` provides the target percentage.
- Paragraph hooks: `ParagraphBuilder::Create` offset `0x81c368`, `ParagraphBuilder::pushStyle` offset `0x82370c`.

## Completed fixes

- Added native proxy build/load files: `app/src/main/cpp/dpis_native.cpp`, `app/src/main/cpp/CMakeLists.txt`, `app/src/main/assets/native_init`.
- Added HyperOS Rust process startup hook: `HyperOsRustProcessHookInstaller`.
- Added HyperOS Flutter font bridge: `HyperOsFlutterFontBridge`.
- Added native property cleaner: `HyperOsNativeFontPropertySyncer` clears `debug.dpis.font.<hash>` and `debug.dpis.rustbin.<hash>`.
- Fixed stale native property reset behavior when font config is cleared or the target package is disabled.
- Fixed local/remote config migration residue so removed target packages are not revived by old remote config.
- Fixed system_server config caching by making `PerAppDisplayConfigSource` use a dynamic StoreProvider.
- Added native proxy status UI: `HyperOsNativeProxyStatus`.
- Narrowed stale system_server runtime warning with `debug.dpis.module.system_server_loaded_at`.
- Hardened debug bridge components by marking them non-exported and removing `READ_LOGS`.

## 2026-04-27 Gallery font hot-publish bypass

- Goal: explain why normal Java apps can update after reinstall while HyperOS Gallery often needed a device reboot after same-version debug reinstall.
- Two explorer agents and local chain checks reached the same direction: do not switch the main path to RustPaint/RustCanvas/font cache hooks. Keep `RustProcessImpl.startRustProcess` -> sibling `libdpis_native.so` -> `libhyper_os_flutter.so` paragraph hooks as the stable path.
- Device evidence: the Gallery process has loaded `libdpis_native.so`, `libapp_gallery.so`, and `libhyper_os_flutter.so`.
- Device properties: `debug.dpis.font.a55b5fe1=100`, `debug.dpis.rustbin.a55b5fe1=/data/app/MIUIGallery/lib/arm64/libapp_gallery.so`.
- New practical route: if only the font percentage changes, DPIS can directly root-set `debug.dpis.forcefont.<hash>` and let the already-deployed native proxy read the new value.
- Code change: `AppConfigSaveHandler` now calls `HyperOsNativeFontPropertySyncer.publishForceFontTargetAsync(...)` when saving an enabled font config.
- Boundary: clearing or disabling font config still clears font/rustbin properties. Changes to RustProcess hook code or proxy deployment still require restarting system_server or the device.

## Next verification

1. Install the new debug APK without rebooting the device.
2. Change Gallery font to a visible value, for example 300%.
3. Check `adb -s d7121fb5 shell su -c getprop debug.dpis.font.a55b5fe1` immediately.
4. Only force-stop/restart Gallery.
5. Check for `ParagraphBuilder::Create override` or `ParagraphBuilder::pushStyle override` logs and confirm the multiplier matches the new value.

## Verification record

- `./gradlew.bat :app:testDebugUnitTest`: passed.
- `./gradlew.bat :app:assembleDebug`: passed.

## 2026-04-27 Force property bypass confirmed

- Evidence: DPIS app startup changed `debug.dpis.forcefont.a55b5fe1` to `300`, while old system_server code still rewrote `debug.dpis.font.a55b5fe1` to `50` during Gallery startup.
- Fix direction: native now reads `debug.dpis.forcefont.<hash>` before global `debug.dpis.forcefont`, env/cmdline, and `debug.dpis.font.<hash>`.
- Device verification: after replacing Gallery sibling `libdpis_native.so`, restarting only Gallery produced `ParagraphBuilder::Create/pushStyle multiplier=2.000000` even though system_server still emitted `DPIS_FONT_SCALE_PERCENT=50`.
- Meaning: font percentage changes can bypass stale system_server config as long as the target app uses the updated native proxy.
- Remaining boundary: changes to the Java RustProcess hook or proxy deployment path still require system_server/device restart.
