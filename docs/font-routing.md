# Font Routing Semantics

This document defines the stable language for DPIS font scaling routes. It is
about product and runtime semantics, not app-specific investigation notes.

## Mode Tree

```text
requested font mode
  |
  +-- system
  |     |
  |     +-- DPIS owns scheduling across internal runtime domains
  |     +-- system_server_font may write Configuration.fontScale at launch
  |     +-- activity_thread_font may supplement app bind configuration
  |     +-- Resources / WebView semantic supplements may remain available
  |     +-- custom hook-chain UI state is ignored
  |
  +-- compat
  |     |
  |     +-- custom hook-chain UI state may select field-rewrite domains
  |     +-- Resources / TextView / Paint / WebView field routes apply in app process
  |
  +-- off
        |
        +-- no DPIS font route
```

## Ownership Boundaries

- User-facing font mode selects the high-level strategy: `system`, `compat`, or
  `off`.
- The custom font hook-chain dialog configures only compat / field-rewrite
  routes.
- Source ownership mirrors that semantic split: hook-domain planning lives under
  `fonts.hookdomain`, while app-process font property delivery, resource-font
  scheduling, and font hook installers live under `runtime.font`.
- `system_server_font` and `activity_thread_font` are internal scheduler
  domains. They are not user-customizable hook-chain switches and are not saved
  in custom hook-chain overrides.
- Restoring the custom hook-chain defaults returns to the compat recommended
  template. It must not rewrite the system-mode internal scheduler state.

## System Mode Scheduling

System mode should be stable even when an internal domain is risky for a
particular runtime entry. Prefer scheduler policy over user-facing package
recommendations.

- `FONT_SCALE` is launch-only in `system_server`.
- Later lifecycle entries such as `config-dispatch` must not receive font-only
  configs.
- Viewport mutation remains multi-entry because Activity-level and display-level
  viewport state may need later lifecycle synchronization.
- Package selection in `system_server` is entry-aware and field-aware:
  font-only configs can enter `launch-activity-item`; viewport configs can
  enter viewport hot paths such as `config-dispatch` and `display-manager-info`.

### Why FONT_SCALE is launch-only (and the runtime tradeoff)

Writing `Configuration.fontScale` during `config-dispatch`
(`ActivityRecord.updateReportedConfigurationAndSend`, the authoritative path
that delivers configuration to a running app) can flip the reported
`CONFIG_FONT_SCALE` bit and trigger an Activity relaunch (window recreation).
That relaunch is heavier and more visible than the residual cost of leaving the
config untouched, so system mode deliberately keeps `FONT_SCALE` writes on the
launch entry only.

The accepted consequence is a runtime split that some apps can observe:

- `launch-activity-item` and `activity_thread_font` set `fontScale` to the
  target at bind time, but every later `config-dispatch` re-delivers the
  system's base `fontScale`. `system_server` does not re-assert the target there.
- The app-process `resources_font` read path then sees the target only on
  `DisplayMetrics.scaledDensity` (which it may fill), while
  `Configuration.fontScale` stays at the system base (system mode does not force
  config on every read; see the Resources Font Event Gate section).
- Apps that size layout from `DisplayMetrics`/sp scale correctly. Apps that read
  `Configuration.fontScale` directly may keep recomputing against the base/target
  mismatch, which shows up as light residual jank rather than a relaunch.

This is an intentional system-mode tradeoff, not a bug: it prioritizes low
invasiveness and relaunch avoidance over runtime config/metrics consistency.
The supported exit for an app that needs both values consistent is `compat`
mode, which unifies `Configuration.fontScale` and `DisplayMetrics` on the
app-process read path without going through `system_server` `config-dispatch`,
so it removes the mismatch without provoking a relaunch.

## App-Specific Evidence

App-specific repros, such as flicker in a video app or social app, are evidence
for route behavior. They are not enough by themselves to create built-in
recommended route lists.

DPIS route fixes should prefer this order:

1. Express the behavior as a scheduler or field policy.
2. Document the route boundary and evidence.
3. Add a package list only when a reusable policy cannot express the behavior.
4. Add a new independent route only when the existing route model cannot safely
   represent the behavior.

## Resources Font Event Gate

The app-process `resources_font` route may see two different runtime meanings
for the same target factor:

- Compose evidence can show that Resources has already applied the target
  factor, so Compose-heavy roots should read the base font scale to avoid
  double scaling.
- Resources read-path conflict evidence can show the same Resources owner
  alternating between base and target font scales. Once that event is observed,
  the read path should stabilize to the target font scale.

Read-path conflict evidence has higher priority than Compose base suppression:

```text
read-conflict target suppression
  > compose base suppression
  > observed-only state
```

Negative Compose observations may clear Compose base suppression, but must not
clear an already established read-conflict target suppression for the same
package and target factor.

In compat / field-rewrite font mode, the automatic domain set does not include
`resources_font`. Users can still enable it manually from the custom hook-chain
font page when an app needs read-path `fontScale` / `scaledDensity` values to
match the target. When enabled in compat mode, `resources_font` uses a mixed
Resources route:

- `ResourcesImpl.updateConfiguration` is installed as a low-frequency metrics
  seed so `scaledDensity` can be initialized before hot resource reads.
- `Resources.getConfiguration()` and `Resources.getDisplayMetrics()` remain
  the read-side fallback and event-gate observation points.
- When the plan installs `ResourcesRead` only for `resources_font`, the read
  path skips viewport target resolution, runtime marker reads, and
  `VirtualDisplayState` reuse. It may still keep `DisplayMetrics.densityDpi` /
  `density` synchronized with the current configuration before applying the
  font `scaledDensity`.
- In system font emulation, `ResourcesRead(getConfiguration)` does not force
  the target `fontScale` on every read. The target font scale is owned by the
  system/ActivityThread/Resources write and seed routes; read-side
  configuration writes remain a compat `resources_font` fallback.
- In system font emulation, `ResourcesRead(getDisplayMetrics)` may still fill
  `DisplayMetrics.scaledDensity` from the target font factor. It must not use a
  lower system `Configuration.fontScale` or Compose base suppression to
  downgrade metrics that already reached the target.
- Compose runtime diagnostics may detach their layout listener after the
  package/target reaches read-conflict target suppression; at that point the
  Resources route has already proven the target-stabilization event.
- `ResourcesManager` write-side hooks stay owned by viewport routes and system
  font emulation. Compat `resources_font` should not install them by itself.

## Documentation Rules

- Public route semantics belong here or in the runtime resync documents.
- Raw logs, screenshots, device paths, and app-specific investigation notes
  belong under `docs/private/` and must stay uncommitted unless intentionally
  sanitized.
- When changing font runtime routes, update this document if the mode tree or
  ownership boundary changes.
