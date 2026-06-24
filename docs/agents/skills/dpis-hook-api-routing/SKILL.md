---
name: dpis-hook-api-routing
description: Project-local DPIS hook implementation and diagnosis workflow for Xposed API routing. Use when implementing, reviewing, migrating, or debugging Legacy Xposed hooks, libxposed hooks, API 101 or 102 capability gating, modern/legacy flavor hook wiring, hook entry selection, or fallback behavior between old and new framework capabilities.
---

# DPIS Hook API Routing

This is a project-local skill bundle. It keeps DPIS hook work aligned with the
repository's split between pure Legacy Xposed behavior and Modern libxposed
behavior, while making API 101 and 102 capability decisions explicit.

## Trigger

Use this skill before work involving:

- hook implementation under `app/src/legacy/java/` or `app/src/modern/java/`
- changes to shared runtime code that affects both hook stacks
- migration from a Legacy-only pattern to a libxposed-capable pattern
- deciding whether a new libxposed API should be used or gated off
- diagnosing why a hook works on one framework/API level but not another
- reviewing branch changes whose main risk is hook entry, install, callback, or
  framework API compatibility

## Do Not Use

Do not use this skill for:

- ordinary UI changes unrelated to runtime hooks
- pure localization or documentation edits
- general Java cleanup that does not alter hook behavior or API routing
- route-policy diagnosis already covered by
  `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md` unless the task
  also requires API-family routing or hook-stack selection

## Required Reads

Read only what applies to the task:

- Always read `AGENTS.md` and `CONTEXT.md`.
- For Legacy work, read [references/legacy.md](references/legacy.md).
- For Modern/libxposed work, read
  [references/libxposed.md](references/libxposed.md).
- For cross-flavor migration or shared abstractions, read
  [references/migration.md](references/migration.md).
- For diagnosis or sign-off, read
  [references/verification-checklist.md](references/verification-checklist.md).
- For runtime route semantics, also read
  `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md` and the relevant
  living route document.

## Core Rule

Treat `legacy` and `modern` as different hook families with different API
contracts.

- Keep Legacy pure. Do not force libxposed concepts into Legacy code just to
  unify style.
- Keep Modern unified across libxposed capability levels, but only by explicit
  capability gating.
- Do not use a newer libxposed API only because the source compiles. First
  prove that the target framework API level and capability surface permit it.
- Every use of a newer API must have a fallback or a deliberate unsupported
  decision that is documented in code comments and the task summary.

## Workflow

1. Classify the touched code as `legacy`, `modern`, or shared.
2. Identify the hook kind: method, resource, app-process, `system_server`,
   entry-point registration, or framework capability detection.
3. Pick the owning flavor workflow:
   - Legacy: use the pure Legacy route in `references/legacy.md`.
   - Modern: use the libxposed family route in
     `references/libxposed.md`.
   - Shared: define the flavor boundary first, then read
     `references/migration.md`.
4. For Modern, prove the capability boundary before selecting a higher-capability
   behavior.
5. Implement the narrowest hook shape that preserves the flavor boundary.
6. Verify entry, install, callback, visible effect, and fallback behavior with
   `references/verification-checklist.md`.

## Decision Order

Make decisions in this order:

1. Which flavor owns this behavior?
2. Does the implementation belong in flavor-specific code or shared code?
3. What is the minimum API surface needed for the desired behavior?
4. Is that API surface guaranteed on the target framework?
5. If not, what is the fallback: API 101-compatible behavior, Legacy behavior,
   or explicit non-support?

Do not invert this order by starting from a preferred new API and searching for
somewhere to force it in.

## Implementation Rules

- Prefer flavor-specific code over shared branching when the behavior is truly
  different.
- Prefer shared helpers only for stable concepts such as capability detection,
  planner decisions, or common config parsing.
- If a helper mentions framework callback types, hook registration objects, or
  framework-owned lifecycle entry types, it is no longer semantic-only shared
  code.
- Leave comments where the code chooses between API families or intentionally
  avoids a newer API. State the reason, not the obvious syntax.
- When a higher API path is optional, make the fallback path readable and test
  worthy. Do not hide it behind reflection-heavy indirection unless the
  framework contract requires it.
- When a framework or LSPosed UI exposes only one primary API version, do not
  assume that all runtime capability questions collapse to that label. Prove the
  capability actually used by the hook.
- Do not copy Modern lifecycle timing assumptions into Legacy code. A behavior
  that installs around `Application.attach(...)` in Modern may still need a
  different Legacy-owned entry or callback boundary.

## Capability Proof

Treat `capability proven` as a task-level gate, not a vibe.

For a newer Modern/libxposed API path, require all of these before enabling it:

1. The framework exposes the required API version or capability signal.
2. The exact feature used by the newer path is identified in the task or code
   comments.
3. The API 101-safe path is still present and understandable.
4. There is a concrete reason the newer path improves correctness, safety, or
   maintainability beyond style.
5. Verification covers both the chosen higher-capability path and the fallback
   path, at least by tests, logs, or an explicit unsupported decision.

Version number alone is acceptable only when the newer feature is fully defined
by that version boundary and the task does not rely on extra runtime probing for
that specific API. If feature-specific runtime probing exists and is cheap, use
it.

## Templates

Use the template snippets only as starting points:

- [assets/legacy-method-hook-template.java](assets/legacy-method-hook-template.java)
- [assets/legacy-resource-hook-template.java](assets/legacy-resource-hook-template.java)
- [assets/libxposed-101-hook-template.java](assets/libxposed-101-hook-template.java)
- [assets/libxposed-102-gated-hook-template.java](assets/libxposed-102-gated-hook-template.java)
- [assets/shared-capability-gate-template.java](assets/shared-capability-gate-template.java)

Adapt naming, ownership, and comments to the touched module instead of copying
the template shape verbatim.

## Output

When finishing hook work or review, report:

- flavor and hook family touched
- whether the change is Legacy-only, Modern-only, or shared
- which libxposed capability boundary was assumed or proven
- fallback behavior when the new API is unavailable
- tests, runtime checks, or log evidence used
- docs or route records updated
