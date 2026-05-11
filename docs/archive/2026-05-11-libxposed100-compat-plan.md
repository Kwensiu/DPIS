# libxposed API 100 Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an isolated `compat100` build path that can experiment with libxposed API 100 without weakening the current API 101 module.

**Architecture:** Keep the current API 101 module as the default path. Add an Android product flavor split with variant-specific Xposed metadata and entry classes, then compile the compat path against a local API 100 compile-only stub module because `io.github.libxposed:api:100` is not resolvable from configured Maven repositories.

**Tech Stack:** Android Gradle Plugin, Java 17, JUnit4, libxposed API 101, local libxposed API 100 stub, LSPosed/Xposed `META-INF/xposed` metadata.

> **Archived:** This plan was superseded by the current implementation which uses the traditional Xposed `IXposedHookLoadPackage` API instead of libxposed API 100 stubs. See `app/src/compat100/` for the actual implementation.

---

### Task 1: Split Modern Xposed Metadata by Flavor

**Files:**
- Modify: `app/build.gradle.kts`
- Move: `app/src/main/resources/META-INF/xposed/module.prop` to `app/src/modern101/resources/META-INF/xposed/module.prop`
- Move: `app/src/main/resources/META-INF/xposed/java_init.list` to `app/src/modern101/resources/META-INF/xposed/java_init.list`
- Move: `app/src/main/resources/META-INF/xposed/native_init.list` to `app/src/modern101/resources/META-INF/xposed/native_init.list`
- Move: `app/src/main/resources/META-INF/xposed/scope.list` to `app/src/modern101/resources/META-INF/xposed/scope.list`
- Test: `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`
- Test: `app/src/test/java/com/dpis/module/ModuleMainHookInstallerTest.java`

- [ ] **Step 1: Write failing tests**

Add assertions that modern metadata lives under `src/modern101/resources/META-INF/xposed` and that no shared `src/main/resources/META-INF/xposed/module.prop` remains.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.LegacyModuleManifestMetadataTest --tests com.dpis.module.ModuleMainHookInstallerTest`

Expected: tests fail because metadata still lives under `src/main/resources`.

- [ ] **Step 3: Add flavors and move metadata**

Add flavor dimension `xposedApi`, create `modern101` and `compat100` flavors, and move the current metadata into `modern101`.

- [ ] **Step 4: Run tests and build modern debug**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.LegacyModuleManifestMetadataTest --tests com.dpis.module.ModuleMainHookInstallerTest`

Run: `.\gradlew :app:assembleModern101Debug`

Expected: both commands pass.

### Task 2: Move API 101 Entry to Modern Source Set

**Files:**
- Move: `app/src/main/java/com/dpis/module/ModuleMain.java` to `app/src/modern101/java/com/dpis/module/ModuleMain.java`
- Test: `app/src/test/java/com/dpis/module/ModuleMainHookInstallerTest.java`
- Test: `app/src/test/java/com/dpis/module/LegacySmokeModuleFilesTest.java`
- Test: `app/src/test/java/com/dpis/module/FontDebugStatsProviderSourceSmokeTest.java`

- [ ] **Step 1: Write failing tests**

Update source smoke tests to read `src/modern101/java/com/dpis/module/ModuleMain.java` and assert `src/main/java/com/dpis/module/ModuleMain.java` is absent.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleMainHookInstallerTest --tests com.dpis.module.LegacySmokeModuleFilesTest --tests com.dpis.module.FontDebugStatsProviderSourceSmokeTest`

Expected: tests fail because `ModuleMain.java` still lives under `main`.

- [ ] **Step 3: Move `ModuleMain.java`**

Move the API 101 entry class into the `modern101` source set.

- [ ] **Step 4: Run modern tests and build**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleMainHookInstallerTest --tests com.dpis.module.LegacySmokeModuleFilesTest --tests com.dpis.module.FontDebugStatsProviderSourceSmokeTest`

Run: `.\gradlew :app:assembleModern101Debug`

Expected: both commands pass.

### Task 3: Add API 100 Compile-Only Stub and Compat Entry Skeleton

**Files:**
- Modify: `settings.gradle.kts`
- Create: `libxposed-api100-stub/build.gradle.kts`
- Create: `libxposed-api100-stub/src/main/AndroidManifest.xml`
- Create: `libxposed-api100-stub/src/main/java/io/github/libxposed/api/XposedModule.java`
- Create: `libxposed-api100-stub/src/main/java/io/github/libxposed/api/XposedModuleInterface.java`
- Create: `libxposed-api100-stub/src/main/java/io/github/libxposed/api/XposedInterface.java`
- Create: `libxposed-api100-stub/src/main/java/io/github/libxposed/api/XposedContext.java`
- Create: `libxposed-api100-stub/src/main/java/io/github/libxposed/api/XposedContextWrapper.java`
- Create: `app/src/compat100/java/com/dpis/module/Compat100ModuleMain.java`
- Create: `app/src/compat100/resources/META-INF/xposed/module.prop`
- Create: `app/src/compat100/resources/META-INF/xposed/java_init.list`
- Create: `app/src/compat100/resources/META-INF/xposed/scope.list`
- Test: `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`

- [ ] **Step 1: Write failing tests**

Add assertions that compat metadata declares `minApiVersion=100`, `targetApiVersion=100`, and `java_init.list` points at `com.dpis.module.Compat100ModuleMain`.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.LegacyModuleManifestMetadataTest`

Expected: tests fail because compat metadata does not exist yet.

- [ ] **Step 3: Add stub module, variant dependencies, and compat skeleton**

Compile `modern101` against `libs.libxposed.api` and `compat100` against `project(":libxposed-api100-stub")`. The first compat entry should only log/load state and should not install app hooks yet.

- [ ] **Step 4: Build both variants**

Run: `.\gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`

Expected: both variants compile. If shared hook installer code does not compile against API 100, keep it out of the compat entry until Task 4 introduces an adapter.

### Task 4: Extract Shared Module Load Coordinator

**Files:**
- Create: `app/src/main/java/com/dpis/module/ModuleLoadCoordinator.java`
- Modify: `app/src/modern101/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/compat100/java/com/dpis/module/Compat100ModuleMain.java`
- Test: `app/src/test/java/com/dpis/module/ModuleLoadCoordinatorTest.java`

- [ ] **Step 1: Write failing tests**

Test package gating and configured-package decisions without depending on libxposed callback types.

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleLoadCoordinatorTest`

Expected: test class or coordinator is missing.

- [ ] **Step 3: Extract shared logic**

Move callback-independent decision logic out of `ModuleMain` into `ModuleLoadCoordinator`. Keep Xposed API calls in the flavor-specific entries.

- [ ] **Step 4: Run focused tests and build both variants**

Run: `.\gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleLoadCoordinatorTest --tests com.dpis.module.ModuleMainHookInstallerTest`

Run: `.\gradlew :app:assembleModern101Debug :app:assembleCompat100Debug`

Expected: both variants pass/compile.

### Task 5: Optional Device Smoke

**Files:**
- No source edits expected.

- [ ] **Step 1: Connect to device**

Run: `adb connect 192.168.5.100:5555`

Expected: adb reports connected or already connected.

- [ ] **Step 2: Install compat APK**

Run: `adb install -r app/build/outputs/apk/compat100/debug/app-compat100-debug.apk`

Expected: install succeeds.

- [ ] **Step 3: Collect basic logs**

Run: `adb logcat -d | Select-String -Pattern "DPIS|Compat100|libxposed|Xposed"`

Expected: log output confirms whether the compat entry is loaded by the framework.
