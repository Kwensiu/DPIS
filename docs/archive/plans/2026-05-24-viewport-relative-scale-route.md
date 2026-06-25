# Viewport Relative Scale Route Draft

> This is a pre-implementation route draft for grilling and decision making.
> It is not the final task-by-task execution plan yet. The final plan should be
> produced only after the decision questions at the end are resolved.

## Goal

Change DPIS viewport behavior from an absolute `smallestScreenWidthDp` target
being the primary mental model to a screen-relative layout scale being the
primary mental model, so the same app configuration behaves consistently across
foldable inner screens, foldable outer screens, rotation, and different device
sizes.

## Current Diagnosis

The current viewport path stores and resolves one absolute width per package:

- `DpiConfigStore`: `viewport.<package>.width_dp`
- `TargetViewportWidthResolver`: resolves that integer as the effective target.
- `ViewportOverride` and `VirtualDisplayOverride`: derive density and logical dp
  fields from `targetWidthDp / sourceSmallestWidthDp`.
- `ResourcesImplHookInstaller`, `ResourcesManagerHookInstaller`,
  `ResourcesReadHookInstaller`, and `SystemServerDisplayEnvironmentInstaller`
  consume the same absolute target value from different runtime entry points.

This makes the current behavior deterministic but too rigid. If a foldable inner
screen is around `850dp` and the user configures `900dp`, DPIS does the intended
small adjustment on the inner screen. When the app moves to an outer screen
around `411dp`, the same absolute target still forces the app to see `900dp`,
which is the issue reported in GitHub issue 64.

The formula itself is not the only problem. Relative scale is not idempotent if
it is recomputed from a configuration that DPIS already modified. The route must
therefore separate source observation, target resolution, and result reuse.

## Proposed Product Semantics

The primary viewport control becomes a relative layout scale:

- `100%`: preserve the current device/window display environment.
- `106%`: make the app see about 6% more dp than the current complete display
  environment exposes.
- `90%`: make the app see about 10% less dp than the current complete display
  environment exposes.

For the issue 64 example:

- Inner screen: `850dp * 106% = 901dp`
- Outer screen: `411dp * 106% = 436dp`

This preserves the user's intended "slightly smaller / denser layout" effect on
the inner screen without turning the outer screen into a forced tablet layout.

Absolute dp can either be removed from the primary UX or retained as an advanced
mode. If retained, its semantic name should be explicit: "force smallest width
dp", not "display scale".

`relative_scale` and `absolute_dp` are mutually exclusive for one package. They
are two target semantics for the same final field: the effective target
`smallestScreenWidthDp`. Applying both would require an arbitrary ordering rule
and could cause double scaling. If DPIS later needs a two-step behavior, it
should be modeled as a separate semantic such as "relative scale with min/max
clamp", not as simultaneous scale plus absolute override.

Target semantics are independent from hook application mode. `relative_scale`
and `absolute_dp` choose the target. Existing viewport apply modes choose where
DPIS applies that target:

- `auto`: default user-facing apply strategy. Prefer system-server/app-process
  cooperation when available, otherwise fall back to app-process compatibility
  rewriting.
- `system_emulation`: explicit advanced strategy. System-server/app-process
  cooperation, disabled when
  system-server hooks are disabled.
- `field_rewrite`: explicit advanced strategy. App-process compatibility
  rewrite path.

Relative scale should not require `system_emulation` in principle. It can run in
`field_rewrite` when the app process sees a source snapshot that DPIS can treat
as fresh. "Fresh" has a narrow meaning: DPIS can prove it is not reusing a DPIS
result from the same target spec. App-process compatibility mode cannot prove
that no other Xposed module has already changed the framework
`Configuration`; that case is outside DPIS' self-idempotency guarantee unless a
future earlier source hook is added. System mode is still expected to be more
complete for apps that read system-server display or window sources early.

`auto` is a persisted UI/config value, not a value that rewrites itself to
`system_emulation` or `field_rewrite`. Runtime code resolves an effective route
silently:

- DPIS should not depend on reading LSPosed scope state to know whether the user
  selected the `android` / system scope. Target app processes generally cannot
  rely on such a direct signal.
- If system-server hooks are available and produce a viewport result, they also
  publish a compact `ViewportRuntimeRecord` marker for that package.
- App-process hooks consume that marker. When the current configuration matches
  the marker's source or result signature, they reuse the marker's result rather
  than deriving relative scale again.
- If no matching system-origin marker is observable for the current source,
  app-process hooks take over with the compatibility rewrite path.
- System-server relative-scale mutation must be coupled to marker publication.
  In `auto`, if the marker cannot be published, system-server should skip the
  viewport mutation and leave app-process compat as the fallback. This avoids
  the worst case where app-process sees an already-modified configuration with
  no positive signal.

This makes missing system scope behave as a silent fallback to compat without
changing the user's visible `auto` selection.

The two routes should be merged as providers into one runtime pipeline rather
than treated as two independent features:

1. Build a `ViewportSourceSnapshot`.
2. Resolve a `ViewportTargetSpec` to one effective target.
3. Publish or reuse a fingerprinted `ViewportRuntimeRecord` with provenance
   (`system_server` or `app_process`).
4. Apply the same derived result through the current hook entry.

System-server hooks are an optional early producer. App-process hooks are the
mandatory per-target-app repair/fallback layer. Auto therefore does not need a
perfect "system scope selected" detector; it only needs a positive marker when
system-server actually produced a result. Missing marker means "no reusable
system result", not "LSPosed scope is disabled".

The primary UI should not present "system" and "compat" as the main viewport
choice. It should present the target semantic choice: "relative scale" or "fixed
dp". The hook apply strategy is an advanced setting with `auto` as the default.

## Core Architecture

### 1. Introduce `ViewportTargetSpec`

Replace the runtime habit of passing `Integer targetViewportWidthDp` through the
system with an explicit target specification.

Expected fields:

- `mode`: `relative_scale`, `absolute_dp`, or `off`
- `scalePermille`: integer scale where `1000` means 100%
- `absoluteWidthDp`: only meaningful in `absolute_dp`

Expected responsibilities:

- Normalize invalid values.
- Provide stable equality/hash behavior for state matching.
- Keep storage and runtime property compatibility explicit.
- Keep `relative_scale` and `absolute_dp` structurally separate. The absolute dp
  code path should continue to resolve a fixed effective target, while the
  relative scale code path should resolve from a trusted source snapshot. Shared
  code should begin only after both modes have produced an effective target.
- Enforce one viewport target semantic per package at a time. The UI and runtime
  config should choose `relative_scale` or `absolute_dp`, never both.

### 2. Introduce `ViewportSourceSnapshot`

Create one source object that describes the unmodified or trusted runtime display
environment.

Expected fields:

- `screenWidthDp`
- `screenHeightDp`
- `smallestWidthDp`
- `densityDpi`
- `widthPx`
- `heightPx`
- `scope`: `display`, `window`, or `unknown`
- `trustedPixels`: whether `widthPx/heightPx` came from a trustworthy source
- `origin`: `system_display_info`, `system_configuration`, `resources_impl`,
  `resources_manager`, or `resources_read`

Expected responsibilities:

- Reject invalid source values early.
- Detect already-applied DPIS results.
- Prevent read hooks from accidentally becoming fresh source producers.

`scope` is not a replacement for `ViewportConfigurationScope`. The existing
`ViewportConfigurationScope.isWindowScoped()` logic remains the Android
framework detector for `Configuration` objects. `ViewportSourceSnapshot` wraps
that answer into a data object and adds origin/trust metadata:

- System-server `DisplayInfo` snapshots are display-scoped and pixel-trusted
  when `logicalWidth`, `logicalHeight`, and `logicalDensityDpi` are present.
- App-process `ResourcesImpl.updateConfiguration` snapshots are display-scoped
  only when `ViewportConfigurationScope.isWindowScoped(config)` is false and
  metrics pixels are trusted.
- `ResourcesManager` snapshots are configuration candidates, but do not publish
  pixel state unless a trusted pixel source is attached.
- `ResourcesRead` snapshots are repair-only by default. They may match or reuse
  an existing record, but they should not publish a fresh relative baseline.
- Window-scoped snapshots can borrow a display record. They never create the
  relative-scale baseline.

Freshness/trust rules:

- A snapshot is self-fresh when it does not match any DPIS runtime result
  signature for the same package and target spec.
- A snapshot that matches an existing record's result signature is
  already-applied. It can be repaired from that record, but must not be used as
  a new relative-scale baseline.
- A snapshot with no marker and no process-local record is treated as the
  platform source. This is safe only because system-server relative mutation is
  not allowed to run without marker publication. DPIS should not invent a
  heuristic that guesses "this looks already scaled" from dp values alone.
- DPIS does not claim to detect arbitrary third-party rewrites. If another
  module mutates the configuration before DPIS sees it and no original source is
  available, the first implementation treats that mutated value as the platform
  source unless it matches DPIS' own fingerprints.

### 3. Introduce `ViewportTargetResolver`

Resolve a `ViewportTargetSpec` against a `ViewportSourceSnapshot`.

Rules:

- `relative_scale`: `targetSmallestWidthDp = round(source.smallestWidthDp *
  scalePermille / 1000.0)`
- `absolute_dp`: `targetSmallestWidthDp = absoluteWidthDp`
- `off`: no viewport override
- Window-scoped sources do not create a fresh relative target by default.
- Invalid or unknown sources return no viewport target rather than guessing.
- Rounding uses `Math.round((source.smallestWidthDp * scalePermille) / 1000.0f)`
  and clamps the final target to at least `1`.
- The rounded effective target is stored in the runtime record together with the
  source signature. Later hooks reuse that integer result instead of
  re-rounding from a possibly modified `Configuration`. Rotation and fold/unfold
  create a new source signature and a new rounded target; repeated callbacks for
  the same source do not accumulate rounding drift.

### 4. Keep `ViewportOverride` As The Low-Level Result Deriver

`ViewportOverride` should keep doing the mechanical conversion from source config
plus effective target smallest width to:

- target `screenWidthDp`
- target `screenHeightDp`
- target `smallestScreenWidthDp`
- target `densityDpi`

The change is that callers no longer pass a raw user-configured dp value. They
pass a resolved effective target from `ViewportTargetResolver`.

### 5. Replace Single-Value `VirtualDisplayState` With Runtime Records

Current `VirtualDisplayState` has one process-wide result. That is risky for
foldables, multi-display, and repeated hook paths.

Expected replacement model:

- Store records keyed by package, target spec fingerprint, source signature,
  and scope class. A small bounded map is enough; it only needs the current and
  recently observed display environments for one process.
- Each record contains:
  - `packageName`
  - `targetSpecFingerprint`: target type plus active value
  - `sourceSignature`: source width/height/smallest dp, density dpi, trusted px,
    and scope
  - `effectiveTargetSmallestWidthDp`
  - `viewportResult`: logical dp and density result
  - `virtualDisplayResult`: pixel-capable result when trusted px exists
  - `resultSignature`: result width/height/smallest dp and density dpi
  - `provenance`: `system_server` or `app_process`
  - `createdElapsedRealtime`
- "Signed" in earlier notes means this structural fingerprint, not
  cryptographic signing. The record is valid only when the current target spec
  fingerprint and either the source signature or result signature match.
- Publish only from trusted display-scoped paths.
- Allow window-scoped paths to borrow a stable display record only when the
  record belongs to the same package and target spec, and the window source is
  not being used as the baseline.
- Provide a positive already-applied match so read hooks can reuse the result
  without multiplying scale again.
- Existing app-process `VirtualDisplayState` can become the process-local record
  cache. A system-server record requires an explicit transport, most likely
  hashed system properties next to the existing viewport property bridge.

This is the critical safety step for relative scale.

Runtime marker transport:

- Process-local records handle repeated app-process callbacks.
- System-server and target app processes do not share Java static memory. Any
  system-origin record that app-process should observe must be transported
  explicitly.
- The first implementation should extend `ViewportPropertyBridge` with compact
  runtime record properties, separate from persisted user config properties.
  These properties should carry at least target spec fingerprint, source
  signature, effective target, result signature, and provenance.
- A marker with a mismatched package hash, target spec fingerprint, or expired
  timestamp is ignored.
- If property transport is unavailable in system-server, `auto` should not apply
  relative-scale mutation in system-server. It should leave app-process compat
  to compute from the first trusted app-process source.

## Hook Strategy

### App Process

`ResourcesImplHookInstaller` should become the primary app-process source path
when it has a complete `Configuration` and trusted `DisplayMetrics` pixels.

Expected behavior:

- Build `ViewportSourceSnapshot` from the original config and metrics.
- First try to match an imported system-origin marker or a process-local record.
  If the config already matches a record result, reuse that record and repair
  metrics/config without resolving relative scale again.
- If scope is display, source is trusted, and the snapshot is self-fresh, resolve
  the target and publish a process-local display record.
- Apply the result to `Configuration` if viewport mode allows configuration
  override.
- Update `DisplayMetrics` density/scaledDensity and pixel values from the record
  result when available.

`ResourcesManagerHookInstaller` should be a consistency path, not a primary
publisher when pixels are unavailable.

Expected behavior:

- Build a source snapshot when possible.
- Apply viewport config if a reliable effective target can be resolved.
- Avoid publishing virtual display state from configuration-only paths with no
  trusted pixels.
- In relative mode, configuration-only paths should prefer record reuse over
  fresh derivation unless the snapshot is clearly self-fresh.

`ResourcesReadHookInstaller` should be a read repair path.

Expected behavior:

- If a returned config already matches a runtime record, reuse that record for
  metrics.
- If the config is unmodified and a trusted record exists for the same
  source signature, repair the read result.
- Do not compute a new relative target from an already-overridden config.

`DisplayHookInstaller`, `WindowMetricsHookInstaller`, and `ViewRootProbeHookInstaller`
should consume runtime records only. They should not compute relative scale
targets.

### System Server

`SystemServerDisplayEnvironmentInstaller` should switch from carrying
`targetViewportWidthDp` to carrying `ViewportTargetSpec`.

Expected behavior:

- `DisplayInfo.logicalWidth/logicalHeight/logicalDensityDpi` remains the most
  useful system-server source for a complete display environment.
- Build a `ViewportSourceSnapshot` from `DisplayInfo`.
- Resolve relative scale from that source.
- Publish a system-origin runtime marker before or with applying the derived
  environment. If the marker cannot be published in `auto` relative mode, skip
  the system-server viewport mutation and let app-process compat be the first
  producer.
- Apply the derived environment to package-scoped system-server paths only after
  the marker requirement is satisfied.
- Do not special-case foldable posture if current display information already
  gives the correct active screen size.
- Keep explicit fold-state APIs as a reserved fallback only. Do not introduce
  `DeviceStateManager` or Jetpack WindowManager in the first implementation.

In explicit `system` strategy, failure to publish a marker should be treated as
a diagnostic failure and logged. The conservative default is still to skip
relative mutation rather than creating an unmarked already-modified source that
app-process could scale again.

## Storage And Runtime Property Route

Primary SharedPreferences storage should move from a single width key to explicit
target fields.

Proposed keys:

- `viewport.<package>.target_type`
- `viewport.<package>.scale_permille`
- `viewport.<package>.width_dp`

Compatibility route:

- If `width_dp` exists and `target_type` is absent, migration can convert it to
  advanced `absolute_dp`, preserving old behavior.
- Newly edited or newly created viewport settings should default to
  `relative_scale`.
- Runtime properties need an equivalent target type and scale value if system
  server and compat100 fallback must continue to work across process restarts.

Runtime marker properties are separate from user target config:

- Target config properties answer "what does the user want for this package?"
- Runtime marker properties answer "has a concrete source already been resolved
  to this concrete result?"

The marker should be compact and generated by a single serializer so it stays
maintainable. A practical shape is a delimited value with a version prefix:

`v1|pkgHash|targetFp|sourceSig|effectiveSwDp|resultSig|provenance|elapsedMs`

The exact encoding can be refined during implementation, but these constraints
are fixed:

- Include a version so future marker changes do not require heuristic parsing.
- Include the package hash used by the existing property bridge.
- Include a target fingerprint so changing percent/dp mode invalidates old
  markers immediately.
- Include both source and result signatures so app-process can distinguish
  self-fresh sources from already-applied results.
- Keep it runtime-only; do not persist markers as user configuration.
- Treat parse failure, version mismatch, package mismatch, target mismatch, or
  stale timestamp as marker miss.

Decision: preserve existing `width_dp` values as the advanced `absolute_dp`
semantic. Do not silently convert old values to relative scale.

Migration must be centralized and versioned. Do not scatter compatibility
behavior across getters, UI save handlers, and runtime property readers.

Expected migration structure:

- Add a dedicated migration coordinator for viewport target/apply-strategy keys.
- Track a schema version key such as `viewport.schema_version`.
- Make every migration idempotent and monotonic.
- Keep legacy readers small and temporary: they may interpret old keys only to
  feed the migration or compatibility fallback, not as the long-term primary
  model.
- Unit-test each migration from a concrete old key set to the new key set.
- Preserve old users' explicit apply mode:
  - old `system_emulation` -> new explicit `system`
  - old `field_rewrite` -> new explicit `compat`
- Preserve old users' implicit apply mode:
  - old `width_dp` with no mode key currently behaves as `system_emulation`, so
    migrate it to new explicit `system`
  - packages with no viewport configuration default to `auto` only when they are
    newly configured
- Preserve old `width_dp` as active `absolute_dp`.
- Newly created or newly edited viewport settings default to active
  `relative_scale`.

## UI Route

Primary app configuration UI should expose relative scale as the normal control.

Expected shape:

- One shared input control with a mode button on the right.
- The mode button toggles between `relative_scale` and `absolute_dp`.
- Toggling the mode switches the displayed input value to that mode's stored
  value. Only the active mode is effective at runtime.
- For `relative_scale`, the input is a percentage centered at `100%`.
- For `absolute_dp`, the input is an integer dp value.
- Practical initial range: `50%` to `200%`.
- Fine adjustment step: initially `1%`.
- Future precision option: one decimal place can be supported by storing
  `scalePermille` directly, for example `106.5%` as `1065`. The first
  implementation can keep whole-percent UI while leaving storage compatible with
  decimal precision.
- Display preview text can show the resolved approximate target for the current
  screen, but the saved value is the percent, not the resolved dp.

Absolute dp should remain available through the mode toggle. It should be
visually secondary or carry helper text that it forces one target across all
screens. Existing `width_dp` configurations migrate to this mode.

System/compat should not occupy this same primary toggle. They belong to an
advanced "apply strategy" setting:

- `auto`: default; prefer system when viable and fall back to compat when system
  hooks are unavailable or explicitly disabled.
- `system`: force system emulation, mainly for diagnostics.
- `compat`: force field rewrite, mainly for diagnostics or system-hook
  avoidance.

Apply strategy remains explicitly selectable. `auto` is only the default for
apps that have no prior viewport target/apply-mode configuration. Existing
configured apps keep their current explicit `system_emulation` or
`field_rewrite` value. Existing `width_dp`-only apps keep the old implicit
`system_emulation` behavior as new explicit `system`. They are not silently
rewritten to `auto`.

For packages configured as `auto`, do not persist an automatic switch to system
or compat after runtime resolution. The selected UI value remains `auto`; only
the effective route changes in memory. Existing packages that already have a
viewport target are not auto-converted to `auto`; their old explicit or implicit
apply mode is preserved.

Potential UI locations:

- App config sheet advanced area: easier to reach during per-app setup, but less
  semantically precise and depends on users discovering the sheet can be pulled
  upward.
- A generalized hook configuration panel opened from the existing "Hook chain"
  button: semantically cleaner and extensible, but deeper. A dedicated page with
  only the viewport apply strategy may feel sparse unless grouped with other hook
  chain controls.

Decision: first implementation places apply strategy in a new page/tab inside
the hook-chain configuration panel. Do not duplicate it in the app config sheet.
This keeps the setting semantically tied to hook behavior and avoids implying it
is part of the primary dp/scale or font value.

The hook-chain panel page should edit only the apply strategy. Do not show a
target-value summary and do not allow editing relative scale or absolute dp
there. This avoids creating a second mental model or duplicate editing surface
for the viewport target.

## Small Window Policy

Small windows must not become the baseline for relative scale. The baseline is
the complete active display environment. The small-window question is only what
DPIS should do when a window-scoped configuration is encountered before or after
a display-scoped source has been observed.

Proposed rule:

- Display-scoped source: compute and publish relative target.
- Window-scoped source with matching runtime record: reuse the complete
  display environment's stable state to stay consistent.
- Window-scoped source without matching runtime record: no viewport override.
- Unknown scope: conservative behavior; prefer no publish, and only apply if the
  source is clearly complete and trusted.

This keeps floating windows, split screen, and embedded windows from being
treated as separate physical screens.

Decision: first implementation should reuse the complete display environment's
stable state for small/window-scoped configurations when such state exists.
Small-window-specific behavior needs a separate follow-up discussion.

Important constraint: this is not a cosmetic follow-up. `deriveWindowScoped()`
and `ViewportConfigurationScope.isWindowScoped()` are already in the current
viewport path, so the first relative-scale implementation must preserve the
existing window-scoped guard and add regression tests around it. The deferred
part is product tuning for small-window behavior, not the safety rule that small
windows are never used as the relative baseline.

## Testing Route

Testing should start before the final task-by-task execution plan is locked,
but only for the runtime mechanism that can still invalidate the architecture.
The recommended order is:

1. Run a marker-transport spike first.
2. If the spike proves cross-process marker publication/reading is viable, write
   the final implementation plan around that transport.
3. If the spike fails, update this route with the replacement transport and
   only then produce the final implementation plan.

This is intentionally narrower than implementing the feature first. The target
model, resolver, migration, and UI behavior are ordinary code paths that can be
planned with unit tests. The risky part is whether the system-server producer
can publish a runtime marker that the target app process can reliably observe
without reading LSPosed scope state.

Marker-transport spike:

- Add or use a debug-only probe path; do not ship product behavior from the
  spike.
- From the system-server hook context, attempt to write a compact marker payload
  using the proposed transport.
- From the target app-process hook context, attempt to read the marker and log
  parse success/failure, payload length, package hash, target fingerprint,
  source signature, result signature, provenance, and timestamp age.
- Test at least one normal app process with system scope enabled.
- Test the same app with system scope unavailable or marker publication
  intentionally disabled and confirm app-process code sees a clean marker miss.
- Confirm failure is distinguishable from stale, package-mismatched,
  target-mismatched, malformed, and too-long markers.
- Confirm marker miss does not trigger any attempt to infer "already applied"
  from dp values.

Go/no-go criteria:

- Go: system-server can publish the marker before or together with mutation, the
  target app process can read it, malformed/stale/mismatched markers are
  rejected deterministically, and the payload fits the chosen transport's
  limits with margin.
- No-go: the marker cannot be written from system-server, cannot be read by the
  target app process, cannot carry the required fields, or produces ambiguous
  misses. In this case, do not enable system-server relative mode; choose a new
  transport or make app-process compat the only first implementation route.

Marker-transport spike result:

- Decision: Go. Android system properties are viable as the first runtime
  marker transport.
- Runtime marker property: `debug.dpis.vprtm.<packageHash>`.
- Validated payload shape:
  `v1|pkgHash|targetFp|sourceSig|effectiveSwDp|resultSig|provenance|elapsedMs`.
- Observed payload length: 43 characters in the tested app, below Android's
  91-character system property value limit.
- With LSPosed system scope disabled while DPIS' own system-hook setting stayed
  enabled, app-process hooks observed a clean marker miss with `reason=empty`.
  No system-server publish log appeared, and the app-process compatibility path
  still rewrote viewport configuration.
- With LSPosed system scope enabled, system-server published
  `published=true`, and app-process hooks observed `result=hit` with
  `provenance=s`.
- Safe-mode system-server mutation currently needs pre-proceed marker
  publication because the active entry point is `activity-start`.
- The spike currently fingerprints the absolute target adapter
  (`a<base36-dp>`). Production relative-scale work must generalize target
  fingerprints to include target type and active value, for example
  `r<base36-permille>` and `a<base36-dp>`.
- The marker remains runtime-only. It is separate from user target config
  properties and must not be treated as persisted user state.

Unit tests should lock the behavior before runtime validation:

- `ViewportTargetSpecTest`: normalization and equality.
- `ViewportTargetResolverTest`: relative and absolute target resolution.
- `ViewportOverrideTest`: keep existing absolute math stable once effective
  target is resolved.
- `VirtualDisplayStateTest`: runtime record matching, already-applied detection,
  foldable screen switch behavior.
- `ResourcesImplHookInstallerTest`: no double-application on read/update loops.
- `ResourcesManagerHookInstallerTest`: configuration-only paths do not publish
  pixel state.
- `SystemServerDisplayEnvironmentInstallerTest`: DisplayInfo-derived relative
  targets for inner/outer screen examples.
- `ViewportRuntimeRecordTest`: source/result fingerprint matching,
  mismatched-target rejection, expiry handling, and provenance.
- `ViewportRuntimeMarkerBridgeTest`: property encode/decode, corrupted marker
  rejection, package/target mismatch rejection, and payload-length validation
  against Android system property limits.
- `ViewportSourceSnapshotTest`: wraps `ViewportConfigurationScope`, classifies
  display/window/unknown sources, and rejects read-hook sources as fresh
  relative baselines.

Issue 64 regression fixture:

- Inner source: `smallestWidthDp=850`, target scale `1060`, expected effective
  target around `901dp`.
- Outer source: `smallestWidthDp=411`, same target scale, expected effective
  target around `436dp`.
- Assert outer never resolves to `900dp` in relative mode.

Runtime validation should collect DPIS logs that include:

- source snapshot
- target spec
- effective target
- scope
- whether the result was newly published, reused, or skipped
- marker provenance and marker match/miss reason

## Implementation Phases

### Phase 1: Target Model And Resolver

Add `ViewportTargetSpec`, `ViewportSourceSnapshot`, and `ViewportTargetResolver`
with focused unit tests. No hook behavior changes yet.

### Phase 2: Runtime Display State

Replace single-value `VirtualDisplayState` APIs with runtime record
publish/reuse APIs while preserving existing absolute behavior through an
effective target adapter.

This phase should define route provenance (`system_server` vs `app_process`) and
already-applied detection before any relative-scale hook migration. It is the
safety layer that lets `auto` silently fall back without reading LSPosed scope
state.

This phase must also define the system-origin marker bridge. Without that bridge
the plan cannot safely enable relative-scale mutation in system-server `auto`
mode.

Phase 2 has an explicit go/no-go check: prove the chosen marker transport can be
written by the system-server hook context and read by the target app-process
context with enough payload for the required fields. If Android system property
limits are too tight, replace the transport before Phase 4 rather than weakening
the marker requirement.

### Phase 3: App-Process Hook Migration

Update `ResourcesImplHookInstaller`, `ResourcesManagerHookInstaller`,
`ResourcesReadHookInstaller`, and display/window consumers to use the new target
and runtime record APIs.

### Phase 4: System-Server Hook Migration

Update `PerAppDisplayConfig`, `PerAppDisplayConfigSource`,
`PerAppDisplayOverrideCalculator`, and `SystemServerDisplayEnvironmentInstaller`
to carry `ViewportTargetSpec`, resolve relative targets from trusted
system-server sources, and publish system-origin runtime markers before applying
relative mutations in `auto`.

### Phase 5: Storage, Properties, And UI

Add storage keys, migration behavior, runtime property publication, and UI
controls after the runtime model is stable.

This phase must create or update a centralized migration class before adding UI
save logic. The migration tests should be written before the store changes so
future updates do not gradually disturb legacy compatibility behavior.

### Phase 6: Device Validation And Cleanup

Run unit tests, build both flavors, and validate on:

- normal phone app
- rotated normal phone app
- small window or split screen
- available foldable evidence from issue 64 logs or reporter feedback

Remove temporary probes or keep them behind the existing debug/log switches.

## Must-Ask Decision Questions

### Question 1: Migration Semantics

When an existing package has `viewport.<package>.width_dp` and no target type,
what should DPIS do?

Recommended answer: migrate it to `absolute_dp` first, then make the new UI
default to `relative_scale` for newly edited values. This avoids silently
changing installed users' behavior while still shifting the product direction.

Decision: use the recommended answer. Existing width values remain as
`absolute_dp`; new and edited values use `relative_scale` by default. The
absolute target path and relative target path must stay separate until they
produce an effective target smallest width.

### Question 2: Absolute Mode Fate

Should absolute dp remain available as an advanced mode?

Recommended answer: keep it, but hide it behind an advanced switch and label it
as "force smallest width dp". Some users intentionally want tablet layout on a
phone or a specific app, and relative scale cannot express that cleanly.

Constraint: even if it remains visible, absolute dp is a target type choice, not
an additional option layered on top of relative scale.

Decision: keep it visible as the second state of the primary viewport target
toggle. Use one shared input field; switching target type switches the displayed
stored value. Only the active target type is effective.

### Question 2A: System/Compat UI Fate

Should current system/compat viewport mode remain a primary user-facing switch?

Recommended answer: no. Move it to an advanced apply strategy with `auto` as the
default. The primary viewport switch should be `relative_scale` vs `absolute_dp`.

Decision: use the recommended answer. System/compat are bottom-layer application
strategies, not target semantics. The default strategy is `auto`.

Refinement: system/compat/auto remain explicitly selectable, but move to a more
appropriate configuration location. `auto` is the default only for newly
configured apps without existing viewport target/apply-mode state. It must not
overwrite old users' explicit or implicit system/compat choices.

Decision: use the hook-chain configuration panel as that location, with a new
page/tab for viewport apply strategy.

### Question 3: Relative Scale Range

What range should the UI allow?

Recommended answer: start with `50%` to `200%`, step `1%`, because real-world
needs are not known yet and the range can be narrowed later if validation shows
too many broken layouts. Use `scalePermille` internally so one-decimal UI
precision can be added without a storage migration.

Decision: use `50%` to `200%` initially. One-decimal input is not required for
the first implementation, but the storage model must use integer permille so
one-decimal precision can be enabled later without migration.

### Question 4: Small Window Policy

What should DPIS do when a small/floating/window-scoped configuration is
encountered?

Recommended answer: never use the small window as a fresh baseline. Display-
scoped sources publish state; window-scoped sources can borrow matching stable
state or skip viewport override.

Decision: use the recommended answer, with reuse preferred when a matching
display-scoped stable state exists. Do not derive a fresh relative target from
the small window.

### Question 5: Fold State API

Should DPIS add explicit fold-state detection with `DeviceStateManager` or
Jetpack WindowManager?

Recommended answer: not in the first implementation. Use active display and
configuration metrics as the source of truth. Fold state is posture metadata,
not the actual app-visible display environment.

Decision: keep this as a reserved option. Revisit only if the relative-scale
implementation cannot reliably distinguish active display environments through
`Configuration` / `DisplayInfo`.

### Question 6: Configuration Mode Names

Should persisted mode names stay close to existing viewport naming, or should
they be renamed to the product wording?

Recommended answer: use explicit internal names:

- `relative_scale`
- `absolute_dp`
- `off`

Keep existing `system_emulation` / `field_rewrite` as the hook application mode,
not the target semantic mode.

Decision: use explicit internal target names (`relative_scale`, `absolute_dp`,
`off`) and explicit internal apply-strategy names (`auto`, `system`, `compat`).
Keep legacy `system_emulation` / `field_rewrite` only as migration and
compatibility inputs.

## Current Decision Status

No remaining product-level decision is blocking the implementation plan.

Resolved decisions:

- Existing `width_dp` configurations migrate to active `absolute_dp`.
- Existing explicit `system_emulation` / `field_rewrite` migrate to explicit
  `system` / `compat`.
- Existing `width_dp` without a mode migrates to explicit `system`, matching the
  old implicit behavior.
- Newly configured viewport targets default to `relative_scale` and apply
  strategy `auto`.
- Relative scale range starts at `50%` to `200%`, whole-percent UI, permille
  storage.
- `relative_scale` and `absolute_dp` are mutually exclusive target semantics.
- The primary app sheet uses one shared target input with a right-side mode
  toggle.
- Apply strategy is edited only in the hook-chain configuration panel.
- Auto is a persisted config value; runtime route resolution is silent and does
  not rewrite UI config.
- System and app-process routes merge through one fingerprinted runtime record
  pipeline.
- Small/window-scoped configurations reuse complete-display stable state when
  available and never become the relative-scale baseline.
- Explicit fold-state APIs are reserved for later fallback diagnostics.

Deferred follow-up:

- Small-window-specific behavior should be discussed separately after the main
  route is planned.

## Review Resolution Notes

The external review identified real architecture gaps in the earlier draft. The
updated route resolves them as follows:

- App-process `field_rewrite` does not magically get a pristine source. It can
  only derive relative scale from a self-fresh, trusted source or from an
  imported system marker. It does not use value-shape heuristics to guess that
  an unmarked source is already modified; instead system-server relative
  mutation is gated on marker publication.
- `ViewportSourceSnapshot.scope` wraps the existing
  `ViewportConfigurationScope` result for `Configuration` sources; it does not
  replace that detector.
- Rounding is `Math.round(sourceSmallestWidthDp * scalePermille / 1000.0f)`,
  clamped to at least `1`, and the rounded target is stored in the runtime
  record to prevent repeated rounding drift.
- "Signed state" now means a concrete runtime record with structural source,
  target, and result fingerprints. It is not cryptographic and not cross-process
  Java memory.
- Auto fallback is marker-driven. System-server is allowed to be the early
  producer only when it can publish a marker that app-process can observe;
  otherwise app-process compat becomes the first producer.
- Small-window guardrails are part of the first implementation. Only detailed
  product tuning for small windows is deferred.
