# Secondary navigation and font session design

## Goal

Reduce structural coupling in the secondary-page migration without changing
user-visible routes, persistence, or asynchronous font-library behavior.

## Design

- `SecondaryDestination` remains a pure route value under `ui`.
- Feature sessions receive a `SecondaryNavigation` dependency directly. They
  no longer route through `MainWorkspaceSession` or `MainComposeShellHost`.
- Concrete Activity-to-Intent mapping lives in the app shell navigation host,
  while Compose only sees the destination interface.
- `FontLibrarySession` remains the lifecycle/presentation facade. Import,
  archive, and health workflows move behind focused collaborators with the
  same callbacks, request codes, worker threads, and UI refresh ordering.

## Verification

Run the full debug unit-test suite and both Modern/Legacy debug assemblies.
Run `git diff --check` and inspect the final call graph for removed forwarding
layers.
