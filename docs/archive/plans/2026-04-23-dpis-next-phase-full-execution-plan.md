# DPIS Next-Phase Full Execution Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a stable, test-covered, and release-ready DPIS iteration that improves maintainability of per-app display controls (virtual width + font size) without changing existing user-facing behavior unexpectedly.

**Architecture:** Keep a single-module Android app structure and continue source-first smoke verification for critical UI and hook entry paths. Changes should be incremental, with each task proving behavior through targeted tests before broader integration. Release confidence is built via unit tests, debug build verification, and documented manual validation checkpoints.

**Tech Stack:** Java 17, Android XML resources, Gradle, JUnit4, LSPosed/Xposed runtime assumptions.

---

## 0. Baseline and Guardrails

**Current Focus Files (high-priority):**
- `app/src/main/java/com/dpis/module/MainActivity.java`
- `app/src/main/java/com/dpis/module/ModuleMain.java`
- `app/src/main/java/com/dpis/module/*HookInstaller*.java`
- `app/src/main/res/layout/activity_status.xml`
- `app/src/main/res/layout/dialog_*.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/java/com/dpis/module/*SmokeTest.java`

**Branch & Validation Rules:**
- Always implement by small vertical slices (test -> code -> verify).
- No direct edits to generated outputs in `app/build/`.
- Run targeted tests during each task, then full suite before closeout.

---

### Task 1: Stabilize Critical UI Wiring via Smoke Tests

**Files:**
- Modify: `app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java`
- Modify: `app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java`

- [ ] **Step 1: Write failing assertions for critical list/help wiring**

```java
@Test
public void mainActivityRetainsHelpFabWiring() throws IOException {
    String source = read("src/main/java/com/dpis/module/MainActivity.java");
    String layout = read("src/main/res/layout/activity_status.xml");

    assertTrue(source.contains("help_fab"));
    assertTrue(layout.contains("@+id/help_fab"));
}
```

- [ ] **Step 2: Run target tests and confirm baseline**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.MainActivityLayoutSmokeTest`
Expected: PASS (or FAIL only for real regressions to fix in Step 3).

- [ ] **Step 3: Fix only minimal wiring regressions if found**

Scope: keep existing UX unchanged; update only broken references or handlers.

- [ ] **Step 4: Re-run target tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivitySourceSmokeTest --tests com.dpis.module.MainActivityLayoutSmokeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java app/src/main/java/com/dpis/module/MainActivity.java app/src/main/res/layout/activity_status.xml
git commit -m "test: stabilize main activity critical UI wiring"
```

### Task 2: Harden Per-App Config Read/Write Path

**Files:**
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Modify: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: Add failing tests for invalid/missing values fallback behavior**

```java
@Test
public void fallsBackToSafeDefaultsWhenStoredConfigInvalid() {
    // Arrange invalid persisted values and assert defaults are returned.
}
```

- [ ] **Step 2: Run target test class**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: FAIL before implementation.

- [ ] **Step 3: Implement minimal fallback + bounds normalization in store layer**

Scope: normalize width/font ranges and return defaults on parse/read failures.

- [ ] **Step 4: Re-run target tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/DpiConfigStore.java app/src/test/java/com/dpis/module/DpiConfigStoreTest.java
git commit -m "fix: harden dpi config store fallback and normalization"
```

### Task 3: Align Hook Installer Behavior with Explicit Policy Checks

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/SystemServerMutationPolicy.java`
- Modify: `app/src/test/java/com/dpis/module/*HookInstallerTest.java`

- [ ] **Step 1: Add failing tests for policy-guarded hook entry behavior**

```java
@Test
public void skipsHighRiskSystemServerPathWhenSafetyModeEnabled() {
    // Assert guarded path is not installed when safety mode is on.
}
```

- [ ] **Step 2: Run focused hook tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dpis.module.*HookInstaller*"`
Expected: FAIL before implementation.

- [ ] **Step 3: Implement minimal policy gating in entry installer path**

Scope: avoid broad refactor; keep existing public behavior and only enforce intended policy boundaries.

- [ ] **Step 4: Re-run focused hook tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dpis.module.*HookInstaller*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/ModuleMain.java app/src/main/java/com/dpis/module/AppProcessHookInstaller.java app/src/main/java/com/dpis/module/SystemServerMutationPolicy.java app/src/test/java/com/dpis/module
git commit -m "fix: enforce safety-mode policy in hook installers"
```

### Task 4: Resource & Copy Consistency Pass (No UX Redesign)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/dialog_*.xml`
- Modify: `app/src/test/java/com/dpis/module/*LayoutSmokeTest.java`

- [ ] **Step 1: Add failing smoke checks for key strings and dialog ids**

```java
@Test
public void criticalDialogIdsAndStringsRemainResolvable() throws IOException {
    // Assert key IDs and string names used by dialog flows still exist.
}
```

- [ ] **Step 2: Run target smoke tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dpis.module.*LayoutSmokeTest"`
Expected: FAIL before implementation (if drift exists).

- [ ] **Step 3: Apply minimal resource key/layout id alignment fixes**

Scope: only consistency fixes; do not redesign styles or interaction flow.

- [ ] **Step 4: Re-run target smoke tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.dpis.module.*LayoutSmokeTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout app/src/test/java/com/dpis/module
git commit -m "fix: align dialog resources and layout smoke contracts"
```

### Task 5: Full Verification and Release Candidate Checklist

**Files:**
- Modify: `docs/final-validation-checklist-2026-04-17.md` (append current run record)
- Create: `docs/reports/2026-04-23-next-phase-validation.md`

- [ ] **Step 1: Run full unit test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Build debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Optional device install verification (if adb device available)**

Run: `./gradlew :app:assembleDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/debug/app-debug.apk" }`
Expected: install success or explicit environment-not-ready note.

- [ ] **Step 4: Write validation report and checklist update**

Include command summary, decisive output lines, and unresolved risk list.

- [ ] **Step 5: Commit**

```bash
git add docs/final-validation-checklist-2026-04-17.md docs/reports/2026-04-23-next-phase-validation.md
git commit -m "docs: add next-phase verification report"
```

---

## Milestones

- M1: Critical UI wiring locked by smoke tests.
- M2: Per-app config path safe against malformed data.
- M3: Hook entry behavior aligned with explicit safety policy.
- M4: Resources/layout contracts consistent and test-guarded.
- M5: Full test/build validation completed and documented.

## Risks and Mitigations

- Runtime hook behavior differs across ROM/Android versions.
  - Mitigation: keep policy checks centralized and covered by unit tests + optional device pass.
- Resource key drift causes runtime crash in dialogs.
  - Mitigation: lock IDs/keys in smoke tests.
- Over-refactor risk in hook installer layer.
  - Mitigation: enforce minimal diff per task and frequent commits.

## Done Criteria

- `:app:testDebugUnitTest` passes.
- `:app:assembleDebug` succeeds.
- Validation report committed with exact commands and outcomes.
- No unresolved blocker in hook policy, config store, or critical dialog wiring.
