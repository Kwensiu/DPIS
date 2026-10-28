# ADR 0001: User-Scoped Package Config Source Of Truth

## Status

Accepted

## Context

DPIS package configuration needs a stable identity across the app list,
backup/restore, runtime delivery, and multi-user Android environments. A package
name alone is insufficient when the same application is installed in multiple
Android users: those installations may require different DPIS settings.

## Decision

DPIS will converge on one sparse package config model keyed by
`(packageName, userId)`.

- The pair is the stable identity for UI, backup/restore, runtime snapshots,
  and configured-app counting.
- `target_packages` remains migration evidence only and must not continue as a
  live source-of-truth index.
- The model is sparse: only present blocks and values are stored for a package.
- Optional package-specific blocks exist only when needed.
- Minimal display metadata such as last known app label may be stored so
  uninstalled configured packages remain understandable.
- Current install state is derived from the package manager for the same user,
  not treated as a cached storage fact.

DPIS package-state terminology is explicitly split:

- `User-configured package`: a package identity with saved user-preserved
  package-level state, including uninstalled package identities.
- `Scope-injected package`: an installed package identity in the known active
  LSPosed scope for that Android user. This is injection status, not saved
  package configuration.
- The home configured-apps card and Configured Apps list show the union of
  user-configured and scope-injected package identities. Scope-only entries
  must be labeled as injected without saved values and must not create stored
  package config.
- `Stored package state`: any persisted package-local state.
- `Runtime-target package`: package state that should affect runtime hooks or
  runtime snapshots.

User-configured packages include numeric values, mode-only state,
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

- UI, backup/restore, runtime delivery, migration, and package listing share
  one explicit Android user identity.
- The same app can have independent settings in primary and cloned profiles.
- Future cleanup of legacy scattered keys becomes tractable.
- Mode-only and uninstalled package states can be represented intentionally
  instead of inferred ad hoc from raw keys.

### Negative

- Migration from old scattered keys must be implemented carefully.
- A repository or adapter layer is needed before the storage backend can be
  switched safely.
- Existing package-only settings require a carefully defined migration into
  user-scoped identities.

## Implementation Direction

Introduce a package-config repository layer first, then migrate the repository
backend from scattered legacy keys to the package-aggregated model. Do not
attempt a direct repo-wide storage-format flip without first centralizing reads
and writes.
