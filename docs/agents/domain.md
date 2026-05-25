# Domain Docs

DPIS currently uses a single-context documentation layout.

## Current Sources

Agents should read these files first when they need project context:

- `AGENTS.md` for repository workflow, testing, and runtime debugging rules.
- `README.md` for user-facing product behavior and supported configuration modes.
- `docs/README.md` for the active documentation index.
- `docs/ui-guidelines.md` for UI change rules and resource naming expectations.

## Architecture Notes

There is no root `CONTEXT.md` or `docs/adr/` directory yet. Until those exist, use the active docs above plus nearby production code and tests as the authoritative source.

When a future `CONTEXT.md` is added, it should describe DPIS domain language rather than repeat the README. Good candidates are viewport targets, apply strategies, font hook domains, runtime property sync, and system-server/app-process boundaries.

