# Domain Docs

DPIS currently uses a single-context documentation layout.

## Current Sources

Agents should read these files first when they need project context:

- `AGENTS.md` for repository workflow, testing, and runtime debugging rules.
- `CONTEXT.md` for DPIS domain language and route boundaries.
- `docs/font-routing.md` for font mode, custom hook-chain, and internal
  scheduler-domain ownership.
- `docs/java-toolchain-policy.md` for JDK, Java compatibility, and Android API
  boundaries.
- `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md` for the
  project-local runtime route diagnosis skill bundle, trigger conditions, probe
  order, and evidence requirements.
- `docs/legacy-runtime-resync.md` before changing Legacy viewport/runtime
  routes, and whenever shared route code may affect Legacy.
- `docs/modern-runtime-resync.md` before changing Modern viewport/runtime
  routes, and whenever shared route code may affect Modern.
- `docs/private/` for app-specific investigation notes (not committed).
- `docs/lsposed-diagnostics.md` for LSPosed module log pull-and-filter paths
  used when diagnosing hook installation and callback evidence.
- `README.md` for user-facing product behavior and supported configuration modes.
- `docs/README.md` for the active documentation index.
- `docs/ui-guidelines.md` for UI change rules and resource naming expectations.

## Agent Tooling

Agents may use CodeGraph for structure-aware code navigation, symbol lookup,
callers/callees, and impact analysis. Treat CodeGraph as a navigation and
blast-radius aid, then verify behavior against nearby source, tests, and the
runtime route documents before making conclusions.

When using CodeGraph, prefer current runtime source under `app/src/**`.
Historical snapshots under `docs/archive/` can be indexed too, so same-named
classes or methods from archive paths should not be treated as active code.

The two runtime resync documents are living DPIS route documents. Every new
route exploration, route-detail adjustment, failed attempt, unused path, and
important runtime finding should be recorded there instead of being discarded.
Prefer marking historical entries inactive, superseded, or rejected over
deleting them.

## Architecture Notes

There is no `docs/adr/` directory yet. Until ADRs exist, use `CONTEXT.md`, the active docs above, and nearby production code/tests as the authoritative source.
