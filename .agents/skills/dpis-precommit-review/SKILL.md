---
name: dpis-precommit-review
description: Review DPIS changes before committing or opening a pull request, including scope, domain-rule compliance, tests, Android validation, Sonar inputs, and Git hygiene.
---

# DPIS Pre-Commit Review

Use this skill when a change is ready for commit, when the user asks for a
review before merging, or when a pull request check needs local validation.
It is a project workflow, not a replacement for `AGENTS.md`, `CONTEXT.md`, or
the runtime/localization playbooks. Read those documents first when their
scope applies.

## Review order

1. Establish the change boundary.
   - Run `git status --short`, `git diff --stat`, and `git diff --check`.
   - Inspect `git diff HEAD` and confirm every changed or untracked file belongs
     to the user's request. Never absorb unrelated work or reset it away.
   - Check the current branch and merge base when the review is for a PR.
   - Use `semble` for semantic discovery before `rg`; use `rg` only when every
     literal occurrence must be audited. For large command output, use
     context-mode batch/filter tools and surface only derived evidence.

2. Apply project and domain rules.
   - Read `AGENTS.md` and `CONTEXT.md`; read the relevant active document under
     `docs/` for runtime routes, diagnostics, or other named boundaries.
   - Check package/file ownership, Kotlin/Java boundaries, Compose state
     ownership, naming, comments, UTF-8 without BOM, and stale source-smoke
     anchors.
   - For runtime hooks, prove or preserve the route in order: entry, guard,
     dependency, install, callback, package resolution, mutation, visible
     effect. Do not infer a later stage from an earlier log.
   - For UI changes, trace the active rendering layer and validate the actual
     flavor/device path; a source edit alone is not proof of a visible change.

3. Validate behavior at the smallest useful level, then at CI level.
   - Run targeted tests while iterating, then always run the full pre-commit
     suite before a real commit:
     `./gradlew :app:testAllDebugUnitTests`.
   - For Sonar/coverage changes, also run:
     `./gradlew :app:jacocoModernDebugUnitTestReport`.
     These reproduce CI inputs, not SonarCloud's server-side new-code
     baseline or quality gate.
   - Build both debug flavors for shared, flavor, dependency, R8, or hook
     changes:
     `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`.
   - For Compose changes, install the successful Modern debug APK on the active
     device when available and validate the affected flow. Never use `monkey`;
     use an explicit launcher component with `adb shell am start`.
   - Run Android CLI supplemental checks sequentially: `android studio check`,
     then `android studio analyze-file --project=<reported-name> <path>` for
     touched Java/Kotlin files. Treat findings as optimization leads, not as a
     replacement for Gradle, tests, lint, or runtime evidence. Record when the
     bridge/project is unavailable.

4. Audit Sonar boundaries instead of gaming metrics.
   - `sonar.exclusions` removes files from analysis; `sonar.coverage.exclusions`
     removes only coverage accounting. Do not confuse the two.
   - Keep portable stores, parsers, codecs, policy, and other deterministic
     domain logic measurable. Exclude only framework-bound UI, lifecycle,
     Xposed/hooked-process, root, or other code the active JVM harness cannot
     execute.
   - Treat a broad glob such as `app/src/main/java/**/runtime/**` as a review
     item. New pure policy code under an excluded directory should be audited
     for extraction or a narrower rule; do not widen exclusions to make the
     percentage pass.
   - After a merged PR changes the baseline, wait for the main-branch Sonar
     run to complete before opening a boundary-audit PR. Keep that audit
     separate from feature or migration changes.

5. Finish Git safely.
   - Re-read the final diff after all fixes. Stage named files only; never use
     `git add .` or `git add -A` for a scoped change.
   - Use a Conventional Commit subject (`fix:`, `refactor:`, `test:`, etc.)
     that states the outcome. Keep related code, tests, and docs together and
     unrelated changes out.
   - Do not commit if the required full suite is failing unless the user
     explicitly accepts that risk. Do not push, merge, or open a PR unless the
     user asks for it.
   - After a requested push, report the commit hash, branch, verification
     commands, and current GitHub check state. A pending check is not a pass.

## Tool habits

- In Codex, call `rtk` explicitly for compact output when useful; do not assume
  its shell hook is active.
- Use `apply_patch` for manual edits. Preserve UTF-8 without BOM and avoid
  shell/Python write tricks for source files.
- Keep raw logs, reports, credentials, scanner state, APKs, Frida artifacts,
  and temporary probes out of the repository unless intentionally promoted.
- When a check reports only a percentage or a generic failure, inspect the
  actual source/test/coverage report and GitHub/Sonar check details before
  proposing exclusions or small blind patches.

## Completion report

End with a concise record of:

- files and behavior changed;
- tests/builds/Android CLI/device checks run and their result;
- Sonar exclusions or baseline assumptions;
- commit hash and push/PR/check status, if those actions were requested;
- anything not run and why.
