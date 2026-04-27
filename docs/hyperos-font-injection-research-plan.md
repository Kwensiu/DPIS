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
