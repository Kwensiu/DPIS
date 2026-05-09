# HyperOS Font Injection Progress

## Current Conclusion

- HyperOS Gallery/Weather can bypass normal Java-only hooks through a Rust/Flutter native path.
- The supported path is the target-app sibling proxy `libdpis_native.so`, prepared by DPIS and loaded by the HyperOS Rust process hook.
- DP/font runtime values are published through package-hash system properties so repeated value changes do not require replacing native files again.
- The UI keeps this as part of normal Save/Reset behavior instead of exposing separate proxy buttons.

## User-Facing Flow

- Save non-empty DP/font config: DPIS writes config and prepares HyperOS compatibility support when applicable.
- Save empty config: DPIS clears config and rolls back HyperOS compatibility support when applicable.
- Reset button only clears dialog inputs; the rollback happens after saving, keeping persisted config and runtime state aligned.
- Success is intentionally quiet; failures show a concise compatibility setup/rollback error.

## Technical Notes

- The mount helper uses strict command chaining and fixed-string mount checks so failed bind mounts do not report success.
- The Rust process hook no longer contains the temporary external-path or `LD_PRELOAD` experiment branches.
- Remaining improvement: shared root/setprop helper can reduce duplication between font and viewport runtime property syncers.

## Validation

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

## Weather 2026-04-28 Fix

- `com.miui.weather2` hash is `4568f00f`; runtime config was correctly published as `debug.dpis.forcefont.4568f00f=300` and `debug.dpis.vp.4568f00f=500`.
- Weather failed earlier for two separate reasons:
  - its native directory did not already contain `libdpis_native.so`, so the old mount planner rejected first-time proxy setup;
  - native config refresh returned immediately after JNI/env configuration, so the runtime `forcefont` property could not override the stale `DPIS_FONT_SCALE_PERCENT=100` value.
- Fixes:
  - proxy setup now accepts an existing target native directory even when the target mount-point file is missing, and the root apply command creates the file before bind mount;
  - native font config priority is now `debug.dpis.forcefont.<hash>` -> `debug.dpis.forcefont` -> existing JNI/env/default state, so runtime Weather values can override the process-start value.
- Regression coverage:
  - missing target mount point is valid when the parent native directory exists;
  - missing parent native directory still fails early;
  - apply command creates the mount point and still verifies mount/hash;
  - native source smoke test guards against reintroducing the JNI early-return priority bug.
- Additional Weather evidence: after the proxy loaded, `ParagraphBuilder::Create` logged `d1=0.000000` and multiplier stayed `1.000000`; this means Weather's Flutter build does not pass the observed font scale through the same Create argument used by Gallery.
- Additional fix: Create-hook scale calculation now falls back to an observed scale of `1.0` when the observed argument is zero/invalid, and the Create trampoline no longer scales the sentinel `d0` register. This keeps Gallery's normal path while allowing Weather's `d2` font-size argument to receive the configured multiplier.

## Weather Blank UI After Reboot 2026-04-28

- Root cause: bind mounts do not survive device reboot, but the Weather target mount-point file created by `touch` stayed behind as a 0-byte `/data/app/MIUIWeather/lib/arm64/libdpis_native.so`.
- Failure mode: old system_server hook could still redirect Weather's Rust binary to that sibling path, so Weather attempted to start through an empty native file and only reached a blank/background state.
- Fixes:
  - Rust process proxy resolution now requires sibling `libdpis_native.so` to be a non-empty file before redirecting the Rust binary.
  - proxy status inspection also treats a 0-byte placeholder as missing, not present.
- Immediate device recovery used during verification: remove the stale 0-byte target file, then restart Weather. Weather resumed normal logs without DPIS native injection.
- Operational note: after a device reboot, HyperOS native proxy support must be prepared again before launching target apps if the bind mount was lost.
- Follow-up: installing a new APK does not immediately refresh the already-loaded LSPosed code inside `system_server`; before reboot, stale code may still redirect to a leftover 0-byte sibling file.
- Additional hardening: proxy apply now refuses to overwrite a non-empty target unless it already matches DPIS's proxy, then copies the proxy and removes only matching DPIS-owned copies during rollback.
- Clarification: DPIS's app restart button only force-stops and starts the target app; it does not reload LSPosed code already resident in `system_server`.
- Fix: for HyperOS native proxy candidates with a configured font scale, the DPIS restart button now prepares/rebinds the native proxy first, then restarts the target app.
- Follow-up: Android shell parsing differs between `adb shell su -c ...` and Java `ProcessBuilder("su", "-c", command)`, and grouped `(...)` commands are fragile on this device path. The prepare command was rewritten to avoid parenthesized groups and use mksh-compatible copy/remove commands.
- Device validation: with the rewritten command form, preparing `/data/app/MIUIWeather/lib/arm64/libdpis_native.so` succeeds and Weather starts as process `23901`.

## Weather FontScale Native GOT Path 2026-04-28

- New evidence: Weather's visible font path does not reliably use Gallery's `ParagraphBuilder`/TextStyle construction offsets as the primary source of truth.
- `libweather_app.so` imports `Configuration_get_font_scale` from the HyperOS public/native API layer and calls it during startup.
- A same-name export in `libdpis_native.so` alone does not intercept the call; the weather Rust library resolves its own PLT/GOT entry.
- Verified route: patch `libweather_app.so`'s `Configuration_get_font_scale` GOT slot to DPIS's native override after the original Rust library is loaded.
- Device evidence:
  - GOT hook installed for `com.miui.weather2`.
  - `Configuration_get_font_scale` override logs `value=3.000000` under a 300% font config.
  - Weather process stays alive after launch; unrelated `com.xiaomi.market` crashes appeared in logcat and are not the Weather process.
- Cleanup: removed the temporary Weather TextStyle/entry probe because it was not the root path and could confuse validation.
- Remaining visual validation: user should confirm whether all Weather text now visibly follows the configured font scale; logs only prove the native font-scale source is overridden.
