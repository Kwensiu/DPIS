# Font Hook Execution Plan Refactor

This note records the intended refactor for DPIS app-process hook planning. It is an explanation/design note, not an implementation checklist.

## Current State

The modern app-process path already has a partial planner:

- `AppProcessHookInstaller.resolveFontHookPlan(...)` resolves whether font handling is off, system-emulation, or field-rewrite.
- `FontHookArbitration.resolveDomainPlan(...)` turns that into per-domain font hook switches.
- `AppProcessHookInstaller.install(...)` installs the concrete hook installers.

That means the refactor is not about adding planning from scratch. The problem is that the current plan is split across several objects and several installer-time decisions:

- `FontHookPlan` and `FontDomainPlan` duplicate mode state through booleans.
- `AppProcessHookInstaller` still mutates the domain plan for debug Flutter settings overrides.
- `Resources*HookInstaller` installation is decided by merging viewport, font emulation, and font domain state inside the installer.
- Debug-only paths are represented both in the plan and in repeated `!debugFlutterSettingsOnly` checks during installation.
- `reason` is a free-form string, so tests can only assert behavior indirectly or by substring.

The result is workable, but the semantic boundary between "decide what should run" and "install what was decided" is not strict.

## Goal

Make app-process hook installation plan-driven:

- One planner produces the final execution plan.
- One orchestrator installs hooks from that final plan.
- The orchestrator does not re-read mode state, merge viewport/font booleans, or know about debug override properties.
- Tests assert structured plan fields instead of reverse-engineering booleans or log strings.

## Proposed Units

### `FontMode`

Runtime execution mode:

```java
enum FontMode {
    OFF,
    EMULATION,
    FIELD_REWRITE
}
```

`FontApplyMode` remains the persisted string/config vocabulary. `FontMode` is the normalized runtime decision.

### `DebugFontOverride`

Small value object passed into the planner:

```java
final class DebugFontOverride {
    final boolean forceFlutterSettings;
    final boolean flutterSettingsOnly;
}
```

`SystemProperties` and `BuildConfig.DEBUG` stay outside the planner. `AppProcessHookInstaller` or a small adapter may read them, then pass this value object into the planner. This keeps planner tests deterministic.

### `HookExecutionPlan`

Final app-process hook plan. It should include font, viewport, and probe installation decisions because `Resources*` hooks are shared by viewport and font emulation.

Recommended fields:

```java
final class HookExecutionPlan {
    final FontMode fontMode;

    final boolean viewportEnabled;
    final boolean resourcesHooksEnabled;
    final boolean activityThreadFontEnabled;
    final boolean textViewHooksEnabled;
    final boolean webViewTextZoomEnabled;
    final boolean flutterSettingsEnabled;
    final boolean hyperOsNativeFlutterEnabled;

    final boolean resourcesProbeEnabled;
    final boolean viewportProbeEnabled;

    final FontHookArbitration.FontDomainPlan fontDomainPlan;
    final PlanReason reason;
}
```

Keeping `FontDomainPlan` during the first refactor reduces churn for `ForceTextSizeHookInstaller` and `FlutterSettingsFontHookInstaller`, which already consume it.

### `PlanReason`

Structured explanation for logging and tests:

```java
final class PlanReason {
    final String primary;
    final String fallback;
    final String suppressed;
    final String debugOverride;
    final String downgrade;
}
```

The exact type can be simple strings or lists. The important rule is that tests should assert fields, not parse a sentence.

### `HookExecutionPlanner`

Pure planner. Inputs:

- `HookRuntimePolicy`
- viewport configured flag and mode
- font scale active flag and mode
- Flutter settings flag
- HyperOS native Flutter flag
- `DebugFontOverride`

Output:

- `HookExecutionPlan`

Responsibilities:

- Normalize viewport mode.
- Normalize font mode.
- Apply safe-mode downgrade state if needed.
- Resolve font domain plan.
- Apply debug Flutter settings override.
- Merge viewport/font/debug decisions into `resourcesHooksEnabled`.
- Decide probe hook installation fields.

It must not:

- Read `SystemProperties`.
- Read `BuildConfig.DEBUG`.
- Install hooks.
- Log directly.

### `HookExecutionOrchestrator`

Thin installer:

```java
if (plan.resourcesHooksEnabled) {
    ResourcesManagerHookInstaller.install(...);
    ResourcesImplHookInstaller.install(...);
    ResourcesReadHookInstaller.install(...);
}
if (plan.activityThreadFontEnabled) {
    ActivityThreadFontHookInstaller.install(...);
}
if (plan.textViewHooksEnabled) {
    ForceTextSizeHookInstaller.install(..., plan.fontDomainPlan);
}
if (plan.flutterSettingsEnabled) {
    FlutterSettingsFontHookInstaller.install(..., plan.fontDomainPlan);
}
if (plan.hyperOsNativeFlutterEnabled) {
    HyperOsFlutterFontHookInstaller.install(...);
}
if (plan.webViewTextZoomEnabled) {
    WebViewFontHookInstaller.install(...);
}
```

The orchestrator only checks final booleans. It should not contain `debugFlutterSettingsOnly`, `debugForceFlutterSettings`, or `viewportEnabled || font...` merge expressions.

## Required Constraints

1. Planner output is final.

   Debug overrides, safe-mode downgrade, viewport/resource coupling, and probe decisions all belong in `HookExecutionPlan`.

2. Runtime mode is explicit.

   Do not infer mode from `emulationEnabled` and `fieldRewriteEnabled`. Use `FontMode`.

3. Debug state is injected.

   Property reading remains outside the planner.

4. Reason is structured.

   Logging may format it as text, but planner tests assert fields.

5. Behavior is preserved first.

   The first PR should migrate the decision shape without changing which hooks install for existing configurations.

## Migration Order

1. Add `FontMode`, `DebugFontOverride`, `PlanReason`, and `HookExecutionPlan`.
2. Add `HookExecutionPlanner` and port the existing `resolveViewportHookEnabled`, `resolveFontHookPlan`, `resolveFontDomainPlan`, and `resolveResourcesHooksEnabled` behavior into it.
3. Add table-driven planner tests for:
   - font off
   - font emulation
   - field rewrite
   - viewport-only resources hooks
   - debug force Flutter settings
   - debug Flutter-settings-only
   - probe enabled/disabled
4. Change `AppProcessHookInstaller.install(...)` to build a plan and pass it to a thin orchestrator.
5. Remove installer-time debug plan mutation and repeated `!debugFlutterSettingsOnly` checks.
6. Keep `FontHookArbitration` initially, then decide whether to fold it into `HookExecutionPlanner` after behavior is stable.

## Non-Goals

- Do not change compat100 in the first pass.
- Do not change native Flutter hook behavior.
- Do not optimize hot-path performance in this refactor.
- Do not rewrite `ForceTextSizeHookInstaller` internals.

## Validation

Run focused unit tests first:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppProcessHookInstallerTest
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FontHookArbitrationTest
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModulePackagePlanTest
```

Before submitting:

```powershell
./gradlew :app:testAllDebugUnitTests
```
