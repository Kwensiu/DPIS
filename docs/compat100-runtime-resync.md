# compat100 Runtime Resync

This document records the compat100 runtime route decisions present in this
branch.

## WeChat Native Target Field

Status: active.

Scope: `com.tencent.mm` main WeChat process only. The route does not target
AppBrand, XWeb, TBS, or mini-program processes.

Mechanism:

- The module publishes the configured value through
  `debug.dpis.wechat.targetfield.c5fe9776`.
- `Compat100LegacyModuleHook` treats WeChat as a dedicated route. The main
  process installs the WeChat hook; secondary WeChat processes return without
  generic viewport/font hooks.
- Current WeChat builds use `w45.f.g()` to read
  `screenResolution_target_field`; DPIS overrides that getter result.
- Older investigated builds may expose a `q35.f` constructor field route; that
  remains only as a fallback.
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

- The current device build maps `q35.f` to a RecoveryToolsUI click listener, so
  the old hard-coded `q35.f.screenResolution_target_field` route fails with
  `NoSuchFieldException`. The active route is therefore `w45.f.g()`.
- The device rejects writes to
  `persist.debug.dpis.wechat.targetfield.c5fe9776`, and an old persistent value
  can survive reboot. The hook intentionally ignores the persistent property and
  reads only the writable `debug` property.
- Runtime validation on this device showed no visible change until DPIS was
  opened once after reboot, which republished
  `debug.dpis.wechat.targetfield.c5fe9776`; restarting WeChat after that made
  the route visibly effective.

UI and storage:

- The app config sheet shows a WeChat-only integer input under the font input.
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
