# Font Domain Arbitration And Provenance Plan

Date: 2026-05-20

Status: draft for review, with the first TextView/Paint arbitration slice and
Compose/resources scheduling slice implemented.

## Purpose

Define the next step after per-app font hook domains and Paint provenance:
turn the current set of independently-enabled font domains into a coordinated
runtime arbitration model.

The target behavior is not "all enabled domains always write". It is "enabled
domains can observe, but only the domain with the strongest evidence writes for
a given text size event". This keeps the advanced custom chain editor useful
while reducing multi-domain double scaling.

## Problem Statement

The WeChat conversation-list reproduction shows the failure mode clearly:

- App config enables `field_rewrite` at 150%.
- Viewport rewrite makes WeChat run at `sw500dp`.
- Resources font hooks report `fontScale=1.5` and
  `scaledDensity=density * 1.5`.
- The reproduction uses custom hook-domain overrides that enable additional
  TextView/Paint fallback domains beyond the default `field_rewrite` path.
- Some conversation titles become much larger than surrounding preview/date/tab
  text.

The code-level reason is that downstream hooks can see an already-scaled px
value and still treat it as an unscaled base:

```text
resources_font changes fontScale/scaledDensity
  -> TextView.setTextSize(SP) converts through already-scaled metrics
  -> textview_sp_rewrite may multiply the px value again
  -> textview_current_px_fallback may later read getTextSize() and multiply again
  -> paint_text_size_fallback may do the same for custom Paint/TextPaint paths
```

The current provenance guards reduce repeat writes by the same DPIS route, but
they do not yet model cross-domain facts such as "this TextView size already
came from Resources fontScale".

## Goals

- Preserve user control over per-app domains.
- Keep risky domains opt-in.
- Let multiple domains be enabled without forcing all of them to multiply the
  same text size.
- Prefer explicit unit and lifecycle evidence over px-only fallback evidence.
- Make the runtime decision explainable in logs and tests.
- Avoid target-package-specific behavior.

## Non-Goals

- Guarantee correctness for every custom-rendered text path when all domains
  are enabled.
- Infer Android unit semantics from raw Paint px values.
- Delete `PaintTextSizeFallbackHookInstaller` in this step.
- Change viewport arbitration.
- Reintroduce old global Flutter/HyperOS experimental switches.
- Treat Jetpack Compose as equivalent to TextView/Paint. Compose has separate
  runtime semantics and needs its own compatibility path.

## Evidence Strength

Each font domain should submit or consume provenance with an evidence level.
The strongest applicable evidence wins write authority for a TextView/Paint
size event.

| Evidence | Example | Write Authority |
| --- | --- | --- |
| Explicit semantic unit | `TextView.setTextSize(SP, x)` or `setTextSize(float)` | Strong |
| Resources font state | `Configuration.fontScale == targetFactor` and matching `scaledDensity` | Strong for SP-derived text |
| Explicit absolute unit | `TextView.setTextSize(PX/DIP/PT/IN/MM, x)` | Strong for absolute TextView writes |
| View current px | `TextView.getTextSize()` during attach/draw/textAppearance | Weak |
| Paint current px | `Paint.setTextSize(px)` / `TextPaint.setTextSize(px)` | Weakest |
| Compose resources state | `ComposeView`/`AndroidComposeView` consuming Resources `fontScale`/`scaledDensity` | Strong evidence that Resources reached Compose, but not proof that the visual result is proportional |

The key distinction is that "enabled" means "allowed to participate", not
"allowed to multiply unconditionally".

## Arbitration Rules

Recommended first-pass rules:

1. If `resources_font` is active and the current Resources state already
   reflects the target factor, `SP` TextView writes are considered handled by
   Resources. TextView SP rewrite records provenance but does not multiply.
2. If `resources_font` is not active or has not taken effect, `SP` TextView
   rewrite may write the target px.
3. `PX/DIP/PT/IN/MM` TextView writes are owned by
   `textview_absolute_rewrite` when that domain is enabled.
4. `textview_current_px_fallback` may write only when the TextView has no
   stronger provenance from Resources, SP rewrite, or absolute rewrite.
5. `paint_text_size_fallback` may write only when Paint provenance does not
   show an already-applied DPIS value. TextView provenance may suppress Paint
   only at explicitly proven TextView-owned call sites, such as nested
   framework Paint updates during a `TextView.setTextSize(...)` operation or
   known Android text-layout/span measurement paths.
6. `basePx * factor` remains a safety net against double-scaling during rebase,
   not proof that a value is DPIS-applied provenance.

For WeChat-style mixed chains, this means:

```text
SP TextView text + Resources font active
  -> resources_font wins
  -> textview_sp_rewrite observes/records
  -> current_px fallback skips

Absolute TextView text
  -> textview_absolute_rewrite wins

Unknown TextView px with no provenance
  -> textview_current_px_fallback may write

Paint-only custom text
  -> paint_text_size_fallback remains the final fallback
```

## Compose Runtime Direction

The `com.wispho.ks` validation exposed a different failure shape from the
WeChat TextView/Paint chain:

- The active view hierarchy is `ComposeView -> AndroidComposeView`.
- `resources_font` changes `Configuration.fontScale` and `scaledDensity`.
- TextView/Paint callback logs are absent for the visible text.
- Visible text bounds can grow by more than the configured factor, and some
  text can become clipped.

This should be treated as a Compose resources compatibility problem, not as a
TextView/Paint repeat-multiplication problem. Resources font state is still real
evidence, but for Compose it affects text and layout together. A target of
150% does not guarantee that every Compose semantics text bound grows by 1.5x.

The implementation should move in two steps.

### Step 1: Runtime Classification And Safe Scheduling

First, make the runtime decision explainable and reversible without adding a
Compose-specific text hook.

Detect whether the active window is Compose-heavy by observing Jetpack Compose
library view classes such as:

- `androidx.compose.ui.platform.ComposeView`
- `androidx.compose.ui.platform.AndroidComposeView`

Classification should be view-tree/window scoped. A transient `ComposeView`
inside one Activity must not permanently classify the whole process. The
runtime may keep process-level counters for diagnostics, but scheduling
decisions should be based on the current root/window observation and reset when
that root/window changes.

The detector should feed diagnostics and scheduling, not package-specific
behavior. A Compose-heavy page is considered Resources-handled only when all of
these are true:

- the resolved domain plan enables `resources_font`;
- observed `Configuration.fontScale` matches the target factor;
- observed `DisplayMetrics.scaledDensity / density` matches the target factor;
- the current root/window is classified as Compose-heavy.

For that case:

- record that Resources reached Compose;
- avoid interpreting missing TextView/Paint callbacks as hook failure;
- log and record diagnostic evidence that Resources font may be causing
  layout-level scaling;
- keep TextView/Paint provenance arbitration intact for any real Android views
  that still appear in the same process;
- allow users to disable `resources_font` per app through the existing domain
  editor when Compose layout scaling is unacceptable.

This step changes scheduling semantics only at the classification and
explanation layer. It must not silently compensate the user's target percentage.
For example, a configured 150% target should not become an implicit 125%
Resources write without an explicit mode or documented policy.

Suggested tests:

- Compose view detection returns true for known Compose class names.
- A Compose-heavy current root/window with `resources_font` enabled, matching
  `Configuration.fontScale`, and matching `scaledDensity / density` is reported
  as Resources-handled.
- A Compose view in a stale or different root/window does not classify the
  current window as Compose-heavy.
- Missing TextView/Paint mutation counters do not mark the font route failed
  when Compose resources evidence exists.
- TextView current-px fallback remains governed by TextView provenance, not by
  a global "Compose page" switch.

Implemented first-pass behavior:

- `ComposeFontRuntimeClassifier` classifies only the supplied current root tree.
- `ComposeResourcesFontEvidence` requires the full Resources-handled evidence
  set: domain enabled, matching `Configuration.fontScale`, matching
  `scaledDensity / density`, and Compose-heavy current root.
- `ComposeFontRuntimeDiagnosticsInstaller` registers Activity lifecycle
  callbacks when an Application is available. If app-process hooks are installed
  before `ActivityThread.currentApplication()` exists, it installs protected
  `Application.attach`/`onCreate` retry hooks instead of permanently skipping
  diagnostics.
- Runtime diagnostics evaluate the current Activity decor root on resume and on
  throttled global-layout changes, even when ordinary runtime logging is
  disabled, because the evidence now feeds scheduling.
- Matching evidence feeds a short-lived process-local scheduler. The scheduler
  suppresses Resources fontScale/scaledDensity writes back to the inferred base
  font scale for the same package while the evidence is fresh.
- The scheduler does not modify the configured target percentage, disable
  `resources_font`, or suppress TextView/Paint fallback domains.

### Implemented Slice: Resources Font Scheduling For Compose

The current implementation can identify a Compose-heavy root and report that
`resources_font` reached Compose. When all evidence matches, it also performs a
narrow scheduling action: Resources fontScale/scaledDensity writes are
temporarily suppressed to the inferred base font scale for that package.

Working position:

- Do not globally disable TextView current-px fallback just because a page is
  Compose-heavy. TextView provenance remains the authority for real TextViews.
- Do not enable Paint fallback by default as a workaround for Compose. Paint has
  no reliable Compose owner or unit information.
- Treat `resources_font` as the current broad compatibility owner for
  Compose-heavy pages only when the full evidence set matches: domain enabled,
  observed `Configuration.fontScale`, observed `scaledDensity / density`, and
  current-root Compose classification.
- If the automatic suppression is still visually unacceptable, the supported
  user escape hatch remains the existing custom chain editor: disable
  `resources_font` for that app and let remaining routes handle whatever
  non-Compose Android text they can.
- A real automatic fix needs a separate Compose-native backend or an explicit
  Compose policy. It should not silently compensate the target factor inside
  `resources_font`, because that would change the meaning of a configured
  150% target and could regress non-Compose Resources users.

For later Compose-native work, start by collecting one paired trace for a
Compose-heavy app:

1. `DPIS_FONT app hook plan` for the target package.
2. `ComposeFontRuntimeDiagnosticsInstaller` evidence for the current root.
3. Resources `fontScale`, `density`, and `scaledDensity` values.
4. Visual expectation: whether disabling only `resources_font` brings the page
   closer to the configured factor, and which remaining domains still produce
   visible changes.

The next implementation should remain a scheduling/backend decision, not a
package-specific special case. Candidate outputs are:

- a user-visible per-app Compose/resources policy is added;
- a new Compose-native domain is researched and introduced separately.

### Step 2: Compose-Native Font Backend

The final target is a dedicated Compose-compatible font backend that can scale
Compose text without relying on global `Configuration.fontScale` side effects.
That backend should be introduced only after the classification step provides
stable evidence about which apps and pages actually need it.

The desired end state is:

- DPIS can identify Compose text ownership separately from TextView/Paint.
- Compose text scaling can be applied closer to Compose text measurement or
  font scaling inputs.
- Resources font remains available as a broad compatibility route, but it is
  no longer the only path for Compose-heavy apps.
- The domain editor can expose this as a separate cross-runtime domain if the
  hook point proves stable enough.

This is a larger semantic change than Step 1. It likely requires research into
Jetpack Compose runtime internals, version differences, and stable hook points
around Compose density/font scaling or text layout. It should not be mixed into
the first scheduling step.

Open research questions:

- Which Compose runtime classes are stable enough across app versions to hook?
- Can DPIS affect only font scale while leaving dp density and layout
  constraints intact?
- Can the hook be scoped to app text without changing icons, spacing, or
  non-text layout?
- What runtime evidence proves that a Compose-native backend actually handled
  a visible text node?

## Proposed Runtime Model

Introduce a shared TextView provenance tracker beside the existing Paint
tracker. A first version can be package-private and used only by
`ForceTextSizeHookInstaller`.

Suggested fields:

```java
final class TextViewFontProvenance {
    float basePx;
    float appliedPx;
    float factorAtApply;
    Source source;
    UnitKind unitKind;
}

enum Source {
    RESOURCE_FONT_SCALE,
    TEXTVIEW_SP_REWRITE,
    TEXTVIEW_ABSOLUTE_REWRITE,
    TEXTVIEW_CURRENT_PX_FALLBACK
}

enum UnitKind {
    SP,
    ABSOLUTE,
    UNKNOWN
}
```

The tracker does not need to be a public abstraction. It should start as a
small internal helper with behavior tests around arbitration decisions.

## Hook-Level Changes

### TextView.setTextSize(int, float)

- Convert the incoming size to px for comparison and logging.
- Determine `UnitKind` from the incoming unit.
- If unit is `SP` and Resources font is already active for the target factor,
  record `RESOURCE_FONT_SCALE/SP` provenance and return after `chain.proceed()`.
- If unit is `SP` and Resources font is not active, allow
  `textview_sp_rewrite` to write.
- If unit is absolute, allow `textview_absolute_rewrite` to write.

### TextView.setTextSize(float)

Android treats this overload as `SP`. It should use the same SP arbitration
path as `setTextSize(SP, value)`.

### TextView current-px fallback

Before calling `resolveScaledTextSize`, ask the tracker whether a stronger
owner already exists. If so, skip. Otherwise treat the observed `getTextSize()`
as `UNKNOWN` and allow fallback write.

### Paint/TextPaint fallback

Keep the current Paint provenance semantics:

- Trust only explicit `lastAppliedPx + factorAtApply`.
- Keep `basePx * factor` as a rebase safety net.
- Do not infer SP/absolute ownership from Paint px alone.

If a future hook can reliably associate a Paint with a TextView owner, it may
consult TextView provenance before writing. That is not required for the first
pass.

## Logging

Add sampled debug logs that expose arbitration decisions without high-volume
noise:

- source domain requesting write
- unit kind
- current px
- target px
- existing provenance source
- decision: `write`, `observe`, or `skip`
- reason: for example `resources-font-handled-sp`,
  `stronger-textview-provenance`, `unknown-current-px`, or
  `paint-last-applied-match`

The existing `DPIS_FONT` prefix is sufficient. If temporary probes are added,
use a removable `[DEBUG-...]` tag.

## Implementation Slices

1. Add unit/resource arbitration helper.
   - Test Resources-active SP skip.
   - Test Resources-inactive SP rewrite.
   - Test absolute unit rewrite remains allowed.
2. Add TextView provenance tracker.
   - Record `RESOURCE_FONT_SCALE/SP` when SP is already handled.
   - Record explicit DPIS writes with source and factor.
3. Gate current-px fallback on stronger TextView provenance.
   - Test attach/draw fallback skips after SP provenance.
   - Test unknown TextView px still falls back.
4. Keep Paint fallback behavior unchanged except for optional consultation
   points if a reliable owner exists.
   - Existing Paint provenance tests must continue passing.
5. Add concise arbitration logs and update this document with final behavior.

## Regression Tests

Suggested tests:

- `resourcesFontActive_spSetTextSizeDoesNotMultiplyAgain`
- `resourcesFontInactive_spSetTextSizeUsesTextViewRewrite`
- `absoluteTextSizeStillUsesAbsoluteRewriteWhenResourcesFontActive`
- `currentPxFallbackSkipsTextViewWithResourceSpProvenance`
- `currentPxFallbackAppliesToUnknownTextViewSize`
- `paintFallbackDoesNotTreatTextViewResourceProvenanceAsPaintProof`
- `paintFallbackStillSkipsLastAppliedValue`

Where Android framework objects are hard to instantiate in local unit tests,
test the arbitration helper and provenance tracker directly, then use source
smoke tests for hook wiring.

## Open Questions

- Should `resources_font` active be detected from resolved domain plan only, or
  from actual observed `Configuration.fontScale/scaledDensity`? The safer first
  pass is to require both: domain enabled and observed Resources state matching
  the target factor.
- Should current-px fallback be disabled globally when `resources_font` is
  active? The more precise first pass is no: disable it only for TextViews with
  stronger provenance.
- Should Paint fallback be able to read TextView provenance? Only if a reliable
  owner relationship is available. Otherwise Paint remains weak and independent.

## Expected Behavior Change

Opening multiple font domains will no longer mean every enabled domain writes
to the same text object. The runtime will choose the strongest owner for each
event and let weaker domains observe or skip.

This is the practical meaning of "unified scheduling" for DPIS font hooks:
the user can enable broad coverage, while the module avoids multiplying a size
again when a stronger upstream path has already handled it.
