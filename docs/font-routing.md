# Font Routing Semantics

This document defines the stable language for DPIS font scaling routes. It is
about product and runtime semantics, not app-specific investigation notes.

## Mode Tree

```text
requested font mode
  |
  +-- system
  |     |
  |     +-- DPIS owns scheduling across internal runtime domains
  |     +-- system_server_font may write Configuration.fontScale at launch
  |     +-- activity_thread_font may supplement app bind configuration
  |     +-- Resources / WebView semantic supplements may remain available
  |     +-- custom hook-chain UI state is ignored
  |
  +-- compat
  |     |
  |     +-- custom hook-chain UI state may select field-rewrite domains
  |     +-- Resources / TextView / Paint / WebView field routes apply in app process
  |
  +-- off
        |
        +-- no DPIS font route
```

## Ownership Boundaries

- User-facing font mode selects the high-level strategy: `system`, `compat`, or
  `off`.
- The custom font hook-chain dialog configures only compat / field-rewrite
  routes.
- `system_server_font` and `activity_thread_font` are internal scheduler
  domains. They are not user-customizable hook-chain switches and are not saved
  in custom hook-chain overrides.
- Restoring the custom hook-chain defaults returns to the compat recommended
  template. It must not rewrite the system-mode internal scheduler state.

## System Mode Scheduling

System mode should be stable even when an internal domain is risky for a
particular runtime entry. Prefer scheduler policy over user-facing package
recommendations.

- `FONT_SCALE` is launch-only in `system_server`.
- Later lifecycle entries such as `config-dispatch` must not receive font-only
  configs.
- Viewport mutation remains multi-entry because Activity-level and display-level
  viewport state may need later lifecycle synchronization.
- Package selection in `system_server` is entry-aware and field-aware:
  font-only configs can enter `launch-activity-item`; viewport configs can
  enter viewport hot paths such as `config-dispatch` and `display-manager-info`.

## App-Specific Evidence

App-specific repros, such as flicker in a video app or social app, are evidence
for route behavior. They are not enough by themselves to create built-in
recommended route lists.

DPIS route fixes should prefer this order:

1. Express the behavior as a scheduler or field policy.
2. Document the route boundary and evidence.
3. Add a package list only when a reusable policy cannot express the behavior.
4. Add a new independent route only when the existing route model cannot safely
   represent the behavior.

## Documentation Rules

- Public route semantics belong here or in the runtime resync documents.
- Raw logs, screenshots, device paths, and app-specific investigation notes
  belong under `docs/private/` and must stay uncommitted unless intentionally
  sanitized.
- When changing font runtime routes, update this document if the mode tree or
  ownership boundary changes.
