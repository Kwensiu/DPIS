# compat100 Runtime Resync

This document records the WeChat runtime route decisions present in this
branch.

## WeChat Native Target Field

Status: active.

Scope: `com.tencent.mm` main WeChat process only. The route does not target
AppBrand, XWeb, TBS, or mini-program processes.

Mechanism:

- The module publishes the configured value through
  `debug.dpis.wechat.targetfield.c5fe9776`.
- `Compat100LegacyModuleHook` and modern101 `ModuleMain` treat WeChat as a
  dedicated route. The main process installs the WeChat hook; secondary WeChat
  processes return without generic viewport/font hooks.
- Tested WeChat builds expose the target through version-specific obfuscated
  anchors. Known anchors are `w45.f#g()` on WeChat `8.0.72`, constructor field
  `q35.f#screenResolution_target_field` on WeChat `8.0.71`, `d25.f#g()` on
  WeChat `8.0.70`, `az4.f#g()` on WeChat `8.0.69`, and `hy3.d#g()` on WeChat
  `8.0.42`.
- Runtime selection uses the shared `WechatTargetFieldRoutes.java` registry
  keyed by WeChat `versionCode`. All entries are peers; there is no primary
  route and no probing fallback for unlisted versions.
- compat100 and modern101 use different hook APIs, but both consume the same
  route registry and runtime property.
- The route registry is Java code rather than runtime JSON to keep Xposed app
  process startup deterministic: no asset IO, JSON parser, or resource access
  is required before the hook is installed. Locator JSON output is still useful
  as a maintenance aid for updating the registry.
- Unlisted versions are treated as unsupported and are left untouched until a
  locator result is verified and added to the registry.
- Valid values are `300..1200`; invalid or missing properties resolve to `0`
  and leave WeChat untouched.
- The per-app DPIS enable toggle controls the runtime property: disabled writes
  `0`, enabled republishes the stored WeChat target value.
- The `debug` property is a volatile runtime mirror. After reboot or module
  reload, opening DPIS or saving the WeChat config republishes the stored value;
  WeChat must then be restarted so `w45.f.g()` observes the refreshed property.
- Saving WeChat settings, including a blank target-field input, clears the old
  generic viewport configuration for `com.tencent.mm` so stale `width_dp`
  values do not keep shrinking WeChat or get migrated back later.
- A blank WeChat target-field input clears the WeChat route and publishes `0`;
  it does not fall back to the generic viewport input.
- Runtime recovery also migrates a legacy WeChat viewport width in `300..1200`
  into the WeChat target-field route before publishing properties. This handles
  devices that already had `viewport.com.tencent.mm.width_dp=300` from earlier
  experiments.

Runtime finding:

- WeChat `8.0.72` maps `q35.f` to a RecoveryToolsUI click listener, so
  `q35.f.screenResolution_target_field` must not be used as a cross-version
  fallback. It remains valid only for the verified WeChat `8.0.71`
  (`versionCode=3080`) constructor-field route.
- Locator output added WeChat `8.0.69` (`versionCode=3040`) as
  `classes12.dex az4.f#g()`, and WeChat `8.0.42` (`versionCode=2460`) as
  `classes6.dex hy3.d#g()`.
- The device rejects writes to
  `persist.debug.dpis.wechat.targetfield.c5fe9776`, and an old persistent value
  can survive reboot. The hook intentionally ignores the persistent property and
  reads only the writable `debug` property.
- Runtime validation on this device showed no visible change until DPIS was
  opened once after reboot, which republished
  `debug.dpis.wechat.targetfield.c5fe9776`; restarting WeChat after that made
  the route visibly effective.

Locator:

- Use `tools/wechat_target_field_locator.py <wechat.apk>` or the PowerShell
  wrapper `tools/wechat-target-field-locator.ps1 -ApkPath <wechat.apk>` to
  locate version-specific candidates quickly.
- The locator parses DEX directly. It searches for
  `screenResolution_target_field`, then reports methods that reference the
  string. A high-confidence getter is a static `()I` method, such as the current
  `w45.f#g`; a medium-confidence setter is a static `(I)V` method, such as
  `w45.f#k`.
- The locator is an adaptation aid only. Runtime code should still treat
  locator output as a candidate and verify with LSPosed hook-ready and callback
  evidence before adding a new route entry.
- Constructor-field routes write the target after the original constructor has
  run, so the constructor body cannot overwrite the DPIS value before the
  object is used.

UI and storage:

- The app config sheet shows a WeChat-only integer input under the font input.
- If the installed WeChat `versionCode` is not present in
  `WechatTargetFieldRoutes.java`, the sheet shows an unsupported-version hint.
  A non-empty value is invalid in that state, but a blank value remains
  saveable so stale WeChat configuration can be cleared.
- The value is stored as `wechat.com.tencent.mm.target_field`.
- A stored WeChat target field counts as app-specific configuration so WeChat
  appears in the configured-apps page even without viewport/font values.

## Rejected Routes

Status: rejected for this branch.

- Mini-program/AppBrand/XWeb/TBS mutation is intentionally not included. The
  observed mini-program behavior was unstable and visually inverted compared
  with the native WeChat route.
- ResourceHelper/p35 forcing is intentionally not included.
- Density-only WAux/NewMiko-style replacement is intentionally not included in
  this branch; this branch keeps only the final native target-field route.
