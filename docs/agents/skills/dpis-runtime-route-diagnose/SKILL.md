---
name: dpis-runtime-route-diagnose
description: Project-local DPIS runtime hook diagnosis workflow. Use for LSPosed/Xposed logs, app flicker or relaunch, missing DPI/viewport/font scaling, font hook chains, system_server, ActivityThread, Resources, Display, WindowMetrics, WebView, TextView, Paint, Flutter, HyperOS, or shared runtime route changes.
---

# DPIS Runtime Route Diagnose

This is a project-local skill bundle. It is intentionally stored in the
repository instead of a global Codex skill, so new conversations can discover
DPIS runtime rules through `AGENTS.md` without modifying user-wide skill files.

## Trigger

Use this skill before diagnosing or changing behavior involving:

- LSPosed / Xposed runtime hooks
- app flicker, flash, relaunch, or configuration churn
- missing DPI, viewport, smallest-width, or font scaling
- font hook chains, route domains, or template recommendations
- `system_server`, ActivityThread, Resources, Display, WindowMetrics, WebView,
  TextView, Paint, Flutter, or HyperOS font routes
- shared runtime code under `app/src/main/java/com/dpis/module/`

## Do Not Use

Do not use this skill for:

- pure UI layout work
- release-note wording
- ordinary Java refactors that do not affect runtime hook behavior
- CI failures unless the failure changes runtime route behavior

For Java / CI API compatibility questions, read
`docs/java-toolchain-policy.md` instead.

## Required Reads

Read only the documents needed for the touched route:

- Always read `AGENTS.md`.
- For font routes, read `docs/font-routing.md`.
- For modern101 routes, read `docs/modern101-runtime-resync.md`.
- For compat100 routes or shared code that may affect compat100, read
  `docs/compat100-runtime-resync.md`.
- For log collection, read `docs/lsposed-diagnostics.md`.
- For Java / CI API compatibility questions, read `docs/java-toolchain-policy.md`.

## Workflow

1. Classify the layer that owns the suspected behavior.
2. Gather the smallest evidence set that can prove or disprove that layer.
3. Probe route execution one boundary at a time.
4. Choose the narrowest fix shape that expresses the route semantics.
5. Add or update focused tests for policy behavior.
6. Update the relevant living route document.

## Semantic Frame

Before editing, name the owning layer:

- UI preference or form draft
- recommendation template
- planner domain
- app-process route
- `system_server` route
- lifecycle entry
- mutation field
- config source
- LSPosed scope or module loading

If two layers appear to write the same state, resolve the ownership boundary
before adding another branch.

## Probe Order

Probe one boundary at a time:

1. Module entry reached.
2. Guard did not return early.
3. Dependency or class lookup succeeded.
4. Hook installed.
5. Callback fired.
6. Target package resolved.
7. Config source returned the expected app config.
8. Field policy allowed the mutation.
9. Mutation changed the runtime object.
10. App-visible behavior changed.

`hook ready` proves only that installation ran. Require a callback, mutation,
counter, or visible result before calling a route effective.

## Flicker And Relaunch Checks

For flicker or relaunch reports:

- Check whether Activity relaunch is tied to configuration changes.
- Look for `CONFIG_FONT_SCALE`, Activity relaunch, and config dispatch logs.
- Separate viewport changes from `FONT_SCALE` changes.
- For system-mode font scaling, verify `FONT_SCALE` writes are limited to the
  launch entry.
- Treat app-specific repros as route evidence, not as immediate package-list
  requirements.

## Fix Preference

Prefer fixes in this order:

1. Clarify ownership and naming.
2. Express the behavior as a planner, scheduler, entry, or field policy.
3. Add focused unit tests for the policy.
4. Update route documents.
5. Add app-specific package lists only when policy cannot represent the
   behavior.
6. Add independent hook routes only when existing route domains cannot safely
   model the behavior.

## Test Expectations

For route policy changes, add or update focused tests that would fail if the
semantic rule regressed. Source smoke tests are acceptable for wiring checks,
but they should not be the only coverage for scheduler or planner behavior.

## Output

When finishing a runtime route task, report:

- the semantic rule changed or confirmed
- the affected route, entry, and field
- evidence used
- tests or runtime checks run
- route documents updated
