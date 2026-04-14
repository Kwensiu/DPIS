# WindowMetrics and Display Spoof Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保持 per-app `virtualWidthDp` 单配置项不变的前提下，同时伪装 `Configuration`、`WindowMetrics` 和 `Display` 结果，让目标应用更接近“运行在更小屏幕上”，而不修改物理屏幕全局状态。

**Architecture:** 继续使用 `virtualWidthDp` 作为唯一输入。运行时从当前 `Configuration.screenWidthDp` 计算 dp 缩放比例，用这个比例同时导出两类结果：1) viewport 配置结果（widthDp/heightDp/smallestWidthDp + densityDpi）；2) window/display 像素结果（widthPx/heightPx）。Hook 层在 `ResourcesManager` / `ResourcesImpl` 中继续覆盖配置，在 `WindowMetrics.getBounds()` 与 `Display.getMetrics/getRealMetrics/getSize/getRealSize` 中覆盖窗口和显示尺寸。

**Tech Stack:** Android SDK (Java), libxposed/api, libxposed/service, Gradle unit tests, reflection for hidden/final Android framework fields.

---

### Task 1: Add failing tests for unified viewport/display derivation

**Files:**
- Create: `app/src/test/java/com/dpis/module/VirtualDisplayOverrideTest.java`
- Modify: `app/src/test/java/com/dpis/module/ViewportOverrideTest.java`

- [ ] **Step 1: Write failing tests**

Add tests that verify a single `virtualWidthDp` produces:
- smaller `widthDp/heightDp/smallestWidthDp`
- higher `densityDpi`
- smaller `widthPx/heightPx`

```java
@Test
public void derivesWindowPixelSizeFromViewportRatio() {
    VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
            360, 736, 480, 1080, 2208, 300);

    assertEquals(300, result.widthDp);
    assertEquals(613, result.heightDp);
    assertEquals(300, result.smallestWidthDp);
    assertEquals(576, result.densityDpi);
    assertEquals(900, result.widthPx);
    assertEquals(1840, result.heightPx);
}
```

- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests com.dpis.module.VirtualDisplayOverrideTest` and confirm failure** because `VirtualDisplayOverride` does not exist yet.

### Task 2: Implement shared virtual display derivation helpers

**Files:**
- Create: `app/src/main/java/com/dpis/module/VirtualDisplayOverride.java`
- Modify: `app/src/main/java/com/dpis/module/ViewportOverride.java`
- Modify: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: Add `VirtualDisplayOverride` helper**

It should derive a unified result from:
- source width/height dp
- source densityDpi
- source width/height px
- target virtual width dp

The result should include:
- `widthDp`
- `heightDp`
- `smallestWidthDp`
- `densityDpi`
- `widthPx`
- `heightPx`

Rules:
- `dp` height preserves original dp ratio
- `densityDpi` scales by `sourceWidthDp / targetWidthDp`
- `px` width/height scale by `targetWidthDp / sourceWidthDp`

- [ ] **Step 2: Make `ViewportOverride` delegate to the shared helper** so configuration and display spoof use the same math.

- [ ] **Step 3: Re-run focused tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.VirtualDisplayOverrideTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
Expected: PASS.

### Task 3: Hook WindowMetrics and Display results

**Files:**
- Create: `app/src/main/java/com/dpis/module/WindowMetricsHookInstaller.java`
- Create: `app/src/main/java/com/dpis/module/DisplayHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`

- [ ] **Step 1: Add failing helper tests for pixel result application**

Test helper methods that mutate:
- `DisplayMetrics.widthPixels/heightPixels/densityDpi/density/scaledDensity`
- `Point.x/y`
- `Rect`

- [ ] **Step 2: Hook `android.view.WindowMetrics.getBounds()`**

Return a new `Rect` derived from the unified result, scaled from the original bounds.

- [ ] **Step 3: Hook `android.view.Display` methods**

Prioritize:
- `getMetrics(DisplayMetrics)`
- `getRealMetrics(DisplayMetrics)`
- `getSize(Point)`
- `getRealSize(Point)`

Mutate out-params in place using the unified result.

- [ ] **Step 4: Register both installers from `ModuleMain` after the existing resources hooks**

Expected log summary should mention:
- ResourcesManager
- ResourcesImpl
- WindowMetrics
- Display

### Task 4: Align UI and logging with stage-two semantics

**Files:**
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Update labels to explain that one value now drives configuration, window bounds, and display metrics**

- [ ] **Step 2: Add a short note that changes require killing/restarting the target app**

- [ ] **Step 3: Keep UI to one field per package (`virtualWidthDp`)**

### Task 5: Final verification and APK build

**Files:** None

- [ ] **Step 1: Run the full verification command**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: PASS.

- [ ] **Step 2: Prepare device verification checklist**

Use:
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

Expected logs should include:
- viewport overrides (`widthDp/heightDp/smallestWidthDp/densityDpi`)
- window bounds overrides
- display metric overrides

---

Plan complete and saved to `docs/superpowers/plans/2026-04-14-windowmetrics-display-spoof.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
