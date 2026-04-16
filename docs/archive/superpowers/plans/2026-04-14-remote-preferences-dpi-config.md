# 远程偏好 DPI 配置 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 用 `libxposed/service` 建立最小 remote preferences 配置链路，让 Hook 侧按“用户值优先，否则首次观测默认值”计算 DPI，并在原生只读状态页展示当前配置值、默认值和生效值。

**架构：** 模块 App 通过 `XposedServiceHelper` 获取 `XposedService`，初始化并读取 framework 侧 remote preferences；Hook 进程继续沿用现有 Hook 点，但改为从 remote preferences 读取每包配置，并在第一次观测到系统默认 DPI 时回填。状态页只读取同一份配置数据进行展示，不提供编辑能力。

**技术栈：** Android SDK (Java), libxposed/api, libxposed/service, Gradle unit tests, native Android resources/layouts.

---

### 任务 1： 为配置解析与默认回退添加失败测试

**文件：**
- 新增： `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- 修改： `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **步骤 1： 编写失败测试**

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

- [ ] **步骤 2： 运行新测试并观察失败**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
预期： 失败 因为 `DpiConfigStore` 和 `FakePrefs` 尚不存在。

- [ ] **步骤 3： 添加一个用于默认回退场景的失败 Hook 回归测试**

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

- [ ] **步骤 4： 运行 `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest` 并确认失败**，因为新的重载尚不存在。

### 任务 2： 实现基于 remote preference 的配置存储

**文件：**
- 新增： `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- 新增： `app/src/test/java/com/dpis/module/FakePrefs.java`
- 修改： `app/src/main/java/com/dpis/module/DpiConfig.java`
- 修改： `app/src/main/java/com/dpis/module/DpisApplication.java`
- 修改： `app/src/main/AndroidManifest.xml`
- 修改： `app/build.gradle.kts`

- [ ] **步骤 1： 添加 `libxposed/service` 依赖与 provider 支持**

```kotlin
dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    testImplementation(libs.junit4)
}
```

Manifest 合并后应包含库中的 `XposedProvider`；`DpisApplication` 仍作为应用入口。

- [ ] **步骤 2： 将 `DpiConfigStore` 实现为 `SharedPreferences` 的轻量封装**

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

使用以下键：
- `target_packages`
- `dpi.<package>.user`
- `dpi.<package>.default`

- [ ] **步骤 3： 让 `DpiConfig` 仅关注静态种子包**

```java
static final String[] TARGET_PACKAGES = {
        "bin.mt.plus.canary",
        "com.max.xiaoheihe"
};

static Set<String> getSeedTargetPackages() {
    return new LinkedHashSet<>(Arrays.asList(TARGET_PACKAGES));
}
```

不再将 `TARGET_DENSITY_DPI` 作为运行时真值来源。

- [ ] **步骤 4： 在 `DpisApplication` 注册 Xposed service，并在 binder 到达时初始化远程偏好**

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

- [ ] **步骤 5：重新运行新测试并确保 `DpiConfigStoreTest` 通过**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
预期： 通过.

### 任务 3： 在不改 Hook 点的前提下切换为 remote preferences 逻辑

**文件：**
- 修改： `app/src/main/java/com/dpis/module/ModuleMain.java`
- 修改： `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- 修改： `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`

- [ ] **步骤 1： 添加一个供 Hook 代码使用的最小运行时访问器**

`ModuleMain` 应维护进程级 `DpiConfigStore`，在 `onModuleLoaded()` 阶段或包处理前懒加载时通过 `getRemotePreferences(DpiConfigStore.GROUP)` 初始化。

- [ ] **步骤 2： 按配置包集合对包处理进行门控**

```java
DpiConfigStore store = getOrCreateConfigStore();
if (!store.getConfiguredPackages().contains(param.getPackageName())) {
    return;
}
```

- [ ] **步骤 3： 更新 Hook 辅助方法，改为从 store 计算生效 density**

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

现有 Hook 点保持不变：
- `ResourcesManager.applyConfigurationToResources(...)`
- `ResourcesImpl.updateConfiguration(...)`

- [ ] **步骤 4： 重新运行更新后的 Hook 测试和完整单测套件**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
预期： 通过.

运行： `./gradlew :app:testDebugUnitTest`
预期： 通过.

### 任务 4： 更新只读状态页以展示配置值/默认值/生效值

**文件：**
- 修改： `app/src/main/java/com/dpis/module/MainActivity.java`
- 修改： `app/src/main/res/layout/activity_status.xml`
- 修改： `app/src/main/res/values/strings.xml`

- [ ] **步骤 1： 将目标区块改为按包展示的状态块**

每个包行应展示：
- `包名：<name>`
- `当前配置：<user value or 未设置>`
- `当前默认值：<default value or 未观测>`
- `当前生效值：<effective value or 未观测>`

- [ ] **步骤 2： 优先从应用侧 `DpiConfigStore` 构建包摘要；不可用时回退到静态种子包列表并填空值**

```java
private String buildTargetText() {
    DpiConfigStore store = DpisApplication.getConfigStore();
    Set<String> packages = store != null ? store.getConfiguredPackages() : DpiConfig.getSeedTargetPackages();
    ...
}
```

- [ ] **步骤 3：运行 `./gradlew :app:assembleDebug`**

预期： 通过.

### 任务 5： 最终验证与 APK 构建

**文件：** 无

- [ ] **步骤 1：运行完整验证命令**

运行： `./gradlew :app:testDebugUnitTest :app:assembleDebug`
预期： 通过.

- [ ] **步骤 2：检查差异与工作树**

运行： `git status -sb`
预期： 只有计划内文件发生变更。

- [ ] **步骤 3：准备设备验证说明**

使用：
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

确认日志展示了 `bin.mt.plus.canary` 和 `com.max.xiaoheihe` 的“首次观测默认值记录”与“生效 DPI 解析”。

---

计划已完成并保存至 `docs/superpowers/plans/2026-04-14-remote-preferences-dpi-config.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 我为每个任务分发一个新的子代理，在任务间评审，快速迭代

**2. 内联执行** - 在当前会话内使用 executing-plans 执行任务，按检查点批量推进

选择哪种方式？



