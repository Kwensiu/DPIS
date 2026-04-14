# Remote Preferences DPI Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 `libxposed/service` 建立最小 remote preferences 配置链路，让 Hook 侧按“用户值优先，否则首次观测默认值”计算 DPI，并在原生只读状态页展示当前配置值、默认值和生效值。

**Architecture:** 模块 App 通过 `XposedServiceHelper` 获取 `XposedService`，初始化并读取 framework 侧 remote preferences；Hook 进程继续沿用现有 Hook 点，但改为从 remote preferences 读取每包配置，并在第一次观测到系统默认 DPI 时回填。状态页只读取同一份配置数据进行展示，不提供编辑能力。

**Tech Stack:** Android SDK (Java), libxposed/api, libxposed/service, Gradle unit tests, native Android resources/layouts.

---

### Task 1: Add failing tests for config resolution and default fallback

**Files:**
- Create: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- Modify: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.dpis.module;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DpiConfigStoreTest {
    @Test
    public void parsesConfiguredPackageSetFromStoredStrings() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putStringSet("target_packages", new LinkedHashSet<>(Arrays.asList(
                "com.max.xiaoheihe", "bin.mt.plus.canary"))).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("com.example.other"));
    }

    @Test
    public void resolvesEffectiveDensityFromUserValueFirst() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("dpi.bin.mt.plus.canary.default", 480).putInt("dpi.bin.mt.plus.canary.user", 560).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(560, store.getEffectiveDensityDpi("bin.mt.plus.canary"));
    }

    @Test
    public void resolvesEffectiveDensityFromDefaultWhenUserMissing() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("dpi.bin.mt.plus.canary.default", 480).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(480, store.getEffectiveDensityDpi("bin.mt.plus.canary"));
        assertNull(store.getUserDensityDpi("bin.mt.plus.canary"));
    }

    @Test
    public void recordsFirstObservedDefaultOnlyOnce() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.recordDefaultDensityIfAbsent("bin.mt.plus.canary", 480));
        assertFalse(store.recordDefaultDensityIfAbsent("bin.mt.plus.canary", 560));
        assertEquals(Integer.valueOf(480), store.getDefaultDensityDpi("bin.mt.plus.canary"));
    }
}
```

- [ ] **Step 2: Run the new tests and observe failure**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: FAIL because `DpiConfigStore` and `FakePrefs` do not exist yet.

- [ ] **Step 3: Add one failing Hook regression test for default fallback**

```java
@Test
public void usesObservedDefaultDensityWhenNoUserValueExists() {
    Configuration config = new Configuration();
    config.densityDpi = 480;
    config.fontScale = 1.0f;
    DisplayMetrics metrics = new DisplayMetrics();
    metrics.densityDpi = 480;
    metrics.density = 3.0f;
    metrics.scaledDensity = 3.0f;

    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);

    ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

    assertEquals(480, metrics.densityDpi);
    assertEquals(Integer.valueOf(480), store.getDefaultDensityDpi("bin.mt.plus.canary"));
}
```

- [ ] **Step 4: Run `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest` and confirm failure** because the new overload does not exist.

### Task 2: Implement remote-preference-backed config store

**Files:**
- Create: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Create: `app/src/test/java/com/dpis/module/FakePrefs.java`
- Modify: `app/src/main/java/com/dpis/module/DpiConfig.java`
- Modify: `app/src/main/java/com/dpis/module/DpisApplication.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add `libxposed/service` dependency and provider support**

```kotlin
dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    testImplementation(libs.junit4)
}
```

Manifest merge should include the `XposedProvider` from the library; `DpisApplication` remains the application entry.

- [ ] **Step 2: Implement `DpiConfigStore` as a thin wrapper around `SharedPreferences`**

```java
final class DpiConfigStore {
    static final String GROUP = "dpi_config";
    static final String KEY_TARGET_PACKAGES = "target_packages";

    private final SharedPreferences preferences;

    DpiConfigStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    Set<String> getConfiguredPackages() { ... }
    Integer getUserDensityDpi(String packageName) { ... }
    Integer getDefaultDensityDpi(String packageName) { ... }
    Integer getEffectiveDensityDpi(String packageName) { ... }
    boolean recordDefaultDensityIfAbsent(String packageName, int densityDpi) { ... }
    void ensureTargetPackages(Set<String> packageNames) { ... }
}
```

Use keys:
- `target_packages`
- `dpi.<package>.user`
- `dpi.<package>.default`

- [ ] **Step 3: Keep `DpiConfig` focused on static seed packages only**

```java
static final String[] TARGET_PACKAGES = {
        "bin.mt.plus.canary",
        "com.max.xiaoheihe"
};

static Set<String> getSeedTargetPackages() {
    return new LinkedHashSet<>(Arrays.asList(TARGET_PACKAGES));
}
```

Do not keep `TARGET_DENSITY_DPI` as the runtime source of truth anymore.

- [ ] **Step 4: Register Xposed service in `DpisApplication` and initialize remote preferences when binder arrives**

```java
public final class DpisApplication extends Application implements XposedServiceHelper.OnServiceListener {
    private static volatile XposedService xposedService;
    private static volatile DpiConfigStore configStore;

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        xposedService = service;
        DpiConfigStore store = new DpiConfigStore(service.getRemotePreferences(DpiConfigStore.GROUP));
        store.ensureTargetPackages(DpiConfig.getSeedTargetPackages());
        configStore = store;
    }

    @Override
    public void onServiceDied(XposedService service) {
        if (xposedService == service) {
            xposedService = null;
            configStore = null;
        }
    }

    static DpiConfigStore getConfigStore() {
        return configStore;
    }
}
```

- [ ] **Step 5: Re-run the new tests and ensure `DpiConfigStoreTest` passes**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: PASS.

### Task 3: Switch Hook logic to remote preferences without changing hook points

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`

- [ ] **Step 1: Add a minimal runtime accessor that Hook code can use**

`ModuleMain` should keep a per-process `DpiConfigStore` initialized from `getRemotePreferences(DpiConfigStore.GROUP)` during `onModuleLoaded()` or lazily before package handling.

- [ ] **Step 2: Gate package handling by configured package set**

```java
DpiConfigStore store = getOrCreateConfigStore();
if (!store.getConfiguredPackages().contains(param.getPackageName())) {
    return;
}
```

- [ ] **Step 3: Update the hook helpers to compute effective density from the store**

```java
static void applyDensityOverride(String packageName, Configuration config, DisplayMetrics metrics, DpiConfigStore store) {
    if (config == null) return;
    int originalDpi = config.densityDpi;
    store.recordDefaultDensityIfAbsent(packageName, originalDpi);
    Integer effective = store.getEffectiveDensityDpi(packageName);
    int targetDpi = effective != null ? effective : originalDpi;
    ...
}
```

The existing hook points stay the same:
- `ResourcesManager.applyConfigurationToResources(...)`
- `ResourcesImpl.updateConfiguration(...)`

- [ ] **Step 4: Re-run the updated Hook tests and full unit suite**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
Expected: PASS.

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

### Task 4: Update the read-only status page to show config/default/effective values

**Files:**
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`
- Modify: `app/src/main/res/layout/activity_status.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Change the target section into a per-package status block**

Each package row should show:
- `Package: <name>`
- `Current config: <user value or 未设置>`
- `Current default: <default value or 未观测>`
- `Current effective: <effective value or 未观测>`

- [ ] **Step 2: Build the package summary from the app-side `DpiConfigStore` when available, otherwise fall back to the static seed package list with empty values**

```java
private String buildTargetText() {
    DpiConfigStore store = DpisApplication.getConfigStore();
    Set<String> packages = store != null ? store.getConfiguredPackages() : DpiConfig.getSeedTargetPackages();
    ...
}
```

- [ ] **Step 3: Run `./gradlew :app:assembleDebug`**

Expected: PASS.

### Task 5: Final verification and APK build

**Files:** None

- [ ] **Step 1: Run the full verification commands**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: PASS.

- [ ] **Step 2: Review the diff and working tree**

Run: `git status -sb`
Expected: only the planned files changed.

- [ ] **Step 3: Prepare device verification instructions**

Use:
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

Confirm logs show first-observed default recording and effective DPI resolution for `bin.mt.plus.canary` and `com.max.xiaoheihe`.

---

Plan complete and saved to `docs/superpowers/plans/2026-04-14-remote-preferences-dpi-config.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
