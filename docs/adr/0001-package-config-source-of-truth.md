# ADR 0001: Package-Aggregated Config Source Of Truth

## Status

Accepted

## Context

DPIS currently stores per-app configuration across scattered preference keys
such as `viewport.<package>.*`, `font.<package>.*`, `dpis.<package>.*`,
hook-domain keys, app-specific keys, and the legacy `target_packages` index.

This structure makes package-state semantics ambiguous:

- user-visible configured packages
- runtime-target packages
- stored package preferences
- draft-only or mode-only package preferences
- migration residue or contradictory old state

The ambiguity caused inconsistent UI behavior such as configured-app counts and
configured-app list membership diverging for the same package set.

## Decision

DPIS will converge on one sparse, package-aggregated config model keyed by
package name.

- The aggregated package model becomes the long-term source of truth for package
  UI, backup and restore, runtime snapshots, and counting rules.
- `target_packages` remains migration evidence only and must not continue as a
  live source-of-truth index.
- The model is sparse: only present blocks and values are stored for a package.
- Optional package-specific blocks exist only when needed.
- Minimal display metadata such as last known app label may be stored so
  uninstalled configured packages remain understandable.

DPIS package-state terminology is explicitly split:

- `User-visible configured package`: a package with any saved user-preserved
  package-level state. It is the inclusion rule for the home configured-apps
  card and the Configured Apps list.
- `Stored package state`: any persisted package-local state.
- `Runtime-target package`: package state that should affect runtime hooks or
  runtime snapshots.

User-visible configured packages include numeric values, mode-only state,
target-type-only state, hook-domain-only state, app-specific config, explicit
`dpisEnabled=false` overrides, and saved configuration for apps that are no
longer installed. Draft-only config-sheet state is transient and does not count
unless saved.

Mode-only, target-type-only, and hook-domain-only package state are
user-preserved package preferences, not presumed residual state. This includes
viewport target type only, viewport apply mode only, font mode only, and
hook-domain-only package state.

Configured packages that are not currently installed must still appear in the
Configured Apps list, remain editable, and remain clearable. They must not
appear in the All Apps installed-app catalog.

Compact status text should use `已注入 | 空数值` / `Injected | No value` for
configured installed entries with no displayable numeric value, and
`未安装 | 空数值` / `Not installed | No value` for configured uninstalled entries
with no displayable numeric value.

## Consequences

### Positive

- One package identity and one package object become the basis for UI and
  runtime reasoning.
- Backup and restore behavior becomes easier to interpret and migrate.
- Future cleanup of legacy scattered keys becomes tractable.
- Mode-only and uninstalled package states can be represented intentionally
  instead of inferred ad hoc from raw keys.

### Negative

- Migration from old scattered keys must be implemented carefully.
- A repository or adapter layer is needed before the storage backend can be
  switched safely.
- Existing code that reads scattered keys directly must be consolidated.

## Implementation Direction

Introduce a package-config repository layer first, then migrate the repository
backend from scattered legacy keys to the package-aggregated model. Do not
attempt a direct repo-wide storage-format flip without first centralizing reads
and writes.
