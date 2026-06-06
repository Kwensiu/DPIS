# System Font Mutation Scheduler Context

## Problem

DPIS system viewport emulation already has lifecycle-aware scheduling ideas:
capture pre/post snapshots, compute a target viewport environment, and use
runtime markers to avoid treating an already-mutated viewport as a fresh source.

System font emulation currently reuses the same system_server lifecycle hook
surface, but `Configuration.fontScale` is applied directly inside the generic
environment mutation path. That makes font scaling look equivalent to viewport
mutation even though Android treats font scale changes as `CONFIG_FONT_SCALE`,
which can trigger Activity relaunch.

## Current Hotspots

- `SystemServerDisplayEnvironmentInstaller`
  - Installs system_server hook entries.
  - Captures snapshots before and after framework calls.
  - Computes viewport target environments.
  - Applies viewport, frame, display info, and font mutations in one method.
- `SystemServerMutationPolicy`
  - Defines which hook entries run pre/post and which survive safe mode.
  - Currently entry-oriented, not mutation-field-oriented.
- `HookExecutionPlanner`, `HookDomainPlan`, `FontHookDomainRegistry`
  - Decide whether `system_server_font` is active for a package.
  - Expose hook-chain domains to UI/runtime config.
- Route docs
  - `docs/modern101-runtime-resync.md`
  - `docs/compat100-runtime-resync.md`

## Current Evidence

- Bilibili and Douyin flicker correlates with `system_server_font`.
- Logs showed `Configuration.fontScale` mutation followed by
  `CONFIG_FONT_SCALE`, `hookShouldRelaunchLocked`, and relaunch drawing.
- Disabling only system_server font mutation stopped flicker while app-process
  font domains could continue to scale text.

## Desired Semantics

DPIS should have one unified scheduling vocabulary:

- A hook entry is only a lifecycle observation/mutation point.
- A mutation field declares where it may write and what baseline it uses.
- Viewport and font can share the scheduler, but not necessarily the same
  allowed entries or relaunch risk.
- Hook-chain domains express requested capability. The scheduler owns effective
  execution and fallback, so a risky route being enabled in UI does not imply it
  may write at every lifecycle entry.
- Bilibili and Douyin are reproduction evidence for `FONT_SCALE` scheduling.
  They must not become built-in package recommendations or separate app-specific
  routes.
