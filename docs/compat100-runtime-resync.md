# compat100 Runtime Resync Notes

This note records the current compat100 recovery model for long-term reading.

## What is persisted

- compat100 still stores the source of truth in DPIS prefs.
- On LSPosed-based 100 builds, that prefs file lives under the LSPosed-generated
  `shared_prefs` mirror path in `/data/misc/<uuid>/prefs/io.github.kwensiu.dpis/`.
- The ordinary app-private `/data/user/0/io.github.kwensiu.dpis/shared_prefs/`
  tree is not the only place to look when validating compat100 state.

## What is mirrored at runtime

- Viewport and font settings are also mirrored into `debug.dpis.*` system properties.
- Those properties are runtime mirrors, not the only source of truth.
- They can disappear across reboot or be blocked by ROM startup policy.

## Recovery model

- `RuntimePropertyRecoveryCoordinator` replays the persisted store into runtime mirrors.
- `DpisApplication` calls the coordinator on process start and on Xposed service
  bind/died transitions.
- `DpisPackageLifecycleReceiver` also calls it for `MY_PACKAGE_REPLACED` and
  `BOOT_COMPLETED`, but those broadcasts are best-effort only.

## Boundary

- `BOOT_COMPLETED` is not the correctness requirement for compat100 recovery.
- If the ROM blocks auto-start, the app can still recover when DPIS itself starts.
- The coordinator is intentionally idempotent so these entrypoints can replay it safely.

## Practical check

1. Save a target package in the UI.
2. Confirm the LSPosed prefs file contains the package entry.
3. Start DPIS or reopen it after reboot.
4. Confirm `debug.dpis.*` mirrors are republished.
