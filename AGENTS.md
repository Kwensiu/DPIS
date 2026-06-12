# Repository Guidelines

## Project Structure & Module Organization
- Main Android module: `app/` (single-module project; see `settings.gradle.kts`).
- Production code: `app/src/main/java/com/dpis/module/`.
- Flavor-specific code: `app/src/modern/java/` (libxposed API 101) and `app/src/legacy/java/` (legacy Xposed API).
- Resources and UI assets: `app/src/main/res/` and `app/src/main/resources/`.
- Flavor-specific assets: `app/src/legacy/assets/` and `app/src/modern/resources/`.
- Unit tests: `app/src/test/java/com/dpis/module/`.
- Build outputs/logs are generated under `app/build/` and should not be edited manually.
- Documentation:
  - Active docs: `docs/`
  - Agent collaboration config: `docs/agents/`
  - Historical/archived docs: `docs/archive/`

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues for `Kwensiu/DPIS`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default Matt Pocock skill label roles unless the repository labels are intentionally changed. See `docs/agents/triage-labels.md`.

### Domain docs

DPIS currently uses a single-context documentation layout. See `docs/agents/domain.md`.

### CodeGraph

Agents may use CodeGraph for project structure navigation, symbol lookup,
callers/callees, and impact analysis. Prefer runtime source under `app/src/**`
for behavioral conclusions; `docs/archive/` may contain historical snapshots
that can appear in CodeGraph results.

### DPIS runtime route playbook

When a task mentions runtime hooks, LSPosed logs, flicker, relaunch, viewport,
font scaling, `system_server`, ActivityThread, Resources, Display, WebView, or
shared route code, use the project-local playbook in
`docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md`. This is a
project-local skill bundle and does not modify global agent skills.

## Build, Test, and Development Commands
- Build debug APKs (both flavors):
  - `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`
- Build release APKs (produces `DPIS_{version}.apk` and `DPIS_{version}_legacy.apk`):
  - `./gradlew :app:assembleRelease`
- Run unit tests:
  - `./gradlew :app:testAllDebugUnitTests`
  - For filtered tests, use a real flavor test task such as `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.ModulePackagePlanTest`.
- Build then install (PowerShell):
  - `./gradlew :app:assembleModernDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/modern/debug/app-modern-debug.apk" }`
- Install debug APKs through Gradle when validating LSPosed module updates:
  - `./gradlew :app:installModernDebug`
  - `./gradlew :app:installLegacyDebug`
  - If installing from Android Studio, disable deployment optimization. LSPosed
    may fail to update the module when optimized deployment is used, leaving
    stale module paths or optimized code active after reinstall/reboot.
- Clean root build directory:
  - `./gradlew Delete`

## Coding Style & Naming Conventions
- Language: Java (target/source 17).
- Indentation: 4 spaces; keep braces and line wrapping consistent with existing files.
- Naming:
  - Classes: `PascalCase` (e.g., `SystemServerMutationPolicy`)
  - Methods/fields: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Keep class responsibilities focused; prefer small helper classes over monolithic installers.
- Do not introduce unnecessary abstractions; follow KISS/YAGNI.

## Testing Guidelines
- Framework: JUnit4 (`testImplementation(libs.junit4)`).
- Test location mirrors production package structure.
- Test naming: `<ClassName>Test.java` and method names describing behavior (e.g., `usesObservedDefaultDensityWhenNoUserValueExists`).
- Run targeted tests during iteration with a flavor test task, then run full `:app:testAllDebugUnitTests` before submitting.
- Before creating a real commit, run the full CI-aligned unit test suite:
  `./gradlew :app:testAllDebugUnitTests`. Do not rely only on targeted tests
  unless the user explicitly agrees to skip full verification; if skipped,
  state that clearly in the final response.
- Prefer behavior tests for parsers, caches, and policy classes. Source smoke tests are acceptable for wiring checks, but should not be the only coverage for business logic.
- When changing UI structure, resource ids, shared binders/helpers, navigation,
  or layout ownership, explicitly check and update source/layout smoke tests.
  In this project the frequent touch points are
  `MainActivitySourceSmokeTest`, `MainActivityLayoutSmokeTest`,
  `AppConfigDialogBinderSourceSmokeTest`, and related `*SourceSmokeTest`
  files. Use `rg` for removed ids/helper names/layouts so tests keep checking
  the current product semantics instead of preserving stale implementation
  details.

## Update Flow Guidelines
- Do not cache update detection, version decisions, or manifest results.
- Release notes body may be cached by version with TTL, but must not affect update availability.
- Network failure must not overwrite already available release notes content.
- Empty release notes body should still be cacheable when the goal is to reduce repeated body fetches.

## Debug-only UI Entrypoints
- Temporary debug-only UI rows must be gated by `BuildConfig.DEBUG`.
- Name ids, strings, and binding methods with `debug_only`.
- Group debug-only rows with their own dividers so release layouts keep static separators.
- Before release-related commits, explicitly decide whether to remove or keep debug-only entries.

## Runtime Debug Automation
- Autofish may be used as an auxiliary Android automation channel for real-device
  DPIS validation. It is for launching apps, coordinate taps/swipes, screenshots,
  top-activity checks, and repeatable visual comparisons.
- Do not treat Autofish accessibility trees as authoritative for apps with limited
  accessibility exposure. These apps may expose only system/status/sidebar nodes,
  so missing in-app text or tab labels in Autofish output does not prove the UI is
  absent or unmodified.
- For DPIS behavior claims, pair Autofish evidence with DPIS/LSPosed logs,
  `dumpsys activity`, or app-process resource metrics. Autofish can confirm what
  is visible and where to tap; it cannot confirm `Configuration`, `ResourcesImpl`,
  `DisplayMetrics`, or system_server mutation state by itself.
- Keep Autofish connection data local. Use `af config set remote.url ...` and
  `af config set remote.token ...` on the agent machine, but do not commit tokens,
  `af.db`, generated screenshots, or `.debug-*` evidence directories.
- Current useful pattern for issue-style validation:
  configure DPIS through the debug-only config entrypoint, use Autofish to launch
  and navigate the target app, then collect screenshots plus adb/LSPosed logs.

## Frida Runtime Probes
- Frida may be used as a read-only Android app-process probe when Xposed rebuilds
  would be too slow. Prefer it for process lists, current Activity/resources,
  View tree metrics, and one-off method return inspection.
- Current Windows user install path (example):
  `$env:APPDATA\Python\Python314\Scripts\frida.exe` and
  `$env:APPDATA\Python\Python314\Scripts\frida-ps.exe`.
- Current device server pattern:
  push `frida-server-<version>-android-arm64` to `/data/local/tmp/frida-server`,
  then run it as root with
  `adb -s <device> shell su 0 sh -c "nohup /data/local/tmp/frida-server >/data/local/tmp/frida.log 2>&1 &"`.
- Example process check:
  `& "$env:APPDATA\Python\Python314\Scripts\frida-ps.exe" -U | Select-String -Pattern 'com.tencent.mm|PID|Name'`.
- Keep Frida artifacts local. Do not commit `.frida/`, downloaded servers,
  temporary Frida scripts, captures, or generated debug evidence unless they are
  intentionally promoted into documented tooling.
- Treat Frida as supporting evidence only. It complements DPIS/LSPosed logs and
  `dumpsys`; it does not replace system_server `ActivityRecord`,
  WindowManager, or DisplayManager evidence.

## Runtime Hook Debugging Discipline
- Distinguish user-facing UI configuration from internal planner and runtime
  domains. Custom font hook-chain overrides apply only to compat/field-rewrite
  font mode; `system_server_font` and `activity_thread_font` are internal
  scheduler domains, not user-customizable chain switches. See
  `docs/font-routing.md`.
- `docs/legacy-runtime-resync.md` and
  `docs/modern-runtime-resync.md` are the DPIS living route documents for
  viewport/runtime hook routes. Before adding, modifying, or removing any
  viewport/runtime hook route, read the relevant document, and read both when
  touching shared code under `app/src/main/java/com/dpis/module/`.
- For runtime diagnosis, follow
  `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md`:
  identify the owning layer first, then prove entry, guard, hook install,
  callback, package resolution, field policy, mutation, and visible effect in
  order.
- `docs/private/` contains app-specific investigation notes and must stay
  uncommitted. Public route documents should record reusable conclusions and
  safe reproduction boundaries, not private device paths, tokens, screenshots,
  or app-specific raw logs.
- Record every new route exploration, route detail adjustment, abandoned
  attempt, and runtime finding in the relevant living route document. Treat
  failed experiments as valuable evidence: do not delete them unless they are
  demonstrably duplicated or misleading; mark them as inactive, superseded, or
  rejected with a short reason.
- Treat `hook ready` as installation evidence only. Require a callback,
  mutation, counter, or visible result before calling a route effective.
- For LSPosed diagnostics (both flavors), use `/data/adb/lspd/log/modules_*.log`
  and `verbose_*.log` as the primary source when proving module entry or
  hook execution. `logcat` is useful for cross-checking forwarded
  `LSPosedFramework` lines, but absence in plain `logcat` is not a reliable
  negative signal. See `docs/lsposed-diagnostics.md` for the pull-and-filter
  path.
- Probe one boundary at a time: entry, guard return, dependency availability,
  hook install, callback hit, and final effect.
- Do not diagnose a later stage until logs prove execution reached that stage.
- Log the source of runtime context used by hooks, such as the classloader or
  process entry point.
- If evidence changes the working theory, update the plan before continuing;
  do not keep executing an outdated hypothesis by inertia.
- Do not add reproduction-target-specific runtime behavior unless explicitly
  required.
- Prefer scheduler or field policy fixes over app-specific recommended route
  lists. Package lists are a late fallback; independent hook routes are later
  still.
- Keep temporary high-volume probes debug-only or remove them before release
  cleanup.
- Font compatibility hook domains are now configured per app through the custom
  chain editor. Do not reintroduce the old global Flutter/HyperOS experimental
  switches into app-process font hook planning; if a supplement route is needed,
  model it as a domain or a documented built-in package default.

## Gradle Task Detection
- Build scripts must not infer release tasks by scanning arbitrary Gradle arguments such as `--tests`.
- Release signing checks should only trigger for actual release task names.

## Commit & Pull Request Guidelines
- Follow Conventional Commit style observed in history:
  - `feat: ...`, `fix: ...`, `chore: ...`, `docs: ...`
- Keep commits scoped and atomic (code + related tests/docs together).
- Before committing, confirm the worktree diff is intentional and full
  `:app:testAllDebugUnitTests` has passed after the final edits. If any
  full-test failure remains, do not commit unless the user explicitly accepts
  the risk for that commit.
- PRs should include:
  - What changed and why
  - Verification steps/commands executed
  - Screenshots or logs for UI/runtime behavior changes when relevant
  - Linked issue/task if available
