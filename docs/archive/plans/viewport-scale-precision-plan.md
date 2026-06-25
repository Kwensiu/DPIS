# Viewport Scale Precision Upgrade Plan

This document records the planned upgrade from integer-only viewport scale input
and legacy permille storage to true three-decimal percentage support.

The goal is not to make the UI display more decimal places. The goal is to
preserve user-entered relative scale precision through input, storage,
templates, runtime properties, diagnostics, and runtime target resolution.

## Scope

The upgrade applies to every user-visible and runtime-visible viewport relative
scale path:

- app config sheet input and prefill
- template config input, summary, storage, and application
- app-list status text
- feedback diagnostic summary and export text
- package config storage
- runtime property projection and bridge reads
- runtime target width calculation
- copy, prefill, and quick-template flows that carry viewport scale values

Absolute-width targets are out of scope except where shared viewport code needs
to distinguish fixed width from relative scale.

## New Unit

New code should use `scaleMilliPercent` for relative viewport scale.

`scaleMilliPercent` stores percent values with three decimal places:

- `30.000%` is `30000`
- `83.333%` is `83333`
- `100.000%` is `100000`
- `300.000%` is `300000`

The allowed range remains closed: `30.000%` through `300.000%`.

Do not reuse `scalePermille` for the new unit. A name that still says
`Permille` but returns `scaleMilliPercent` would make divide-by-1000 mistakes
hard to spot. After the upgrade, `scalePermille` should appear only in legacy
compatibility code and tests that explicitly cover legacy behavior.

## Compatibility

DPIS must continue to read existing configurations and should remain usable when
a user downgrades to a version that only understands the old field.

Use this compatibility model:

- Read new `scaleMilliPercent` fields first.
- If no new value exists, fall back to the legacy `scalePermille` field.
- When saving a relative scale, write both:
  - the new high-precision value as `scaleMilliPercent`
  - the legacy compatible value as `scalePermille`
- When disabling or clearing a relative scale, clear both new and legacy values.

The legacy compatible value is the new value rounded to the old internal
`0.1%` precision. For example:

- `83.333%` writes legacy `83.3%`
- `83.350%` writes legacy `83.4%`
- `83.349%` writes legacy `83.3%`

This gives downgrade behavior that preserves the nearest value old builds can
understand. Old builds may still lose precision if the user edits and saves the
value there, because old UI input accepts only integer percentages.

## Input Rules

Viewport relative scale input accepts standard decimal numbers without a percent
sign.

Allowed examples:

- `83`
- `83.`
- `83.3`
- `83.33`
- `83.333`
- `30.000`
- `300.000`

Rejected examples:

- `.83`
- `+83`
- `083`
- `83.3333`
- `83%`
- `29.999`
- `300.001`

If a value ends with a decimal point, such as `83.`, save it as the integer
percent `83%`. Leading zeroes are not allowed. All invalid input should continue
to use the existing generic invalid-input UI message.

## Display Rules

All user-visible and diagnostic-visible relative scale text should use one
shared formatter.

The formatter should show at most three decimal places and trim insignificant
trailing zeroes:

- `100.000%` displays as `100%`
- `83.300%` displays as `83.3%`
- `83.330%` displays as `83.33%`
- `83.333%` displays as `83.333%`

Do not add UI copy solely to advertise three-decimal support.

## Runtime Semantics

Configuration identity and runtime effect are separate concerns.

Configuration-level code should preserve the original `scaleMilliPercent` value.
This includes templates, summaries, package storage, equality, and fingerprints
that represent user intent.

Runtime target reuse should avoid splitting equivalent effects. When two
relative scales resolve to the same final target width, runtime record borrowing
and de-duplication may use the final `targetWidthDp` plus the route/source
context instead of the raw scale value.

Final viewport width calculation still resolves to integer dp. The precision
upgrade changes the input scale used by the calculation; it does not remove the
final rounding step.

## Implementation Checklist

- Introduce shared parse and format helpers for `scaleMilliPercent`.
- Rename normal business logic from `scalePermille` to `scaleMilliPercent`.
- Keep `scalePermille` names only at legacy compatibility boundaries.
- Add new storage keys for high-precision values.
- Read new keys first and fall back to legacy keys.
- Double-write new high-precision values and legacy rounded values.
- Update runtime property projection and bridge reads with the same new-first,
  legacy-fallback behavior.
- Update target-width calculations to divide by the new scale base.
- Update app config, templates, summaries, diagnostics, copy, and prefill flows
  to use the shared formatter.
- Clear new and legacy values together when relative scale config is removed.

## Test Coverage

Tests should cover the full chain because this change is easy to partially
apply without compiler errors.

Required coverage:

- parsing accepted values: `83`, `83.`, `83.3`, `83.33`, `83.333`
- rejecting invalid values: `.83`, `+83`, `083`, `83.3333`, `83%`
- boundary values: `30.000`, `300.000`, `29.999`, `300.001`
- formatting with at most three decimals and trimmed trailing zeroes
- new storage read/write
- legacy fallback reads
- double-write legacy rounding
- template read/write and summary text
- runtime property projection and bridge fallback
- target-width resolver and display override calculation
- source smoke checks that normal business logic no longer uses
  `scalePermille`

## Completion Criteria

The implementation is not complete until a literal search for `scalePermille`
shows only legacy compatibility boundaries and tests that explicitly verify
legacy compatibility.
