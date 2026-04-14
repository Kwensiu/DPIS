# ResourcesImpl Hook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a ResourcesImpl `updateConfiguration` hook that mirrors the existing ResourcesManager override and keeps configuration/DisplayMetrics density values in sync before the original method runs.

**Architecture:** A new installer reflects into `ResourcesImpl`, intercepts `updateConfiguration`, and reuses `DensityOverride` logic via a helper that is exercisable by unit tests. `ModuleMain` invokes both installers for the target package.

**Tech Stack:** Java, Android SDK (`Configuration`, `DisplayMetrics`), Xposed interface, Gradle unit testing.

---

### Task 1: Exercise the density override helper via tests

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Create: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: Write the failing tests**  
  Add `ResourcesImplHookInstallerTest` with two focused cases:

  ```java
  package com.dpis.module;

  import android.content.res.Configuration;
  import android.util.DisplayMetrics;

  import org.junit.Test;

  import static org.junit.Assert.assertEquals;

  public class ResourcesImplHookInstallerTest {
      @Test
      public void configurationDensityOverridesWhenMetricsNull() {
          Configuration config = new Configuration();
          config.densityDpi = 320;
          config.fontScale = 1.1f;
          ResourcesImplHookInstaller.applyDensityOverride(config, null);
          assertEquals(DpiConfig.TARGET_DENSITY_DPI, config.densityDpi);
      }

      @Test
      public void displayMetricsFieldsUpdatedWhenPresent() {
          Configuration config = new Configuration();
          config.densityDpi = 320;
          config.fontScale = 1.25f;
          DisplayMetrics metrics = new DisplayMetrics();
          metrics.densityDpi = 320;
          metrics.density = 2.0f;
          metrics.scaledDensity = 2.5f;
          ResourcesImplHookInstaller.applyDensityOverride(config, metrics);
          assertEquals(DpiConfig.TARGET_DENSITY_DPI, metrics.densityDpi);
          assertEquals(DensityOverride.densityFromDpi(DpiConfig.TARGET_DENSITY_DPI), metrics.density, 0.0001f);
          assertEquals(DensityOverride.scaledDensityFrom(DpiConfig.TARGET_DENSITY_DPI, config.fontScale), metrics.scaledDensity, 0.0001f);
      }
  }
  ```

- [ ] **Step 2: Run the new tests and observe failure**  
  Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
  Expected: FAIL (compiler error because `applyDensityOverride` is currently private and the test cannot access it).

- [ ] **Step 3: Make the helper accessible and implement the density syncing logic**  
  Update `ResourcesImplHookInstaller` so `applyDensityOverride` is package-private and contains the full override logic:

  ```java
  static void applyDensityOverride(Configuration config, DisplayMetrics metrics) {
      if (config == null) {
          return;
      }
      int originalDpi = config.densityDpi;
      int targetDpi = DensityOverride.resolveDensityDpi(DpiConfig.TARGET_DENSITY_DPI, originalDpi);
      if (targetDpi == originalDpi) {
          return;
      }
      config.densityDpi = targetDpi;
      float targetDensity = DensityOverride.densityFromDpi(targetDpi);
      float targetScaledDensity = DensityOverride.scaledDensityFrom(targetDpi, config.fontScale);
      if (metrics != null) {
          metrics.densityDpi = targetDpi;
          metrics.density = targetDensity;
          metrics.scaledDensity = targetScaledDensity;
      }
      DpisLog.i("override densityDpi in ResourcesImpl: "
              + originalDpi + " -> " + targetDpi
              + ", density=" + targetDensity
              + ", scaledDensity=" + targetScaledDensity);
  }
  ```

- [ ] **Step 4: Re-run the tests and ensure they pass**  
  Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
  Expected: PASS.

- [ ] **Step 5: Commit the helper/test changes**  
  ```bash
  git add app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
  git commit -m "test: add ResourcesImpl density helper tests"
  ```

### Task 2: Install the new hook during package readiness

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`

- [ ] **Step 1: Update `ModuleMain.onPackageReady` to register both installers**  
  Replace the existing block with:

  ```java
  try {
      ResourcesManagerHookInstaller.install(this);
      ResourcesImplHookInstaller.install(this);
      DpisLog.i("installed ResourcesManager and ResourcesImpl hooks for " + param.getPackageName());
  } catch (Throwable throwable) {
      DpisLog.e("failed to install Resources hooks", throwable);
  }
  ```

- [ ] **Step 2: Run the full unit test suite to ensure no regressions**  
  Run: `./gradlew :app:testDebugUnitTest`  
  Expected: PASS; verifies the new tests plus existing ones succeed with the updated code.

- [ ] **Step 3: Commit the hook installation changes**  
  ```bash
  git add app/src/main/java/com/dpis/module/ModuleMain.java
  git commit -m "feat: register ResourcesImpl hook alongside ResourcesManager"
  ```

### Task 3: Final verification

**Files:** None (commands only)

- [ ] **Step 1: Run lint/format check if needed**  
  Run: `./gradlew :app:check` (optional but catches regressions).

- [ ] **Step 2: Confirm working tree status**  
  Run: `git status -sb`

- [ ] **Step 3: Prepare summary for review**  
  Review `git diff` to ensure all changes align with the spec.

---

Plan complete and saved to `docs/superpowers/plans/2026-04-14-resourcesimpl-hook-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** – dispatch a fresh subagent per task via `superpowers:subagent-driven-development`, review between tasks.
2. **Inline Execution** – proceed in this session using `superpowers:executing-plans`, batching tasks with checkpoints.

Which approach?
