# Repository Guidelines

## Project Structure & Module Organization
- Main Android module: `app/` (single-module project; see `settings.gradle.kts`).
- Production code: `app/src/main/java/com/dpis/module/`.
- Flavor-specific code: `app/src/modern101/java/` (libxposed API 101) and `app/src/compat100/java/` (legacy Xposed API).
- Resources and UI assets: `app/src/main/res/` and `app/src/main/resources/`.
- Flavor-specific assets: `app/src/compat100/assets/` and `app/src/modern101/resources/`.
- Unit tests: `app/src/test/java/com/dpis/module/`.
- Build outputs/logs are generated under `app/build/` and should not be edited manually.
- Documentation:
  - Active docs: `docs/`
  - Historical/archived docs: `docs/archive/`

## Build, Test, and Development Commands
- Build debug APKs (both flavors):
  - `./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`
- Build release APKs (produces `DPIS_{version}.apk` and `DPIS_{version}_legacy.apk`):
  - `./gradlew :app:assembleRelease`
- Run unit tests:
  - `./gradlew :app:testAllDebugUnitTests`
  - For filtered tests, use a real flavor test task such as `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModulePackagePlanTest`.
- Build then install (PowerShell):
  - `./gradlew :app:assembleModern101Debug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk" }`
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
- Prefer behavior tests for parsers, caches, and policy classes. Source smoke tests are acceptable for wiring checks, but should not be the only coverage for business logic.

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

## Gradle Task Detection
- Build scripts must not infer release tasks by scanning arbitrary Gradle arguments such as `--tests`.
- Release signing checks should only trigger for actual release task names.

## Commit & Pull Request Guidelines
- Follow Conventional Commit style observed in history:
  - `feat: ...`, `fix: ...`, `chore: ...`, `docs: ...`
- Keep commits scoped and atomic (code + related tests/docs together).
- PRs should include:
  - What changed and why
  - Verification steps/commands executed
  - Screenshots or logs for UI/runtime behavior changes when relevant
  - Linked issue/task if available
