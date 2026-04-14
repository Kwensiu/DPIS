# Virtual Viewport Spoofing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前 per-app 目标 DPI 配置改为 per-app 虚拟 dp 视口配置，让目标应用在不改变物理屏幕参数的前提下，看到一个更小的可用 dp 视口，从而使内容整体显得更大并触发布局断点变化。

**Architecture:** 保留 `ResourcesManager.applyConfigurationToResources(...)` 与 `ResourcesImpl.updateConfiguration(...)` 两个主 Hook 点，在进入资源链路时优先改写 `Configuration.screenWidthDp`、`screenHeightDp`、`smallestScreenWidthDp`，必要时同步 compat 字段。配置页只暴露两个固定包的“虚拟宽度 dp”输入框，并根据当前 `Configuration` 的纵横比在运行时推导高度 dp 与 smallest width。

**Tech Stack:** Android SDK (Java), libxposed/api, libxposed/service remote preferences, Gradle unit tests, native Android UI.

---

### Task 1: Add failing tests for virtual viewport calculation

**Files:**
- Create: `app/src/test/java/com/dpis/module/ViewportOverrideTest.java`
- Modify: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: Write failing tests for viewport derivation and config storage**

```java
@Test
public void derivesHeightAndSmallestWidthFromConfiguredWidth() {
    Configuration config = new Configuration();
    config.screenWidthDp = 600;
    config.screenHeightDp = 1000;
    config.smallestScreenWidthDp = 600;

    ViewportOverride.Result result = ViewportOverride.derive(config, 360);

    assertEquals(360, result.widthDp);
    assertEquals(600, result.heightDp);
    assertEquals(360, result.smallestWidthDp);
}

@Test
public void storesVirtualWidthDpPerPackage() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps());

    store.setTargetViewportWidthDp("bin.mt.plus.canary", 360);

    assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
}
```

- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.DpiConfigStoreTest` and confirm failure** because `ViewportOverride` and the new store APIs do not exist yet.

### Task 2: Implement viewport config model and storage

**Files:**
- Create: `app/src/main/java/com/dpis/module/ViewportOverride.java`
- Modify: `app/src/main/java/com/dpis/module/DpiConfig.java`
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Modify: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: Add a focused helper that derives target viewport dimensions from current configuration**

```java
final class ViewportOverride {
    static final class Result {
        final int widthDp;
        final int heightDp;
        final int smallestWidthDp;
    }

    static Result derive(Configuration config, int targetWidthDp) { ... }
    static void apply(Configuration config, Result result) { ... }
}
```

Rules:
- `targetWidthDp <= 0` means disabled
- `heightDp` is derived by preserving the current `screenHeightDp / screenWidthDp` ratio
- `smallestWidthDp = min(widthDp, heightDp)`
- also update compat fields when present

- [ ] **Step 2: Replace seed/runtime key model from `dpi.<pkg>.target` to `viewport.<pkg>.width_dp`**

`DpiConfigStore` should expose:
- `Integer getTargetViewportWidthDp(String packageName)`
- `void setTargetViewportWidthDp(String packageName, int widthDp)`
- `void clearTargetViewportWidthDp(String packageName)`
- `Integer getEffectiveViewportWidthDp(String packageName)`
- `void ensureSeedConfig(Map<String, Integer> seedViewportWidthDps)`

- [ ] **Step 3: Set new seed values in `DpiConfig`**

Use one seed virtual width value for the two fixed packages, for example:

```java
static final int SEED_TARGET_VIEWPORT_WIDTH_DP = 360;
```

- [ ] **Step 4: Re-run the focused tests and ensure they pass**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: PASS.

### Task 3: Switch Hook logic from density-first to viewport-first

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Modify: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: Add failing tests for configuration dp field override**

Add tests that verify:
- configured virtual width changes `screenWidthDp`
- derived `screenHeightDp` follows ratio
- `smallestScreenWidthDp` updates
- disabled config leaves values unchanged

- [ ] **Step 2: Update `ModuleMain` logging and package gating to use viewport config**

Expected log shape:

```java
DpisLog.i("target package matched: package=" + packageName
        + ", targetViewportWidthDp=" + targetWidthDp);
```

- [ ] **Step 3: In `ResourcesManager` hook, apply viewport override before proceeding**

Use `ViewportOverride.derive(config, targetWidthDp)` then write back:
- `config.screenWidthDp`
- `config.screenHeightDp`
- `config.smallestScreenWidthDp`
- compat counterparts where appropriate

- [ ] **Step 4: In `ResourcesImpl` hook, ensure configuration fields remain synchronized after upstream updates**

Keep the existing density synchronization code only as a secondary consistency layer if needed, but the primary observable change must be the viewport dp fields.

- [ ] **Step 5: Re-run the hook tests and full unit suite**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
Expected: PASS.

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

### Task 4: Update the native config page for virtual width editing

**Files:**
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`
- Modify: `app/src/main/res/layout/activity_status.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Change labels from DPI to virtual width dp**

Each package section should show:
- current virtual width
- derived effective viewport summary when config is present
- input hint such as `例如 360` or `留空禁用`

- [ ] **Step 2: Update save handlers to write viewport width instead of target DPI**

```java
store.setTargetViewportWidthDp(packageName, widthDp);
```

- [ ] **Step 3: Improve wording to reduce PoC/testing tone and add initialization status**

Show whether remote preferences are initialized, and clarify that saving changes requires killing/restarting the target app process.

- [ ] **Step 4: Run `./gradlew :app:assembleDebug`**
Expected: PASS.

### Task 5: Final verification and APK build

**Files:** None

- [ ] **Step 1: Run the full verification command**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: PASS.

- [ ] **Step 2: Build the device verification checklist**

Use:
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

Expected logs should mention `targetViewportWidthDp=<value>` and viewport-related overrides.

---

Plan complete and saved to `docs/superpowers/plans/2026-04-14-virtual-viewport-spoofing.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
