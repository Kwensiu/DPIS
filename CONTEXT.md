# DPIS Context

This document records DPIS domain language for agents and maintainers. It is not
a user guide and does not replace `README.md`.

## Product Boundary

DPIS is an LSPosed/Xposed module for per-app display and font behavior. A change
should stay package-scoped unless the code is explicitly part of `system_server`
route installation, runtime property recovery, or debug tooling.

## Viewport Model

`ViewportTargetSpec` is the authoritative viewport target representation.

- `relative_scale`: interface scale, stored as permille. User-facing range is
  30-300%.
- `absolute_dp`: fixed smallest-width target in dp.
- `off`: no viewport target.

Do not treat `width_dp` as the universal source of truth. It is a legacy value
and an adapter value for fixed-width targets.

Viewport target type and viewport apply strategy are separate concepts:

- Target type answers what to apply: relative scale or fixed width.
- Apply strategy answers where to apply it: auto, system, compat, or off.

## Apply Strategies

Viewport apply strategies:

- `auto`: use the system route when system hooks are available; otherwise
  fall back to the compat route.
- `system`: system-server route only.
- `compat`: app-process compatibility route only.
- `off`: disabled.

Legacy persisted values still exist:

- `system_emulation` normalizes to `system`.
- `field_rewrite` normalizes to `compat`.

New code should use `ViewportApplyMode.AUTO`, `SYSTEM`, `COMPAT`, and `OFF`
unless it is explicitly testing legacy compatibility.

## Runtime Routes

- `docs/modern-runtime-resync.md` and `docs/legacy-runtime-resync.md`
  are the DPIS living route documents for viewport/runtime hook routes.
- `docs/private/` contains app-specific investigation notes (not committed).

Before changing viewport/runtime hook routes, read the relevant document.
For shared code under `app/src/main/java/com/dpis/module/`, read both. Any new
route exploration, route detail adjustment, failed attempt, or runtime finding
must be recorded in the appropriate document. Do not delete historical
experiments unless they are duplicated or misleading; mark them as inactive,
superseded, or rejected with evidence.

System route:

- Runs in `system_server`.
- Mutates package-scoped launch/config/display data.
- Is the first choice for `auto` when system hooks are available.
- In safe mode, keep only package-gated low-risk entries. `launch-activity-item`
  is needed for apps whose activity-start path does not expose a mutable
  configuration.

Compat route:

- Runs inside the target app process.
- Uses Resources/DisplayMetrics compatibility hooks.
- Can fix apps not reached by system route, but has higher risk of layout or
  process instability.

If `auto` falls back to compat, verify whether the system route is unavailable
or ineffective before treating the compat path as a regression.

## Runtime Properties

Runtime properties mirror per-app configuration for cross-process readers and
Legacy runtime fallback.

Viewport properties carry target type, active target value, compat config value,
and apply mode. Relative scale may legitimately have `vp=0` or `vpcfg=0`; check
`vptype` and `vpscale` before assuming the target is off.

Persistent `persist.debug.dpis.*` properties are fallbacks. Network, update, and
manifest decisions must not depend on cached runtime properties.

## Font Model

Font scale, typeface replacement, and hook domains are distinct concerns.

- Font scale changes `Configuration.fontScale` or app-process text-size paths.
- Typeface replacement changes font files or system-family selection.
- Hook domains select which font compatibility hooks are enabled per app.

Imported TTF, OTF, and TTC files are standard font-library formats. TTC import
inspects the collection and registers every loadable face; the app configuration
sheet owns the later face selection. Each face's saved `ttcIndex` is part of its
identity and must be preserved through catalog recovery, archive import/export,
and runtime loading. TTC support is not an experimental preference or a runtime
gate. Catalog labels should use the face's embedded family/style name, with an
index-based label only as a parsing fallback.

A TTC collection alias is independent from its face labels. The font-library
list, collection detail title, rename action, and archive metadata use the
alias; the app configuration sheet uses embedded face labels when selecting a
typeface. Renaming a collection must not rewrite its individual face labels.

Do not fold typeface replacement into font scale mode. Do not reintroduce old
global Flutter/HyperOS font switches; per-app hook domains own that decision.
Custom font hook-chain overrides apply only to compat/field-rewrite font mode.
`system_server_font` and `activity_thread_font` are internal scheduler domains,
not user-customizable chain switches. See `docs/font-routing.md`.

### Font Storage Ownership

The font catalog and imported font files are local-only user data. The catalog
is stored in the dedicated `font_library` preference store and is owned solely
by `FontLibraryStore`; imported files are owned by that same store under the
app-private font directory.

`dpi_config` is for package configuration and its Legacy XML mirror only. Do
not add `font.library.*` keys to `DpisConfigStore`, remote preference snapshots,
backup payloads, runtime delivery, or whole-store mirror/replace logic. The
only permitted cross-store operation is the one-time upgrade migration from the
old `dpi_config` catalog into `font_library`, after which the old catalog key is
removed.

When adding persistent state, first state its owner, authoritative source, and
whether it may cross a process boundary. A writer must only replace data in its
own store; cross-store migration must be explicit, one-way, and regression
tested against unrelated-store loss.

## App Config Sheet

The app config sheet stores viewport scale and width drafts independently. The
active target type decides which draft is effective; the inactive draft must not
be cleared by saving the other mode.

UI labels may intentionally hide internal route names. For example, app-list
status uses one `Interface` segment for both relative scale and fixed width, with
the value showing `%` or `dp`.

## Log Page And Feedback Diagnostics

The log page and feedback diagnostic flow are user-facing support tools, not
temporary debug-only surfaces. They exist to collect enough DPIS-specific
evidence for support without asking users to export unrelated LSPosed logs.

App-specific runtime routes are first-class diagnostic evidence for their target
apps. For example, WeChat DPI belongs to `route=wechat_dpi`; diagnostics should
expose hook install evidence, callback evidence, mutation evidence, and skipped
or failed locator evidence for WeChat. A configured WeChat DPI target makes
callback and mutation evidence expected; without that config, callback or
mutation hits are treated as unexpected route activity.

Global log output is the feature gate for both the log page and feedback
diagnostics. If log output is disabled, the UI should offer an enable path
instead of silently producing empty diagnostics.

Feedback diagnostics start by restarting the target app. The user reproduces the
issue, returns to DPIS, and DPIS ends collection immediately. Packaging should
happen at that return point, before the result sheet appears. Save and share
actions should use the already-built package bytes instead of rebuilding.

Diagnostic package file roles are distinct:

- `diagnostic.txt` is the DPIS semantic analysis entry point. It should include
  the manifest, app config, diagnostic plan, runtime summary, runtime density,
  runtime anomalies, the complete `runtime-timeline`, runtime self-test status,
  and raw-log file references.
- `dpis-log.txt` is DPIS app-process log storage from `DpisAppLogStore`. Prefer
  entries from the diagnostic window. If none match but recent DPIS app logs
  exist, use a small, clearly labeled recent fallback instead of exporting days
  of history.
- `lsposed-log.txt` is raw LSPosed evidence from `modules_*.log` and
  `verbose_*.log`. It is for parser and hook-evidence cross-checking, not the
  primary user-facing summary.

High-frequency runtime events are not noise by default. For jank, crash, and
hook-loop analysis, the complete runtime flow is evidence for what DPIS actually
did. Add summary, density, and anomaly sections before the full flow; do not
replace the full timeline with summaries unless the timeline becomes too large
to handle safely.

## Template And Prefill Summaries

Global prefill and quick templates summarize saved custom defaults. Editor
defaults must not be shown explicitly unless a rule below says they are part of
a saved custom value.

- Empty defaults show only the empty state text.
- Saved numeric values always show, even when the value is close to a platform
  default.
- Default target type `relative_scale`, default viewport strategy `auto`, and
  default font route `system_emulation` do not show by themselves.
- Fixed-width target type is custom. Fixed-width empty values show
  `Interface No value · Min width` / `界面 空数值 · 宽度`.
- Explicit viewport `system` or `compat` strategy is custom and may show with or
  without a numeric viewport value. `auto` does not add `Auto` to a summary.
- Explicit font `field_rewrite` route is custom and may show with or without a
  numeric font value. `system_emulation` does not show by itself.
- Independent capabilities such as custom hook-chain state and typeface
  selection should appear as their own summary chip/part instead of being folded
  into default mode text.

## Package Config State

`configured` is overloaded and must not be used as a single canonical concept
for package state.

When discussing package state, qualify which meaning is intended. At minimum,
separate user-visible configuration from other stored package state such as
draft-only values, runtime-target state, or preserved package-local preferences.

`User-visible configured package` is the package state represented by the home
workspace configured-apps card and by the Configured Apps list. These two UI
surfaces must use the same inclusion rule.

A package is user-visible configured when it has any saved user-preserved
package-level state. This includes numeric viewport or font values, mode-only
state, target-type-only state, hook-domain-only state, app-specific config such
as WeChat DPI, explicit per-package `dpisEnabled=false` overrides, and saved
configuration for apps that are no longer installed.
Installed packages in the known active LSPosed scope are also user-visible
configured for the home configured-apps card and Configured Apps list, even
when they have no saved package-level values. Unknown scope state, such as
Legacy builds without a scope service, must not be inferred as injected. This
is UI state only; it must not create stored package config by itself.

Draft-only state is transient app-config-sheet state. It is not a user-visible
configured package unless the user saves it as package-level state.

Long-term package configuration storage should converge on one package-aggregated
source of truth. Package UI, backup/restore, runtime snapshots, and counting
rules should read from that aggregated package model rather than from scattered
per-key indexes.

The aggregated package model should be keyed by package name, not stored as an
ordered package array. DPIS is per-app configuration; package name is the
natural identity and lookup key.

`target_packages` is legacy migration evidence only. New package storage should
not keep writing `target_packages` as a live index or source of truth.

Legacy migration should preserve contradictory or incomplete old per-key state
as explicit package-local residual or draft-only state instead of silently
dropping it. Preserved residual state is evidence for cleanup and diagnostics;
it must not automatically become a runtime target.

Mode-only, target-type-only, and hook-domain-only package state are
user-preserved package preferences, not residual state. This includes viewport
target type only, viewport apply mode only, font mode only, and hook-domain-only
package state.

Configured-app list status should show concrete effective values when present.
If a package is configured only by mode or target-type preferences and has no
displayable viewport, font, typeface, hook-domain, app-specific, or disabled
value, show the configured/injected scope state plus an empty-value status.
When one dimension has an effective value and another dimension only has a
mode preference, show the effective value and omit the no-value mode-only
dimension from the compact status text.
Use `已注入 | 空数值` / `Injected | No value` for configured installed entries
with no displayable numeric value.

Configured packages that are not currently installed should still appear in the
Configured Apps list. They should be visually distinguished from installed apps
with an unavailable/uninstalled state, remain fully editable, and remain
clearable. They should not appear in the All Apps list.
For configured but uninstalled packages, prefer expressing the uninstalled
state directly in the compact status text instead of adding a separate badge:
`未安装 | 空数值` / `Not installed | No value` when there is no displayable
numeric value.

The package-aggregated model may store minimal display metadata such as the
last known app label so uninstalled configured packages remain understandable.
Current install state should still be derived from the package manager when
available, not treated as a cached storage fact.

Optional package-specific config blocks should only exist when the package has
that config. For example, WeChat DPI belongs in an optional app-specific block;
packages without app-specific settings should not store or show an empty
app-specific section.

The package-aggregated model should be sparse: store only blocks and values
that are present for a package. Do not create fixed empty viewport, font,
hook-domain, app-specific, or metadata blocks for every package.

## Debugging Checklist

Before changing route logic:

- Confirm the exact package name and Java hash used by runtime properties.
- Check target type, target value, and apply mode separately.
- Check whether system hooks and system safe mode are enabled.
- Confirm whether the target app process is in LSPosed scope before expecting
  compat hooks to load.
- Treat `hook ready` as installation evidence only; require an apply log,
  callback, mutation, process metric, or visible result before calling a route
  effective.

Runtime evidence rules:

- For LSPosed diagnostics, `/data/adb/lspd/log/modules_*.log` and
  `verbose_*.log` are the primary evidence for module entry and hook execution.
  Plain `logcat` can cross-check forwarded `LSPosedFramework` lines, but absence
  in plain `logcat` is not a reliable negative signal.
- `docs/private/` may contain app-specific investigation notes, but public route
  documents should record reusable conclusions and safe reproduction boundaries,
  not private device paths, tokens, screenshots, or app-specific raw logs.
- Failed route experiments are evidence. Do not delete them unless they are
  duplicated or misleading; mark them inactive, superseded, or rejected with the
  evidence that changed the conclusion.
- Do not add reproduction-target-specific runtime behavior unless a general
  route, scheduler, or field-policy fix has been ruled out. Package lists are a
  late fallback; independent hook routes are later still.

When a system-route app does not respond:

- Inspect `system_server` load time after installing a new module build.
- Reboot or restart `system_server` before judging changes to hook installation.
- Look for launch/config/display apply evidence, not only app-process logs.
- Avoid package-specific runtime exceptions unless a general route fix is ruled
  out.
