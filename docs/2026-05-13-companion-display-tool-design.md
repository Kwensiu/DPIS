# Companion Display Tool Design

## Goal

Define a companion Android tool app that acts as a stable LSPosed/DPIS test target for display and font validation, so regression testing no longer depends on arbitrary third-party apps.

## Positioning

The companion app is:
- A standalone Android app module in this repository.
- A long-lived tool app, not a one-off debug demo.
- Not an Xposed module.
- Not a DPIS product feature module.
- Weakly aware of DPIS-style hook validation needs, but not coupled to DPIS APIs or config storage.

The companion app exists to:
- Render controlled UI scenes.
- Emit agent-friendly runtime logs.
- Offer simple adb-triggerable control points.

The companion app does not exist to:
- Replace DPIS diagnostics inside the main app.
- Export files or act as a general automation platform in phase 1.
- Embed DPIS logic, scope management, or hook-specific settings.

## Why This Exists

Current validation depends too much on unrelated apps, which introduces noise:
- App updates change behavior unexpectedly.
- Different UI frameworks hide whether a failure comes from DPIS or the target app itself.
- Reproducing font propagation bugs becomes slow and inconsistent.

A controlled tool app provides:
- Stable package name and scene ids.
- Repeatable cold-start test order.
- Controlled fragile scenarios for double-scaling and no-scaling boundaries.
- A cleaner bridge between device logs and agent analysis.

## Scope

### In Scope for Phase 1

- A new standalone Android app module in this repository.
- Native Android View/XML/Resources scenarios only.
- Cold-start automatic execution of a fixed core scene set.
- Compact UI with limited on-screen data.
- Structured single-line logcat output as the primary report surface.
- adb-triggerable fixed actions.
- Lightweight anomaly hints focused on font propagation problems.

### Out of Scope for Phase 1

- Compose, WebView, Flutter, Unity, Canvas-heavy custom rendering.
- File export.
- ContentProvider report queries.
- Network upload.
- Exhaustive pass/fail assertion machinery.
- Deep manual-only exploratory UI.

## Phase Model

### Phase 1: Native Android Base Chain

Focus:
- `Configuration`
- `DisplayMetrics`
- `dp/sp`
- XML/View text rendering
- layout propagation
- font scaling propagation
- common container and window boundaries

Primary value:
- Establish a stable baseline for most Android UI display behavior.
- Catch double-scaling, no-scaling, and inconsistent reading paths early.

### Phase 2: Secondary Framework and Host Surfaces

Candidate scope:
- Jetpack Compose
- WebView
- additional windowing boundaries if needed

Why phase 2 is separate:
- These surfaces often have their own measurement and text pipelines.
- They should build on top of phase 1 logging and control conventions instead of changing them.

Expected carry-over from phase 1:
- same package stability
- same run model
- same scene id and variant conventions
- same flat log format
- same adb action pattern

### Phase 3: Special or Non-Standard Rendering Chains

Candidate scope:
- Flutter
- custom Canvas text and draw pipelines
- game-like or engine-driven rendering
- framework-specific native bridges

Why phase 3 is separate:
- Failures here are more likely to mix framework behavior with DPIS behavior.
- Investigation often requires specialized probes, not just ordinary View/UI logging.

Expected direction:
- Preserve the same reporting shell.
- Allow framework-specific scene groups and specialized anomaly reasons.
- Keep phase 3 additive rather than rewriting the earlier model.

## Chosen Architecture Direction

Build a separate app module in the same repository, with a stable package name and a narrow responsibility:
- host controlled scenes
- run a deterministic startup sequence
- emit deterministic logs
- respond to a minimal adb command set

This is preferred over:
- adding everything into the main DPIS app as a debug-only page
- depending on third-party target apps
- splitting the tool into a separate repository

## Runtime Model

Phase 1 uses a hybrid startup model:
- All phase 1 content is registered at app startup.
- Core scenes are truly attached and laid out during cold start.
- Secondary phase 1 surfaces may be pre-sampled without becoming the main UI.
- Entering a page later may trigger extra sampling, but initial baseline data must not depend on manual navigation.

Core-scene cold-start attach does not require every scene to be first-screen visible.
It is acceptable to attach core scenes inside a real but non-primary host container, as long as:
- they are part of a real view tree
- they complete real measure and layout
- they still map to stable scene detail navigation later

The cold-start run must:
1. start once
2. execute all core scenes in fixed order
3. emit structured logs at fixed checkpoints
4. stop after one run

No endless loop and no unbounded background spam.

Phase 1 permits only one active run at a time.
If a new run request arrives while another run is active, the implementation should reject the new request and emit a structured rejection log rather than silently restarting.

For phase 1, the default cold-start automatic run and `run_all` command execute the same required matrix:
- all six core scenes
- `normal` variant only

`fragile` variants are phase 1 required surfaces, but they are executed through explicit control requests such as `run_scene` and later targeted regression flows rather than the default cold-start sweep.

## Core Scene Set

Phase 1 cold-start core scenes are:

1. `baseline_text_sp`
2. `nested_scroll_text`
3. `recycler_text_bind`
4. `dialog_text_sp`
5. `styled_text_appearance`
6. `programmatic_text_px`

These six scenes balance coverage and maintenance cost:
- baseline text for calibration
- nested structure for propagation bubbling
- recycler reuse for repeated apply bugs
- dialog/window boundary checks
- style inheritance checks
- programmatic sizing path checks

## Variant Model

Scene id and variant are separate.

- `scene` stays stable, for example `nested_scroll_text`
- `variant` is one of `normal` or `fragile`

Phase 1 fragile coverage is intentionally limited:
- `baseline_text_sp` -> `normal`
- `nested_scroll_text` -> `normal`, `fragile`
- `recycler_text_bind` -> `normal`, `fragile`
- `dialog_text_sp` -> `normal`
- `styled_text_appearance` -> `normal`, `fragile`
- `programmatic_text_px` -> `normal`, `fragile`

This keeps the model expandable without doubling every scene immediately.

## Fragile Scenario Intent

Fragile variants are not random breakage. They are controlled scenarios that help expose:
- repeated font multiplier application
- mixed px/sp conversion paths
- parent and child both compensating for font scale
- adapter rebinding that applies scale cumulatively
- style and runtime text sizing conflicts

Fragile variants should stay readable and deterministic. They are not fuzz tests.

## UI Design Direction

The UI should be intentionally simple.

Requirements:
- group list plus overview and detail navigation
- no large data wall on first screen
- enough visible output to confirm the current scene and a few key readings
- stable layout that does not hide its own measurement bugs with excessive chrome

The UI is for orientation, not the primary report channel.
The primary report channel is logcat.

## Logging Contract

### Primary Rule

Logs are optimized for agents first, humans second.

Requirements:
- single line
- ASCII only
- flat key=value pairs
- stable field order
- no JSON
- no nested blobs
- avoid quoting and shell-hostile punctuation when possible

Field handling rules:
- required fields must always be present
- optional fields may be omitted when not applicable
- phase 1 should prefer omission over placeholder text such as `null`, `n/a`, or `unknown`
- field order must remain stable for a given event type
- scene-specific extensions are allowed only after the common required field prefix

Example direction:

```text
DPIS_TEST stage=phase1 run_id=1747123456789_01 scene=nested_scroll_text variant=fragile event=first_layout pkg=com.example.displaytool font_scale=1.30 density_dpi=560 scaled_density=4.55 width_dp=360 height_dp=780 view=text_primary text_px=58.0 base_sp=14 line_count=2 measured_w=312 measured_h=96 suspicious=true suspicious_reason=double_scale
```

### Sentinel Events

Each automatic run must emit:
- `event=run_start`
- `event=run_end`

Both events must carry:
- `run_id`
- `stage`
- summary-safe identifying fields

`run_end` should also include a compact summary such as:
- total scenes
- total suspicious events
- total errors if any

Minimum `run_start` field order:
- `stage`
- `run_id`
- `event`
- `trigger`
- `scene_total`
- `variant_mode`
- `pkg`

Minimum `run_end` field order:
- `stage`
- `run_id`
- `event`
- `trigger`
- `scene_total`
- `scene_completed`
- `suspicious_total`
- `error_total`
- `pkg`

For phase 1:
- `trigger` identifies the source such as `cold_start` or `adb`
- `variant_mode` indicates the matrix that ran, such as `normal_only`

### Scene Events

Phase 1 should log only meaningful checkpoints, such as:
- activity created or resumed
- first layout complete
- dialog shown
- recycler first screen stable
- post-rotation or equivalent re-entry checkpoints if later needed

Avoid noisy logs for every minor scroll or redraw.

Minimum scene-event field order:
- `stage`
- `run_id`
- `scene`
- `variant`
- `event`
- `pkg`
- `font_scale`
- `density_dpi`
- `scaled_density`
- `width_dp`
- `height_dp`

Recommended common optional fields after the required prefix:
- `view`
- `text_px`
- `base_sp`
- `line_count`
- `measured_w`
- `measured_h`
- `suspicious`
- `suspicious_reason`

If a scene event is rejected, skipped, or fails, the same prefix should be preserved as far as applicable before adding error fields.

## Anomaly Hints

Phase 1 uses lightweight anomaly hints instead of strict assertions.

Initial anomaly classes:
- `double_scale`
- `no_scale`
- `inconsistent_readings`

These hints exist to speed up triage. They do not replace raw field output.

Minimum evidence expectations:
- `double_scale` must be based on a mismatch between expected scaling input and rendered or applied text result, not on visual intuition alone
- `no_scale` must be based on a lack of expected change across runtime readings or rendered text output
- `inconsistent_readings` must be based on disagreement between at least two reading paths, such as configuration, display metrics, or rendered view metrics

## adb Control Surface

Phase 1 adb control actions are intentionally fixed and small:

1. `run_all`
2. `run_scene`
3. `show_scene`
4. `dump_summary`
5. `reset_state`

Notes:
- Do not implement a generic command interpreter in phase 1.
- Scene id and variant can be parameters rather than new action names.
- File export and provider queries remain future options, not current requirements.

Phase 1 should use one control transport for deterministic automation:
- `am broadcast`

Recommended command contract:
- one package-owned action namespace
- one broadcast receiver entrypoint
- flat string extras only where practical

Recommended extras:
- `action`
- `scene`
- `variant`
- `trigger`

Control expectations:
- `run_all` starts a new run only when no active run exists
- `run_scene` targets exactly one scene and one variant
- `show_scene` changes visible UI focus without redefining scene ids
- `dump_summary` emits a structured summary log for the latest known run
- `reset_state` clears tool-app-side transient run state and emits a structured reset log

## Package and Naming Rules

Requirements:
- stable independent package name
- stable scene ids
- `snake_case` ids for scene names
- no version markers in scene ids
- no UI copy embedded in ids
- no debug or flavor-specific `applicationIdSuffix` for the installed target package used in hook validation

Good examples:
- `baseline_text_sp`
- `recycler_text_bind`
- `styled_text_appearance`

Bad examples:
- `scene1`
- `fontTestA`
- `phase1_page2_item3`

## Interaction with DPIS

The tool app should remain weakly aware of the surrounding validation ecosystem.

Allowed:
- scene design that is useful for DPIS and LSPosed validation
- logs that align cleanly with existing DPIS probe analysis
- package stability for hook targeting

Not allowed:
- reading DPIS private config directly
- embedding DPIS business logic
- becoming a control UI for DPIS

## Relationship to Existing DPIS Diagnostics

DPIS already emits hook-side diagnostics such as:
- resources probe logs
- view root probe logs
- window/session probe logs
- system_server display probe logs

The companion tool app should complement these logs by providing target-app-side evidence:
- what the target app reads
- what the target app renders
- where a suspicious scaling path appears

This separation keeps the tool useful even if the hook implementation changes.

## Success Criteria for Phase 1

Phase 1 is successful when:
- the tool app installs and runs as a standalone target app
- a cold start produces one complete deterministic run
- the six core scenes emit stable logs
- agent-side log collection can detect run boundaries reliably
- normal and fragile variants provide enough evidence to distinguish likely double-scale and no-scale cases
- UI remains simple and does not become the main dependency for diagnosis

## Follow-On Work After Phase 1

After phase 1 lands, the next decision is not "what to invent next" but "which surface should join the same contract next."

Recommended order:
1. stabilize phase 1 scene and log contract on device
2. add phase 2 scene groups for Compose and WebView
3. only then add phase 3 framework-specific chains such as Flutter

The critical rule is:
- new phases extend the same control and reporting model
- they do not replace it

## Open Implementation Notes

These are intentionally not a full execution plan, but they should guide the next planning step:
- add a new app module rather than another source set under the existing module
- keep scene registration separate from scene rendering
- keep logging contract centralized so every scene cannot drift
- keep fragile behavior explicit and named rather than hidden in ad hoc code paths
- keep startup orchestration deterministic and testable
