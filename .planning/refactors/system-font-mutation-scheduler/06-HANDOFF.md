# System Font Mutation Scheduler Handoff

## Recommended Next Step

Start with Wave 1 only.

Wave 1 gives the codebase a visible semantic anchor without changing runtime
behavior. That is important because the current bug is real, but the installer
is high-risk and already has active edits in the worktree.

## First Implementation Boundary

Files likely touched in Wave 1:

- `SystemServerDisplayEnvironmentInstaller.java`
- `SystemServerMutationPolicy.java`
- `SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java`
- `docs/modern101-runtime-resync.md`
- `docs/compat100-runtime-resync.md`

Wave 1 should not touch:

- UI layout/string ordering beyond existing pending work.
- App-process font hook implementations.
- Hook domain persistence format.
- Viewport calculation internals.

## Decision To Preserve

Viewport and font can share a unified scheduler vocabulary, but `fontScale` must
not inherit every viewport lifecycle write point by default.
