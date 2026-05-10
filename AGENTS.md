# Repository Guidelines

## Project Structure & Module Organization
- Main Android module: `app/` (single-module project; see `settings.gradle.kts`).
- Production code: `app/src/main/java/com/dpis/module/`.
- Resources and UI assets: `app/src/main/res/` and `app/src/main/resources/`.
- Unit tests: `app/src/test/java/com/dpis/module/`.
- Build outputs/logs are generated under `app/build/` and should not be edited manually.
- Documentation:
  - Active docs: `docs/`
  - Historical/archived docs: `docs/archive/`

## Build, Test, and Development Commands
- Build debug APK:
  - `./gradlew :app:assembleDebug`
- Run unit tests:
  - `./gradlew :app:testDebugUnitTest`
- Build then install (PowerShell):
  - `./gradlew :app:assembleDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/debug/app-debug.apk" }`
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
- Run targeted tests during iteration, then run full `:app:testDebugUnitTest` before submitting.
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
