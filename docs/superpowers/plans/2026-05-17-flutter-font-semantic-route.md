# Flutter Font Semantic Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Flutter app font scaling work through runtime semantic hooks, and remove native fingerprint-based Flutter font replacement as a default route.

**Architecture:** Treat Flutter as a semantic rendering domain, not a per-app native patch target. The primary path is to intercept Android-to-Flutter settings traffic and lifecycle boundaries (`flutter/settings`, `FlutterView`, `FlutterJNI`, `Activity`/`ViewRoot`) so the runtime can be told the correct text scale without depending on `libflutter.so` offsets. Native Flutter patching stays only as an explicit fallback/probe path and is no longer the default decision path.

**Tech Stack:** Java 17, C++17, JUnit4, libxposed/LSPosed, Android framework hooks, Gradle.

---

### Task 1: Remove native Flutter fingerprint route from default dispatch

**Files:**
- Modify: `app/src/main/java/com/dpis/module/FontHookArbitration.java`
- Modify: `app/src/main/cpp/dpis_native.cpp`
- Modify: `app/src/test/java/com/dpis/module/HyperOsFlutterFontHookConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void nativeFlutterFingerprintRouteIsNotTheDefaultDispatchPath() throws Exception {
    String source = readSource("src/main/cpp/dpis_native.cpp");
    assertFalse(source.contains("matches_verified_push_style_d11_window"));
    assertFalse(source.contains("VERIFIED_PUSH_STYLE_D11"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.HyperOsFlutterFontHookConfigTest`
Expected: FAIL because the current native file still contains the fingerprint route.

- [ ] **Step 3: Write minimal implementation**

```cpp
// Replace the route resolver with a semantic-only default.
GenericFlutterFontRoute resolve_generic_flutter_font_route(uintptr_t base) {
#if defined(__aarch64__)
    (void) base;
    return GenericFlutterFontRoute::kNone;
#else
    (void) base;
    return GenericFlutterFontRoute::kNone;
#endif
}
```

```java
static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                        boolean fieldRewriteEnabled) {
    if (!fontScaleEnabled) {
        return new FontDomainPlan(false, false, false, false, false, false, false,
                "font-scale-disabled");
    }
    return new FontDomainPlan(false, true, false, false, false, false, true,
            field-rewriteEnabled ? "semantic-font-domain-plan-field-rewrite"
                    : "semantic-font-domain-plan");
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.HyperOsFlutterFontHookConfigTest`
Expected: PASS and no default route references remain in the native runtime path.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/cpp/dpis_native.cpp app/src/main/java/com/dpis/module/FontHookArbitration.java app/src/test/java/com/dpis/module/HyperOsFlutterFontHookConfigTest.java
git commit -m "fix: drop native flutter fingerprint default route"
```

### Task 2: Make Flutter settings rewrite the first-class semantic path

**Files:**
- Modify: `app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- Modify: `app/src/test/java/com/dpis/module/FlutterSettingsFontHookInstallerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void flutterSettingsInstallerTargetsDispatchPlatformMessageForSettingsChannel() throws Exception {
    String source = readSource("src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java");
    assertTrue(source.contains("dispatchPlatformMessage"));
    assertTrue(source.contains("flutter/settings"));
    assertTrue(source.contains("textScaleFactor"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FlutterSettingsFontHookInstallerTest`
Expected: FAIL if the current route is still too broad or only logs without semantic rewrite.

- [ ] **Step 3: Write minimal implementation**

```java
// Keep only the semantic hooks that rewrite flutter/settings or trigger resend.
// Remove any dependency on native route success before the settings rewrite.
private static void hookPlatformMessageDispatch(...){
    // rewrite flutter/settings textScaleFactor here
}
```

```java
// Ensure FlutterView attach/resume paths call resendFlutterUserSettings(...) after attach.
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.FlutterSettingsFontHookInstallerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java app/src/main/java/com/dpis/module/AppProcessHookInstaller.java app/src/test/java/com/dpis/module/FlutterSettingsFontHookInstallerTest.java
git commit -m "feat: prioritize flutter settings semantic font route"
```

### Task 3: Add evidence-first debug logs for bisecting Flutter success

**Files:**
- Modify: `app/src/modern101/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java`
- Modify: `app/src/test/java/com/dpis/module/ModuleMainHookInstallerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void moduleMainContainsBridgeLogsForSemanticFlutterProbe() throws Exception {
    String source = read("src/modern101/java/com/dpis/module/ModuleMain.java");
    assertTrue(source.contains("rawBridgeLog("));
    assertTrue(source.contains("module-loaded app hook probe enter"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleMainHookInstallerTest`
Expected: FAIL until direct bridge logging is in place.

- [ ] **Step 3: Write minimal implementation**

```java
// Use direct XposedBridge logging for probe points only.
// Keep DpisLog for normal behavior, but probe evidence must not depend on it.
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModuleMainHookInstallerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/modern101/java/com/dpis/module/ModuleMain.java app/src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java app/src/test/java/com/dpis/module/ModuleMainHookInstallerTest.java
git commit -m "chore: make flutter probe logs bridge-visible"
```

### Task 4: Validate on device with big-step binary search

**Files:**
- No code changes unless diagnostics reveal a missing semantic hook
- Read only: LSPosed logs, `dumpsys activity top`, Flutter views in `dumpsys`

- [ ] **Step 1: Rebuild and install**

Run: `./gradlew :app:assembleModern101Debug && adb -s d7121fb5 install -r app/build/outputs/apk/modern101/debug/app-modern101-debug.apk`

- [ ] **Step 2: Capture evidence**

Run:
`adb -s d7121fb5 shell su -c 'cat /data/adb/lspd/log/modules_*.log'`
`adb -s d7121fb5 shell dumpsys activity top`

- [ ] **Step 3: Binary-search the semantic boundary**

Check, in order:
1. Did `module-loaded app hook probe enter` appear?
2. Did `target app matched` appear?
3. Did `DPIS_FONT app hook plan` appear?
4. Did `flutter/settings` rewrite logs appear?
5. Did `dumpsys activity top` still show `mCurrentConfig={2.0 ...}` in the target process?

- [ ] **Step 4: Commit evidence-driven fixes only**

If a step is missing, patch the smallest boundary that should have produced it; do not add new native fingerprints.

---

### Coverage Check

- Native fingerprint route removed from default dispatch: Task 1
- Flutter settings semantic rewrite prioritized: Task 2
- Bridge-visible probe logs for binary search: Task 3
- Device validation and narrowing by evidence: Task 4

