# System Font Mutation Scheduler Risk Map

## Risk Level

High.

## Evidence

- `SystemServerDisplayEnvironmentInstaller` is a central runtime hook installer
  with multiple system_server entrypoints.
- The same mutation method currently touches viewport configuration, frames,
  display info, and font scale.
- Behavior depends on Android framework lifecycle ordering and LSPosed hook
  execution, so failures can appear only after reboot/device runtime testing.
- Existing tests include source smoke and policy tests, but the actual relaunch
  behavior is mostly proven by logs rather than a deterministic unit seam.
- The worktree already contains related modified files from the current
  investigation, so structural changes must avoid mixing with unrelated cleanup.

## Blast Radius

- Runtime behavior for every app using system viewport mode.
- Runtime behavior for apps using system font mode.
- Hook-chain UI defaults and persistence for font domains.
- Route documentation for both modern101 and compat100.

## Main Coupling

- Hook domains decide whether a route is active.
- Execution planner translates app config into runtime domains.
- System_server installer applies all enabled system mutations.
- Mutation policy decides hook entry timing, but not by field.

## Stop Conditions

- A naming or extraction wave requires changing app-process font behavior.
- A viewport behavior test changes unexpectedly.
- Route docs cannot explain the new scheduling rule in one paragraph.
- Device logs still show `CONFIG_FONT_SCALE` relaunch after the font route is
  supposed to avoid config-dispatch writes.
