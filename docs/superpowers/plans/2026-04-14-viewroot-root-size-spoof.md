# ViewRootImpl Root Size Spoof Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留 Configuration/Display 统一伪装结果的前提下，升级到 ViewRootImpl 根尺寸伪装，验证目标 App 的根布局测量是否真正由虚拟窗口像素尺寸驱动。

**Architecture:** 继续沿用 `virtualWidthDp` 单配置项和 `VirtualDisplayState` 共享结果。`ViewRootProbeHookInstaller` 从单纯日志探针升级为最小伪装器：在 `performTraversals()` 前读取 `mWidth/mHeight`，若与虚拟像素尺寸不一致则写入并记录 before/after；其他链路保持不变，只作为一致性前置层。

**Tech Stack:** Android SDK (Java), libxposed/api, reflection on framework private fields, Gradle unit tests.

---

### Task 1: Add failing tests for root size override helpers

**Files:**
- Modify: `app/src/test/java/com/dpis/module/ProbeHookInstallerTest.java`
- Modify: `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java`

- [ ] **Step 1: Write failing tests**

Add tests for:
- `buildPerformTraversalsLog` with original width/height
- helper that applies root size override to `mWidth/mHeight`

```java
@Test
public void appliesVirtualRootSizeToFakeViewRoot() {
    VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300, 576, 900, 1840));
    FakeViewRoot root = new FakeViewRoot(1080, 2376);

    assertTrue(ViewRootProbeHookInstaller.applyRootSizeOverride(root));
    assertEquals(900, root.mWidth);
    assertEquals(1840, root.mHeight);
}
```

- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ProbeHookInstallerTest` and confirm failure** because `applyRootSizeOverride` does not exist yet.

### Task 2: Upgrade ViewRootImpl probe into minimal spoof

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java`

- [ ] **Step 1: Add helper methods**

Expose package-private helpers:
- `static boolean applyRootSizeOverride(Object viewRootImpl)`
- `static String buildRootOverrideLog(Object viewRootImpl)`

Behavior:
- read current `mWidth/mHeight`
- read `VirtualDisplayState.get()`
- if override exists and differs, set `mWidth/mHeight` to `widthPx/heightPx`
- return whether a change was made

- [ ] **Step 2: Update `performTraversals()` hook**

Before `chain.proceed()`:
- log current size (limited count)
- attempt override
- if changed, log `ViewRoot override: width=... -> ..., height=... -> ...`
- then proceed

- [ ] **Step 3: Re-run focused tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ProbeHookInstallerTest`
Expected: PASS.

### Task 3: Full verification and APK build

**Files:** None

- [ ] **Step 1: Run verification**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: PASS.

- [ ] **Step 2: Prepare device checklist**

After install and relaunch, expected new log:
- `ViewRoot override: width=1080 -> 900, height=2376 -> 1840`

---

Plan complete and saved to `docs/superpowers/plans/2026-04-14-viewroot-root-size-spoof.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
