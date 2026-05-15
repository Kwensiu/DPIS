# Companion Display Tool Compose Phase 2 Design

## Goal

Add a Compose-focused phase to the companion display tool design so DPIS validation can observe Jetpack Compose text behavior through the same stable package, control, and log contract established in phase 1.

Phase 2 focuses first on Compose font-chain observation. Layout and density observations are included only where they help interpret text results.

## Positioning

Compose phase 2 is an additive scene group inside the existing companion display tool app.

It must:
- reuse the stable package `io.github.kwensiu.dpis.displaytool`
- reuse the existing adb broadcast control surface
- reuse the phase 1 run boundary and scene-event log prefix
- keep scene ids stable and `snake_case`
- stay weakly aware of DPIS validation needs without reading DPIS config

It must not:
- replace phase 1 native View/XML scenes
- introduce WebView behavior
- introduce fragile variants in the first Compose pass
- add file export, provider queries, network upload, or a generic command interpreter
- turn the tool app into a DPIS settings or diagnostics UI

## Scope

### In Scope

- Jetpack Compose text scenes only.
- Four first-pass Compose scenes:
  - `compose_baseline_text`
  - `compose_nested_scroll_text`
  - `compose_lazy_list_text`
  - `compose_styled_text`
- `normal` variant only.
- Cold-start automatic Compose sampling for:
  - `compose_baseline_text`
  - `compose_lazy_list_text`
- Explicit adb-triggered execution for all four Compose scenes.
- Compose-specific log fields appended after the phase 1 common scene-event prefix.
- First-pass Compose fields focused on text output and text measurement.
- Reserved naming and grouping space for WebView, without implementing WebView.

### Out Of Scope

- WebView scenes.
- Compose fragile variants.
- Compose animation, gesture, input, Material component matrix, or custom drawing.
- Screenshot/image analysis.
- Strict pass/fail assertions.
- File export or ContentProvider report APIs.
- Any dependency on DPIS private config or DPIS module internals.

## Relationship To Phase 1

Phase 2 extends phase 1; it does not change phase 1 semantics.

The existing native scene group remains the baseline for:
- Android `Configuration`
- `DisplayMetrics`
- View `TextView` text size
- XML/resource text paths
- phase 1 anomaly classes

The Compose scene group adds target-app-side evidence for:
- Compose `Text` measuring and rendering
- Compose density and font scale values observed from composition locals
- Compose text layout results
- lazy item composition and reuse behavior
- text style inheritance inside Compose

Phase 2 logs must remain comparable with phase 1 logs. Agents should be able to parse both groups by reading the shared prefix first, then consuming surface-specific extensions.

## Architecture Direction

Phase 2 should add a Compose scene layer beside the existing native scene layer.

Recommended boundaries:
- `SceneRegistry` keeps phase/group metadata and stable scene ids.
- A Compose scene adapter owns hosting Compose content in a real Android view tree.
- A Compose text probe owns `TextLayoutResult` capture and Compose-specific field extraction.
- The existing run orchestrator continues to own run ordering, active-run rejection, and sentinel logs.
- The existing log formatter remains the only place that emits `DPIS_TEST` lines.

Compose scenes should be hosted through a `ComposeView` or equivalent Android view host that participates in normal measure/layout. This preserves the existing app shape: the orchestrator attaches a scene view, waits for a meaningful checkpoint, then logs.

## Scene Group

Phase 2 introduces a scene group concept for organization, but not a new command transport.

Recommended group id:

```text
compose
```

Scene ids remain globally unique:

```text
compose_baseline_text
compose_nested_scroll_text
compose_lazy_list_text
compose_styled_text
```

The group id may be logged as an optional extension field such as `surface=compose` after the common prefix. It should not replace the stable `scene` field.

## Runtime Model

### Cold Start

Cold start should keep the tool fast and deterministic.

Required Compose automatic cold-start matrix:

```text
compose_baseline_text normal
compose_lazy_list_text normal
```

These two scenes provide the highest first-pass value:
- baseline Compose text calibration
- lazy list composition/reuse path

Cold start should not automatically run all Compose scenes in the first pass. Native phase 1 already provides a broad startup baseline; Compose phase 2 should add focused evidence without making startup noisy.

This is the only automatic Compose execution path in this design. A normal app launch should run the existing phase 1 native cold-start matrix and then the two-scene Compose cold-start subset above, using the same run lifecycle and sentinel behavior.

### run_all

The existing phase 1 `run_all` behavior should remain unchanged unless a later design explicitly introduces a phase or group selector.

For this design, plain `action=run_all` remains phase 1 native-only and must not silently add Compose scenes. Compose execution outside normal app launch is explicit.

If implementation needs an adb way to rerun the Compose cold-start subset, use the existing `run_all` action with a flat group extra:

```text
action=run_all group=compose
```

`action=run_all group=compose` means exactly the two-scene Compose cold-start subset:

```text
compose_baseline_text normal
compose_lazy_list_text normal
```

It must not run all four Compose scenes. The other Compose scenes remain explicit `run_scene` targets.

### run_scene

`run_scene` must support all four Compose scenes with `variant=normal`.

Unsupported variants must be rejected with the same structured rejection behavior used in phase 1.

Examples:

```text
action=run_scene scene=compose_baseline_text variant=normal
action=run_scene scene=compose_lazy_list_text variant=normal
```

### show_scene

`show_scene` may navigate the visible UI to a Compose scene detail host. Showing a scene should not redefine scene ids or emit run logs unless the existing UI behavior already samples on show.

If show-time sampling is added later, it must be a meaningful checkpoint and must use the same scene-event prefix.

## Variant Model

The first Compose pass supports only:

```text
variant=normal
```

No Compose fragile variants are designed in this phase.

Reasons:
- Compose text measurement has enough independent behavior to validate before adding adversarial cases.
- Phase 1 fragile cases already cover several repeated-scaling patterns.
- Adding fragile Compose scenarios too early would make it harder to distinguish Compose pipeline behavior from intentionally stressed behavior.

Future Compose fragile variants may be designed after normal results are stable on device.

## Compose Scene Definitions

### compose_baseline_text

Purpose:
- Establish a simple Compose `Text` baseline.
- Capture how Compose reads density/font scale and how `TextLayoutResult` reports measured text.

Content:
- One primary `Text`.
- Stable base text size, for example `14.sp`.
- No custom font family, animation, dynamic state, or Material dependency requirement.

Checkpoint:
- First completed text layout.

Expected evidence:
- Compose local density.
- Compose local font scale if available through the chosen Compose API.
- Requested text size in sp.
- Text layout size.
- Line count.
- Rendered or layout-derived text px.

### compose_nested_scroll_text

Purpose:
- Observe text through nested Compose containers and scrolling boundaries.

Content:
- A vertical scroll container.
- At least one nested child container before the primary `Text`.
- One primary `Text` with stable `14.sp` sizing.

Checkpoint:
- First completed text layout after composition and measure.

Expected evidence:
- Same text fields as baseline.
- Container measured size.
- Whether scroll container was present as a stable scene attribute.

### compose_lazy_list_text

Purpose:
- Observe lazy composition and item reuse/recomposition behavior for text.

Content:
- A `LazyColumn`.
- A stable first-screen item set.
- Primary log target should be a deterministic item, such as row index `0`.

Checkpoint:
- First visible item text layout after lazy list settles.

Mechanical settle rule:
- the logged row is `item_index=0`
- `lazy_first_visible_index` must be `0`
- the row must have produced a `TextLayoutResult`
- logging should occur after the next frame callback following that text layout result
- only one `compose_lazy_first_screen_stable` event is emitted per run for this scene

Expected evidence:
- Same text fields as baseline.
- `lazy_first_visible_index` if available without noisy observation.
- `item_index` for the logged row.
- A stable event name such as `compose_lazy_first_screen_stable`.

### compose_styled_text

Purpose:
- Observe Compose text style inheritance and explicit style application.

Content:
- A parent style provider or local text style.
- A primary `Text` that receives style through Compose style resolution.
- Stable `14.sp` effective base size.

Checkpoint:
- First completed text layout.

Expected evidence:
- Same text fields as baseline.
- Requested style size if available.
- Resolved text size used for layout.

## Logging Contract

### Shared Prefix

Compose scene events must reuse the phase 1 scene-event prefix exactly:

```text
stage
run_id
scene
variant
event
pkg
font_scale
density_dpi
scaled_density
width_dp
height_dp
```

The prefix remains Android resource/configuration evidence. Compose-specific fields come after this prefix.

### Compose-Specific Fields

First-pass Compose fields should prioritize text results:

```text
surface=compose
compose_density=<float>
compose_font_scale=<float>
compose_text_sp=<float>
compose_text_px=<float>
compose_line_count=<int>
compose_layout_w=<int>
compose_layout_h=<int>
compose_rendered_scale=<float>
```

Field semantics:
- `surface`: literal value `compose`
- `compose_density`: `Density.density` observed inside composition
- `compose_font_scale`: font scale observed from Compose density APIs inside composition
- `compose_text_sp`: requested effective text size in sp for the logged `Text`
- `compose_text_px`: text size in px used by Compose layout for the logged `Text`
- `compose_line_count`: line count from `TextLayoutResult`
- `compose_layout_w`: width in px from `TextLayoutResult.size.width`
- `compose_layout_h`: height in px from `TextLayoutResult.size.height`
- `compose_rendered_scale`: `compose_text_px / (compose_text_sp * scaled_density)`

`scaled_density` in the denominator is the Android `DisplayMetrics.scaledDensity` already emitted in the shared prefix. This keeps Compose rendered scale comparable with phase 1 native text logs.

Optional scene-specific fields:

```text
item_index=<int>
lazy_first_visible_index=<int>
style_source=<value>
container=<value>
```

Field rules:
- Keep all values ASCII and shell-safe.
- Omit unavailable optional fields rather than writing `null`, `n/a`, or `unknown`.
- Keep field order stable for Compose scene events.
- Add Compose fields only after the shared prefix.
- Do not emit JSON or nested blobs.

### Event Names

Recommended event names:

```text
compose_first_text_layout
compose_lazy_first_screen_stable
```

Use `compose_first_text_layout` for:
- `compose_baseline_text`
- `compose_nested_scroll_text`
- `compose_styled_text`

Use `compose_lazy_first_screen_stable` for:
- `compose_lazy_list_text`

### Anomaly Hints

Phase 2 should reuse the existing anomaly vocabulary:

```text
double_scale
no_scale
inconsistent_readings
```

For Compose, `inconsistent_readings` should be considered when at least two reading paths disagree, for example:
- Android `Configuration` and Compose local font scale disagree.
- Android `DisplayMetrics` and Compose local density disagree.
- Compose requested text size and Compose text layout result disagree beyond tolerance.
- Compose text result and phase 1 native TextView result disagree under the same DPIS replacement state.

The first Compose pass should avoid strict pass/fail assertions. It should emit evidence and lightweight hints.

## Control Surface

The existing `am broadcast` control surface remains the only automation entrypoint.

Required support:
- `run_scene` for each Compose scene with `variant=normal`
- `show_scene` for each Compose scene
- `dump_summary` includes latest run summary, regardless of whether it was native or Compose
- `reset_state` clears transient run state

No generic command interpreter should be added.

If a group selector is added later, it should be a flat extra:

```text
group=compose
```

It must not change existing phase 1 behavior unless explicitly requested.

## UI Direction

The UI should stay simple.

Recommended additions:
- A Compose scene group in the existing scene list.
- Detail navigation for each Compose scene.
- A small visible text sample for orientation.
- No large on-screen metrics wall.

The UI is not the primary report channel. Logcat remains authoritative.

## WebView Reserved Space

WebView is not part of this phase.

Reservation only:
- Keep the scene/group model open to a future `webview` group.
- Do not name Compose abstractions in a way that assumes only native and Compose surfaces exist.
- Do not add WebView dependencies, scenes, controls, logs, or UI rows in this phase.

## Testing And Verification Strategy

Design-time expectations for the implementation plan:

- Unit tests should lock scene registration order and supported variants.
- Unit tests should lock Compose log field order where formatting is pure Java/Kotlin.
- Source or build tests should verify that Compose scenes do not support `fragile` in the first pass.
- Device smoke should verify:
  - cold start emits only the selected Compose automatic subset when Compose cold-start sampling is enabled
  - each of the four Compose scenes runs through `run_scene`
  - scene logs preserve the shared prefix
  - Compose-specific fields are present after the prefix
  - run boundaries remain parseable

Device verification should compare evidence under the same DPIS replacement configuration used for phase 1, such as effective 500 dp density and 300 percent rendered font scale.

## Success Criteria

Compose phase 2 first pass is successful when:

- The app still installs as `io.github.kwensiu.dpis.displaytool`.
- Phase 1 native behavior remains unchanged.
- Compose dependencies and scenes are additive.
- `compose_baseline_text` and `compose_lazy_list_text` can be sampled automatically at cold start without noisy loops.
- All four Compose scenes can be run explicitly through `am broadcast`.
- Each Compose scene emits a single-line ASCII `DPIS_TEST` scene event with the phase 1 prefix and Compose-specific text fields.
- Normal-only variant support is enforced.
- WebView remains unimplemented.

## Open Implementation Notes

- Prefer a small Compose scene adapter rather than embedding Compose handling directly in the run orchestrator.
- Keep Compose measurement capture close to the Composable that owns `TextLayoutResult`.
- Avoid using Material components unless needed for text behavior; plain Compose foundation/ui text is enough for the first pass.
- If implementation language choices arise, keep the module maintainable within the existing Java-first repository while using Kotlin only where Compose requires it.
- Do not change the established package name, adb action, or phase 1 scene ids.
