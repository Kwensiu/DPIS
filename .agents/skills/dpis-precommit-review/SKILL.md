---
name: dpis-precommit-review
description: Review DPIS changes before committing or opening a pull request, including scope, domain-rule compliance, tests, Android validation, SonarQube MCP checks, and Git hygiene.
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
   - Java-to-Kotlin on the touched set: if the diff materially changes an
     existing Java class whose responsibility sits in the Kotlin/Compose
     ownership boundary, migrate that class to Kotlin in the same change.
     Skip only files that are unsafe or too complex to convert here:
     reflection, JNI, flavor Xposed entrypoints, externally observed JVM
     signatures, or a conversion that would expand far beyond the current
     ownership boundary. Record that reason in the change; do not leave a
     convertible Java file for a later cleanup when this review already
     touched it.
   - `MainActivity.java` slim-down: if `MainActivity.java` is in the diff,
     audit any newly touched or newly exposed workspace/feature logic nested
     in it (editor sessions, dialogs, coordinators, exporters, diagnostics,
     or other feature state machines). Extract that work into focused
     classes under `app/src/main/java/com/dpis/module/` in the same change.
     Keep `MainActivity` limited to app-shell startup and event wiring. Do
     not grow nested workspace workflows there, even as a temporary host.
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
     That is only the local coverage XML CI uploads. After PR Check has
     published an analysis, read the Cloud result through the SonarQube MCP
     (see below). Do not curl Sonar REST or scrape GitHub check logs for the
     same facts.
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

4. Read Sonar through MCP; audit boundaries instead of gaming metrics.
   - Project key: `Kwensiu_DPIS`. MCP talks to the already-uploaded Cloud
     analysis. It does not run the scanner.
   - On a PR: `list_pull_requests`, then pass that Sonar PR key to
     `get_project_quality_gate_status`, `get_component_measures` (`new_coverage`,
     `new_violations`), `search_sonar_issues_in_projects` (`inNewCodePeriod`),
     and `search_files_by_coverage` / `get_file_coverage_details`. Never pass a
     git branch name as `pullRequest`. Do not set both `branch` and
     `pullRequest`.
   - Off a PR: `list_branches` (`LONG` for main) and use `branch`.
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
   - After a merged PR changes the baseline, wait for the main-branch analysis
     (MCP `list_branches` / quality gate on `main`) before opening a
     boundary-audit PR. Keep that audit separate from feature or migration
     changes.

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
  local test/coverage report and the SonarQube MCP result before proposing
  exclusions or small blind patches.

## Completion report

End with a concise record of:

- files and behavior changed;
- tests/builds/Android CLI/device checks run and their result;
- SonarQube MCP quality-gate / new-code measures, plus any exclusion changes;
- commit hash and push/PR/check status, if those actions were requested;
- anything not run and why.
