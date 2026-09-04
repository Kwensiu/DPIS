# Compose Presentation Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the pending Compose presentation reclassification with one consistent token and naming model, then validate and commit the coherent migration.

**Architecture:** Keep the public Kotlin package `com.dpis.module.ui.compose` stable while aligning physical files by responsibility under `design`, `dialogs`, `editor`, `interop`, `wear`, and `workspace`. Centralize shared dimensions, animation durations, and layout thresholds in semantic token objects; feature-specific tokens remain in their owning feature packages. Remove obsolete files only after every caller and source smoke test points at the replacement.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Wear Compose, Gradle/JUnit4, Android Studio CLI.

---

### Task 1: Map and normalize presentation ownership

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/*.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/editor/*.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/*.kt`
- Modify: all callers currently shown by `git diff --name-only`
- Test: source smoke tests under `app/src/test/java/com/dpis/module`

- [x] Inventory every old-to-new file move and classify each symbol as design, dialog, editor, interop, wear, or workspace.
- [x] Keep `com.dpis.module.ui.compose` as the compatibility package; remove redundant `Dpis` prefixes only where the owning package already supplies that context.
- [x] Update imports and source smoke tests together; no compatibility aliases or duplicate implementations remain.

### Task 2: Centralize semantic constants

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/Spacing.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/Shapes.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/design/Typography.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/editor/*.kt`
- Modify: `app/src/main/java/com/dpis/module/ui/presentation/workspace/*.kt`

- [x] Replace repeated raw layout values with named constants whose names describe role, such as `WorkspaceRailWidth`, `WorkspaceDrawerWidth`, `EditorRowAnimationDurationMillis`, and `SectionLabelToFirstItemGap`.
- [x] Keep feature-specific values in feature token objects; move only values shared by at least two modules to `design` tokens.
- [x] Preserve stable minimum touch sizes and inset ownership from `CONTEXT.md`; do not change visual semantics while removing literals.

### Task 3: Clean up Android Studio findings

**Files:**
- Modify only files reported by `android studio analyze-file` after Tasks 1-2.

- [x] Resolve safe findings such as redundant explicit types, unnecessary lambda wrappers, missing trailing commas, and replaceable conditionals.
- [x] Leave warnings that would alter Java/Kotlin interop, reflection, Compose state ownership, or runtime behavior unchanged and document them in the final summary.

### Task 4: Validate migration contracts

**Files:**
- Modify: affected `*SourceSmokeTest.kt`, `*LayoutSmokeTest.kt`, and unit tests

- [x] Run `git diff --check` and verify no staged deletion lacks its replacement.
- [x] Run `./gradlew :app:testAllDebugUnitTests`.
- [x] Run `./gradlew :app:assembleModernDebug :app:assembleLegacyDebug`.
- [x] Run `android studio check`, then sequential `analyze-file` for touched Kotlin/Java files.
- [x] Install Modern Debug APK after a successful build and record the result.

### Task 5: Commit the coherent cleanup

**Files:**
- Commit all intended migration, token, naming, and test changes together.

- [ ] Review `git diff --cached` for unrelated files and ensure the plan document is excluded unless explicitly intended.
- [ ] Create a Conventional Commit describing the completed Compose presentation cleanup.
- [ ] Verify `git status --short` and the final commit summary.
