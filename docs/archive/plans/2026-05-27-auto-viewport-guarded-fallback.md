# Auto Viewport Guarded Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make viewport `auto` system-first without treating an unavailable system route as an implicit compat selection. App-process derivation is reserved for guarded runtime evidence that proves the system route failed after it was expected to run.

**Architecture:** Keep requested mode and execution route separate. `auto` remains system-preferred in mode resolution. Missing system capability resolves to `off`, while guarded app-process fallback only applies when marker/record evidence is present and clearly invalid; an empty marker is treated as pending system evidence, not as a fallback trigger.

**Tech Stack:** Android Java, libxposed API 101, legacy Xposed API 100, JUnit4.

---

### Task 1: Keep Auto App-Process Hooks Installed

**Files:**
- Modify: `app/src/main/java/com/dpis/module/HookExecutionPlanner.java`
- Test: `app/src/test/java/com/dpis/module/HookExecutionPlannerTest.java`

- [x] Change `auto` execution planning so resources hooks are installed even when the resolved preference is `system`.
- [x] Assert that `auto + system hooks on` still reports `resolvedViewportMode=system` but has viewport/resources hooks enabled.

### Task 2: Gate Auto Fallback By Runtime Evidence

**Files:**
- Modify: `app/src/main/java/com/dpis/module/TargetViewportWidthResolver.java`
- Test: `app/src/test/java/com/dpis/module/TargetViewportWidthResolverTest.java`

- [x] Add a no-target result for display-scoped sources when no current system marker or existing record is available.
- [x] Keep explicit `compat` and explicit `system` behavior unchanged.
- [x] Keep already-target and marker-hit paths from applying a second transform.
- [x] Treat `empty` marker reads as pending system evidence; only malformed, stale, mismatch, or too-long marker reads count as clear fallback signals.

### Task 3: Preserve Same-Package Secondary Process Support

**Files:**
- Modify: `app/src/modern/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/legacy/java/com/dpis/module/LegacyModuleHook.java`
- Test: `app/src/test/java/com/dpis/module/ModuleMainHookInstallerTest.java`
- Test: `app/src/test/java/com/dpis/module/LegacyModuleHookSourceTest.java`

- [x] Allow `package:process` secondary processes to keep viewport hooks.
- [x] Continue suppressing non-package-owned secondary process viewport routes.

### Task 4: Verify

**Files:**
- No code changes.

- [x] Run targeted tests:
  `./gradlew :app:testModernDebugUnitTest --tests com.dpis.module.HookExecutionPlannerTest --tests com.dpis.module.TargetViewportWidthResolverTest --tests com.dpis.module.ModuleMainHookInstallerTest --tests com.dpis.module.LegacyModuleHookSourceTest --tests com.dpis.module.ViewportModePolicyTest`
- [x] If code compiles and tests pass, build/install only when runtime validation is needed.
