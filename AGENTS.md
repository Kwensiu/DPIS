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

### Project-level compliance

- `AGENTS.md`, `CONTEXT.md`, active documents under `docs/`, and any task-specific
  project playbook are binding implementation and review constraints. Before
  editing, identify the applicable rules; after editing, audit the complete
  diff against them. Do not knowingly leave a violation for a later cleanup.
- This applies to all rules, not only testing: package/file ownership, Compose
  and Kotlin conventions, Java interoperability boundaries, state ownership,
  runtime-route semantics, inset ownership, naming, comments, and validation
  requirements must be corrected in the same change when the touched scope
  exposes a violation.
- Keep physical directories aligned with responsibility. Do not leave new
  presentation code in a catch-all package when an existing feature/design,
  editor, workspace, dialog, interop, or Wear boundary applies. Keep Kotlin
  packages stable during a pure physical reorganization unless package changes
  are required and all callers/tests are updated.
- Do not hide a known violation behind compatibility aliases, duplicated
  implementations, or stale tests. Remove obsolete names and implementation
  anchors when the owning code is renamed or refactored.
- Names must describe the responsibility and semantic role of the value or
  function. Avoid vague helpers, duplicated type/feature prefixes, and names
  that confuse layout, content inset, clipping, or interaction ownership.

### Issue tracker

Issues are tracked in GitHub Issues for `Kwensiu/DPIS`. See `docs/agents/issue-tracker.md`.

### Domain docs

DPIS currently uses a single-context documentation layout. See
`docs/agents/domain.md`.
Read `CONTEXT.md` for DPIS domain language, product semantics, file-role
boundaries, and appearance before changing package state rules, runtime-route
meaning, app-list status semantics, log page behavior, feedback diagnostic
packaging, or UI colors and shared chrome.

### CodeGraph

Before editing shared code, use CodeGraph for callers, callees, and impact. Treat `app/src/**` as
the behavioral source. Results from `docs/archive/` are historical and are not current behavior.

### Tooling

Before committing or opening a PR, use `.agents/skills/dpis-precommit-review/SKILL.md`.

### Sub-agent usage

Reuse an existing sub-agent for the same task or feature. Start a new one only when the previous one
is overloaded, the task domain has changed, or shared context would pollute the result. For a large
diff, slice by ownership and use one read-only reviewer per slice. The split rules live in
`.agents/skills/dpis-precommit-review/SKILL.md`.

### DPIS runtime route playbook

When a task mentions runtime hooks, LSPosed logs, flicker, relaunch, viewport,
font scaling, `system_server`, ActivityThread, Resources, Display, WebView, or
shared route code, use the project-local playbook in
`docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md`. This is a
project-local skill bundle and does not modify global agent skills.

### DPIS hook API routing

When a task mentions Legacy Xposed hooks, libxposed hooks, API 101 or 102
capability gating, modern/legacy flavor hook wiring, or fallback behavior
between old and new framework capabilities, use the project-local playbook in
`docs/agents/skills/dpis-hook-api-routing/SKILL.md`. This is a project-local
skill bundle and does not modify global agent skills.

### DPIS localization playbook

When a task mentions translations, localization, l10n, locale resource files,
language selector entries, `AppLocaleManager`, Crowdin, machine translation,
or localization review, use the project-local playbook in
`docs/agents/skills/dpis-localization/SKILL.md`. This is a project-local skill
bundle and does not modify global agent skills.

### DPIS release notes

When a task mentions release notes, changelog text, 更新日志, 发布说明, or
release-please output for users, use
`.agents/skills/dpis-release-notes/SKILL.md`. This is a project-local skill
bundle and does not modify global agent skills.

Do not add translation text for locales other than English and Simplified
Chinese unless the user explicitly requests that locale's content.

### DPIS HyperOS smoke

When a task needs HyperOS Gallery/Weather (or similar native/Rust/Flutter)
device evidence for dp/font emulation, use
`.agents/skills/dpis-hyperos-smoke/SKILL.md`. This is a project-local skill
bundle and does not modify global agent skills.

## Build, Test, and Development Commands

The complete pre-commit sequence is in `.agents/skills/dpis-precommit-review/SKILL.md`. The main
quick commands are:

- `./gradlew :app:testAllDebugUnitTests`
- `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`
- `./gradlew :app:assembleRelease`

For device/module installation, use the corresponding Gradle install task and
disable Android Studio deployment optimization so LSPosed does not retain stale
module paths or optimized code.

## Coding Style & Naming Conventions

- Create new code and test files in Kotlin by default. Java 17 remains the
  compatibility target for existing Java sources and JVM bytecode.
- DPIS has completed its main Compose UI migration. Build new screens,
  dialogs, sheets, list rows, navigation surfaces, and reusable controls with
  Jetpack Compose and Kotlin; do not add new XML/View UI unless an external API
  makes Compose impractical and the exception is documented in the change.
- When a task materially changes an existing Java class, migrate that class to
  Kotlin when the conversion stays within the task's ownership boundary. Keep
  Java-callable signatures stable with tools such as `@JvmStatic`, explicit
  interfaces, and nullable types where existing Java callers require them.
- Do not turn a focused change into a repository-wide language migration.
  Flavor-specific Xposed entrypoints, reflection-sensitive hooks, JNI-facing
  code, or classes with externally observed JVM signatures may remain Java
  until their interoperability contract can be verified explicitly.
- Kotlin uses the existing project formatting conventions and trailing commas
  for multiline declarations/calls. Java continues to use 4-space indentation
  and the surrounding brace/wrapping style.
- Naming:
  - Classes: `PascalCase` (e.g., `SystemServerMutationPolicy`)
  - Methods/fields: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Choose concise, specific names. Do not repeat package, feature, or type
  context already clear at the declaration site.
- Keep class responsibilities focused; prefer small helper classes over monolithic installers.
- Keep `MainActivity` limited to essential app-shell startup and event wiring.
  Do not place feature workflows, dialogs, coordinators, exporters,
  diagnostics, or feature-specific state machines there; put them in focused
  classes under `app/src/main/java/com/dpis/module/`.
- Treat Compose as the presentation and interaction layer. Hoist durable state
  and business/runtime work into focused coordinators, presenters, stores, or
  ViewModels; composables should not become alternate owners of package state,
  runtime-route decisions, persistence, or long-running I/O.
- Standalone Compose secondary pages must use the shared
  `SecondaryPageScaffold` so title typography, status-bar ownership, content
  insets, haptics, and the circular back button remain consistent. Use
  `SecondaryPageTopBar` directly only when a specialized scaffold is required;
  do not hand-roll a plain `TopAppBar` or `CenterAlignedTopAppBar` for standard
  secondary navigation.
- Preserve the adaptive Compose baseline across phone, tablet, landscape, and
  compact/round watch layouts. New navigation and bottom surfaces must handle
  cutouts and colored safe areas explicitly while allowing scroll content to
  render beneath gesture insets where the existing shell permits it.
- Do not introduce unnecessary abstractions; follow KISS/YAGNI.

## Testing Guidelines

- Framework: JUnit4 (`testImplementation(libs.junit4)`).
- Test location mirrors production package structure.
- Test class names end in `Test` and method names describe behavior (e.g.,
  `usesObservedDefaultDensityWhenNoUserValueExists`). Use the matching `.kt`
  or `.java` extension for the implementation language.
- The required suite, flavor builds, and Sonar checks are in
  `.agents/skills/dpis-precommit-review/SKILL.md`.
- Prefer behavior tests for parsers, caches, and policy classes. Source smoke tests are acceptable for wiring checks, but should not be the only coverage for business logic.
- JVM coverage exclusions are a directory contract, decided when the file is
  created, not after Sonar fails. Put JVM-untestable code under
  `**/presentation/**`, `ui/**`, `runtime/**`, `root/**`, or a flavor tree
  (`legacy/**`, `modern/**`). Put stores, parsers, codecs, and policy next to
  other measurable domain code and write a behavior test in the same change.
  Do not append a file to `sonar.coverage.exclusions` to make the gate pass.
  A per-file exclusion means the file is in the wrong directory: move it or
  extract the policy. Android framework types (`*Activity`, `*Service`,
  `*Receiver`, `Application`) may be excluded by those suffixes because the
  JVM harness cannot construct them. Do not invent extra suffix globs
  (`*Session`, `*Confirm`, `*Handler`, `*Binder`, `*Shell`) to hide coupling.
- Tests must pin user-visible behavior, domain invariants, or stable module contracts. Do not add tests that merely repeat a source line, method name, literal value, file path, or implementation detail unless that detail is itself an intentional compatibility contract.
- Keep source smoke tests sparse. When changing UI structure, resource ids, shared binders,
  navigation, or layout ownership, update the related `*SourceSmokeTest` files, including
  `MainActivitySourceSmokeTest`, `MainActivityLayoutSmokeTest`, and
  `AppConfigDialogBinderSourceSmokeTest`. After Java-to-Kotlin, assert the current `.kt` semantics,
  not the old Java spelling.
- For Compose UI changes, run the relevant unit/source smoke tests, build the
  Modern Debug APK, and install it on the active device after a successful
  build. For shared Java/Kotlin interoperability, dependency, R8, or flavor
  changes, validate both flavors and run the release shrink path when signing
  configuration is available.

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

## Log Page & Feedback Diagnostics

- Read `CONTEXT.md` before changing log page behavior, feedback diagnostic flow,
  or the roles of `diagnostic.txt`, `dpis-log.txt`, and `lsposed-log.txt`.
- When changing diagnostic package structure, log parsing, export file names, or
  result sheet file cards, update `ExportBuilderTest` and the
  related source/layout smoke tests so they assert the current diagnostic
  semantics.

## Android Runtime Validation

- Do not use `monkey` to launch target applications, either from ADB automation or
  DPIS runtime logic. On some systems it can unexpectedly enable automatic
  rotation. Prefer an explicit launcher intent or resolved launcher component;
  for ADB validation, use `adb shell am start` with the target package/component.

## Frida Runtime Probes

- Frida may be used as a read-only app-process probe when an Xposed rebuild would be too slow.
- Do not commit Frida artifacts, and do not encode a machine-specific path in project docs.
- Frida does not replace `system_server` evidence from `ActivityRecord`, WindowManager, or
  DisplayManager.

## Runtime Hook Debugging Discipline

- Read `CONTEXT.md` before changing runtime-route meaning, evidence rules, or
  route-effectiveness criteria.
- `docs/legacy-runtime-resync.md` and `docs/modern-runtime-resync.md` are the
  living route documents. Read the relevant one before adding, modifying, or
  removing a viewport/runtime hook route, and read both when touching shared
  code under `app/src/main/java/com/dpis/module/`.
- Record new route exploration, adjustments, abandoned attempts, and runtime
  findings in the relevant living document.
- Follow `docs/agents/skills/dpis-runtime-route-diagnose/SKILL.md` for the diagnosis order.
- For LSPosed diagnostics, the framework's module and verbose logs are the primary source. Absence
  in plain `logcat` is not a reliable negative signal. See `docs/lsposed-diagnostics.md`.
- Keep temporary high-volume probes debug-only or remove them before release cleanup.

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
