## ResourcesImpl updateConfiguration Hook

### Context
The module already installs a hook on `ResourcesManager.applyConfigurationToResources` inside the target package. The second hook must be installed during `onPackageReady` as well, keep the same package filter, and stay limited to the minimal proof-of-concept requirements described in the request. The new hook should run _before_ the original `ResourcesImpl.updateConfiguration(Configuration, DisplayMetrics, CompatibilityInfo)` call and adjust the incoming configuration/metrics similarly to the existing override logic.

### Approach options

1. **Mirror the existing ResourcesManager hook with a dedicated installer.** Create `ResourcesImplHookInstaller` that uses reflection to locate `ResourcesImpl.updateConfiguration(...)` and intercept it via Xposed just like the first hook. Inside the interceptor adjust `Configuration.densityDpi` and, when `DisplayMetrics` is provided, synchronize `densityDpi`, `density`, and `scaledDensity`.
2. **Hook at a higher level (e.g., `ResourcesImpl.getDisplayMetrics`).** Instead of intercepting `updateConfiguration`, hook another entrypoint and patch the metrics when they are requested. This spreads the logic into more call sites and makes the PoC wider than requested.
3. **Reuse the ResourceManager hook to trigger a flag and patch `ResourcesImpl` only when that flag is observed.** Adds unnecessary state-tracking and doesn't provide extra value for this task.

**Recommendation:** Option 1 keeps the implementation focused, reuses the current installer pattern, and matches the requirement for installing the second hook before `updateConfiguration` runs.

### Selected design

1. **New installer.** `ResourcesImplHookInstaller` will mirror the structure of `ResourcesManagerHookInstaller`. It will load `android.content.res.ResourcesImpl` and `CompatibilityInfo`, hook `updateConfiguration`, and intercept calls. Before proceeding it invokes a helper that resolves the configured density override, mutates `Configuration.densityDpi`, and (when provided) refreshes `DisplayMetrics.densityDpi`, `density`, and `scaledDensity`. It also logs the override for troubleshooting.
2. **Installation moment.** `ModuleMain.onPackageReady` keeps the package filter and now calls both installers inside the same `try` block so both hooks are registered before the package is considered ready. Failures in either hook will remain logged via `DpisLog.e` but will not crash the host.
3. **Helper access.** The helper that mutates `Configuration`/`DisplayMetrics` is package-private so unit tests can exercise it without exposing any new public API.

### Testing

1. Add `ResourcesImplHookInstallerTest` that exercises the helper method:
   * Verifies `Configuration.densityDpi` changes when the override applies and that `DisplayMetrics` remains untouched when null.
   * Validates that `DisplayMetrics.densityDpi`, `density`, and `scaledDensity` are updated when a non-null metrics object is passed.
2. Run `./gradlew :app:testDebugUnitTest` to ensure the unit tests compile against the Android stubs and pass.

### Notes

- This change keeps the implementation scoped to the requested hook; no additional hooks or refactors are introduced.
- Density adjustments reuse `DensityOverride` so both hooks behave consistently.
- The spec assumes the target package remains `com.android.settings`. If that configuration changes, the hook boundaries remain the same.

Spec written and saved to `docs/superpowers/specs/2026-04-14-resourcesimpl-hook-design.md`. Please review it and let me know if you want any edits before I move on to implementation detail planning.
