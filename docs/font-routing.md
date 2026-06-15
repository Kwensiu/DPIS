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

For compat / field-rewrite font mode, `resources_font` uses a mixed Resources
route:

- `ResourcesImpl.updateConfiguration` is installed as a low-frequency metrics
  seed so `scaledDensity` can be initialized before hot resource reads.
- `Resources.getConfiguration()` and `Resources.getDisplayMetrics()` remain
  the read-side fallback and event-gate observation points.
- When the plan installs `ResourcesRead` only for `resources_font`, the read
  path skips viewport target resolution, runtime marker reads, and
  `VirtualDisplayState` reuse. It may still keep `DisplayMetrics.densityDpi` /
  `density` synchronized with the current configuration before applying the
  font `scaledDensity`.
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
