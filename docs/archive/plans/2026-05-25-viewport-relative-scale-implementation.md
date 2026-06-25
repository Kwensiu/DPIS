# Viewport Relative Scale Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a screen-relative viewport target mode while preserving legacy absolute dp behavior and making `auto` silently fall back from system-server production to app-process compatibility.

**Architecture:** Store viewport target semantics as `ViewportTargetSpec` (`relative_scale`, `absolute_dp`, `off`) and apply strategy separately as `auto`, `system`, or `compat`. Runtime hooks first resolve a target spec against a trusted display source into one effective `smallestWidthDp`, then reuse the existing `ViewportOverride` and `VirtualDisplayOverride` math. System-server and app-process routes share compact runtime markers keyed by target fingerprint so relative scale is not repeatedly derived from a DPIS result.

**Tech Stack:** Java 17 Android module, SharedPreferences config, Android system properties, JUnit4 unit tests, existing Xposed hook installers.

---

### Task 1: Target Semantics And Store

**Files:**
- Create: `app/src/main/java/com/dpis/module/ViewportTargetType.java`
- Create: `app/src/main/java/com/dpis/module/ViewportTargetSpec.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportApplyMode.java`
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Test: `app/src/test/java/com/dpis/module/ViewportTargetSpecTest.java`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreViewportTargetTest.java`

- [ ] Add target type constants: `relative_scale`, `absolute_dp`, `off`.
- [ ] Add `ViewportTargetSpec` with `off()`, `relativeScale(int permille)`, `absoluteDp(int widthDp)`, `isEnabled()`, `activeValue()`, and `fingerprint()`.
- [ ] Normalize relative scale to `500..2000` permille and absolute dp to `>=1`.
- [ ] Add `ViewportApplyMode.AUTO`, `SYSTEM`, and `COMPAT`; normalize legacy `system_emulation` to `system` and `field_rewrite` to `compat`.
- [ ] Keep legacy constants as aliases so old call sites compile while returning new persisted values.
- [ ] Add store keys `viewport.<package>.target_type` and `viewport.<package>.scale_permille`.
- [ ] Preserve existing `viewport.<package>.width_dp` as the absolute dp value.
- [ ] Implement `getTargetViewportSpec(packageName)`: explicit type wins; missing type plus legacy width returns `absolute_dp`; no target returns `off`.
- [ ] Implement `setTargetViewportSpec(packageName, spec)` and update package membership removal checks to include both new keys.
- [ ] Preserve legacy apply strategy: existing width with no mode returns `system`; newly created targets default to `auto` only when the caller uses the new save API.
- [ ] Unit-test legacy width-only migration behavior, relative normalization, absolute preservation, and target clearing.

### Task 2: Source Snapshot And Target Resolver

**Files:**
- Create: `app/src/main/java/com/dpis/module/ViewportSourceSnapshot.java`
- Create: `app/src/main/java/com/dpis/module/ViewportTargetResolution.java`
- Modify: `app/src/main/java/com/dpis/module/TargetViewportWidthResolver.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportRuntimeMarkerBridge.java`
- Test: `app/src/test/java/com/dpis/module/ViewportSourceSnapshotTest.java`
- Test: `app/src/test/java/com/dpis/module/ViewportTargetResolverTest.java`

- [ ] Add `ViewportSourceSnapshot` factories for configuration sources and system display sources.
- [ ] Wrap `ViewportConfigurationScope.isWindowScoped()` into snapshot scope without replacing that detector.
- [ ] Add deterministic source/result signature helpers through `ViewportRuntimeMarkerBridge`.
- [ ] Resolve `absolute_dp` by returning the stored absolute dp.
- [ ] Resolve `relative_scale` by `Math.round(source.smallestWidthDp * scalePermille / 1000.0f)`, clamped to at least `1`.
- [ ] Reject invalid, unknown, read-only, or window-scoped sources as fresh relative baselines.
- [ ] Permit window-scoped sources to borrow an existing runtime record for the same target fingerprint.
- [ ] Unit-test issue 64 fixture: `850dp @ 1060 -> 901`, `411dp @ 1060 -> 436`, and relative mode never resolves outer display to `900dp`.

### Task 3: Runtime Records And Marker Import

**Files:**
- Create: `app/src/main/java/com/dpis/module/ViewportRuntimeRecord.java`
- Modify: `app/src/main/java/com/dpis/module/VirtualDisplayState.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportRuntimeMarkerBridge.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportRuntimeMarkerProbe.java`
- Test: `app/src/test/java/com/dpis/module/VirtualDisplayStateTest.java`
- Test: `app/src/test/java/com/dpis/module/ViewportRuntimeMarkerBridgeTest.java`

- [ ] Store runtime records by target fingerprint and source/result signature instead of one unqualified global result.
- [ ] Keep compatibility getters (`get()`, `getForTarget`) as adapters for existing consumers during migration.
- [ ] Add `findForFreshSource`, `findForAlreadyAppliedResult`, and `findDisplayRecordForTarget`.
- [ ] Import a system marker into an app-process runtime record only when package hash, target fingerprint, age, and signature match.
- [ ] Generalize marker target fingerprints from absolute-only `a<dp>` to target-spec fingerprints (`a<dp>`, `r<permille>`).
- [ ] Preserve debug-only marker probe logs, with rate limiting.
- [ ] Unit-test stale, malformed, target mismatch, source hit, result hit, and window borrow behavior.

### Task 4: App-Process Hook Migration

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesReadHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesProbeHookInstaller.java`
- Test: existing hook tests plus targeted additions in `ResourcesImplHookInstallerTest.java` and `ResourcesManagerHookInstallerTest.java`

- [ ] Replace raw `Integer targetViewportWidth` resolution with `ViewportTargetResolution`.
- [ ] In `ResourcesImpl`, use configuration plus trusted `DisplayMetrics` as the primary app-process display source.
- [ ] In `ResourcesManager`, prefer runtime record reuse; publish only when source is display-scoped and safe.
- [ ] In `ResourcesRead`, never publish a fresh relative baseline; only repair from matching records.
- [ ] Keep absolute dp behavior unchanged by resolving it to the same effective target before calling `ViewportOverride`.
- [ ] Preserve existing font-scale behavior and log messages, adding target spec/effective target fields where useful.
- [ ] Unit-test no double-application when the second callback sees an already-applied relative result.

### Task 5: System-Server Hook Migration

**Files:**
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayOverrideCalculator.java`
- Modify: `app/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java`
- Test: `app/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java`
- Test: `app/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest.java`

- [ ] Carry `ViewportTargetSpec` in `PerAppDisplayConfig`.
- [ ] Resolve system-server relative targets from display-scoped source snapshots.
- [ ] Publish runtime marker before safe-mode pre-proceed mutation.
- [ ] In `auto` relative mode, skip system-server viewport mutation when marker publication fails so app-process compat remains the first producer.
- [ ] Keep explicit `system` diagnostic behavior logged when marker publication fails.
- [ ] Preserve existing absolute dp mutation behavior.

### Task 6: Properties, UI, And Status Text

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ViewportPropertyBridge.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportPropertySyncer.java`
- Modify: `app/src/main/java/com/dpis/module/SystemPropertyConfigPreferences.java`
- Modify: `app/src/main/java/com/dpis/module/AppListItem.java`
- Modify: `app/src/main/java/com/dpis/module/InstalledAppCatalogCoordinator.java`
- Modify: `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
- Modify: `app/src/main/java/com/dpis/module/AppConfigSaveHandler.java`
- Modify: `app/src/main/java/com/dpis/module/AppStatusFormatter.java`
- Modify: `app/src/main/res/layout/dialog_app_config.xml`
- Modify: `app/src/main/res/layout/dialog_font_hook_domains.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Test: source smoke tests and formatter tests.

- [ ] Publish target type and active value through runtime properties for compat100/system-server readers.
- [ ] Read legacy absolute dp properties as `absolute_dp` when no type property exists.
- [ ] Change the app sheet viewport toggle labels to `Scale` / `Fixed dp` (`比例` / `固定 dp`).
- [ ] Use one input field; switching the toggle switches displayed stored values and only the active mode saves.
- [ ] Relative input accepts whole percent `50..200` and stores permille.
- [ ] Absolute input accepts integer dp `>=1` and stores width dp.
- [ ] Move apply strategy editing into the existing hook-chain dialog and do not show a target summary there.
- [ ] Default new viewport targets to relative scale and `auto`; preserve migrated legacy configs as absolute and `system`.
- [ ] Update list/status text to show relative targets as percent and absolute targets as dp.

### Task 7: Verification And Cleanup

**Files:**
- Modify tests only as needed.
- No release-only behavior should depend on debug probes.

- [ ] Run targeted unit tests for target spec, store, resolver, runtime state, marker bridge, app hooks, system-server hooks, property sync, formatter, and source smoke tests.
- [ ] Run `./gradlew :app:testAllDebugUnitTests`.
- [ ] Run `./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`.
- [ ] Run `git diff --check`.
- [ ] Confirm debug marker probe remains gated by `BuildConfig.DEBUG`.
- [ ] Record real-device validation commands for next review: `adb shell getprop | Select-String -Pattern "debug.dpis.vprtm","debug.dpis.vp"` and filtered `adb logcat` with `DPIS_VIEWPORT_MARKER|DPIS_VIEWPORT`.
