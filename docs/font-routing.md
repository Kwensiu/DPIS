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

## Imported Typeface Transport

Typeface replacement is independent from font-scale mode selection. A configured
`FontFace` is loaded once when the target process installs its typeface hooks;
the selected face is then retained for that process lifetime.

TTF, OTF, and TTC are all supported font-library inputs. A TTC is inspected as
a collection: every face that passes preflight is registered, with its embedded
family/style name and original collection index. The app configuration sheet
owns face selection. An invalid selected face must leave the target app's
original Typeface in place; it must not fall back to face zero. This is stable
font-library behavior, not a Laboratory feature.

Collection aliases are collection metadata, not face names. They identify the
imported file in library management and archives; face selection always uses
the embedded face family/style label.

- The primary transport is the exported DPIS font provider. It opens DPIS-private
  font files as read-only, seekable descriptors after proving that the Binder
  caller owns a package currently configured for that exact face.
- Provider authorization reads the per-app `typefaceId` configuration source;
  it must not use catalog metadata as an authorization fallback. The catalog is
  local-only data in the dedicated `font_library` preference store. Older
  catalog data in `dpi_config` is a one-time migration source only and is
  removed after a successful copy.
- The provider exposes one face URI only. It must not expose catalog enumeration,
  arbitrary paths, or write operations.
- The root-published `/data/local/tmp` copy is a fallback for Provider failure,
  not the catalog authority. Private catalog storage remains authoritative. A
  failed cleanup of that optional copy must be recorded, but must not prevent
  the user from deleting the private font and its catalog entry.
- Both Modern and Legacy use the same descriptor/file loader and pass the TTC
  index. A failed requested TTC face keeps the app's original Typeface; it must
  not silently use face zero.
- Saving a different face affects the next target process creation. Do not add
  cross-process config reads to TextView or Paint hot paths.
- Feedback diagnostics record the typeface route as stable boundaries:
  `source_provider_loaded`, `source_fallback_loaded`, `hook_installed`,
  `replacement_hit`, and `load_failed`. Loading and replacement events reuse
  the existing per-process log deduplication, so TextView/Paint hot paths do
  not repeatedly append diagnostic records.

## Resources Font Event Gate

## Synchronous Field Mutation Scheduling

Compat field-rewrite routes keep `TextView`, `Paint`, and `TextPaint` setters
synchronous because callers must observe the target size before layout and draw
continue. Their shared mutation scheduler is a decision boundary, not a
background executor:

- an object whose current size is already the recorded target keeps that value
  when the same recorded base size is submitted again;
- this `kept` outcome is exported separately from `skipped`, so a diagnostic
  can measure duplicate setter calls removed by the scheduler;
- an external size drift, factor change, or new base size re-enters normal
  arbitration and may write a new target;
- a stronger TextView/resource provenance remains observationally authoritative
  for the Paint fallback;
- the keep decision avoids a redundant native setter. It does not suppress a
  genuinely new user/app size or defer a required mutation.

This boundary exists because the runtime evidence from comment-heavy screens
showed repeated Paint fallback applications and TextView setter reinforcement.
The optimization targets duplicate synchronous writes and layout invalidation;
it does not claim that every remaining frame delay is caused by DPIS.

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

## Diagnostic Evidence Semantics

Font route performance aggregates distinguish three outcomes:

- `applied`: the route synchronously changed the framework object.
- `kept`: the route owns the current target value and avoided a redundant
  setter call.
- `skipped`: the callback was observed, but the route yielded to a stronger
  provenance owner or could not establish a safe target.

`kept` remains full-fidelity in the performance aggregate, but is not emitted
as one timeline/transport event per callback because it performs no mutation;
this keeps repeated setter observations from becoming diagnostic work on the
target thread.

TextView target-state cache entries carry the factor that produced the target.
An entry is eligible for `kept` only when that factor still matches the active
factor; a factor change therefore re-enters normal arbitration instead of
reusing an old target size. Current-size drift and a new base size remain
value-based invalidation boundaries, while the weak-key map still lets dead
TextViews be collected.

The `textview_current_px_fallback` route follows the same distinction. A
known target-sized `TextView` is `kept`; a `Resources` / SP / absolute route
that owns the value remains `skipped` for the fallback route. This prevents
the aggregate from understating scheduler hits or treating route arbitration
as a redundant-write optimization.

Paint mutation counters and latency measurements remain full-fidelity during
diagnostics. Caller stack text is sampled per Paint type and input-size bucket
to keep repeated stack formatting and transport payloads from becoming a
second source of jank; the sample is evidence for attribution, not the source
of the aggregate counts.

## Documentation Rules

- Public route semantics belong here or in the runtime resync documents.
- Raw logs, screenshots, device paths, and app-specific investigation notes
  belong under `docs/private/` and must stay uncommitted unless intentionally
  sanitized.
- When changing font runtime routes, update this document if the mode tree or
  ownership boundary changes.
