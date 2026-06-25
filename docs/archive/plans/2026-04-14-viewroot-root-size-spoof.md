# ViewRootImpl 根尺寸伪装 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 在保留 Configuration/Display 统一伪装结果的前提下，升级到 ViewRootImpl 根尺寸伪装，验证目标 App 的根布局测量是否真正由虚拟窗口像素尺寸驱动。

**架构：** 继续沿用 `virtualWidthDp` 单配置项和 `VirtualDisplayState` 共享结果。`ViewRootProbeHookInstaller` 从单纯日志探针升级为最小伪装器：在 `performTraversals()` 前读取 `mWidth/mHeight`，若与虚拟像素尺寸不一致则写入并记录 before/after；其他链路保持不变，只作为一致性前置层。

**技术栈：** Android SDK (Java), libxposed/api, reflection on framework private fields, Gradle unit tests.

---

### 任务 1： 为根尺寸覆盖辅助方法添加失败测试

**文件：**
- 修改： `app/src/test/java/com/dpis/module/ProbeHookInstallerTest.java`
- 修改： `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java`

- [ ] **步骤 1： 编写失败测试**

添加以下测试：
- `buildPerformTraversalsLog` 使用原始 width/height
- 将根尺寸覆盖应用到 `mWidth/mHeight` 的辅助方法

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

- [ ] **步骤 2：运行 `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ProbeHookInstallerTest` 并确认失败**，因为 `applyRootSizeOverride` 尚不存在。

### 任务 2： 将 ViewRootImpl 探针升级为最小伪装

**文件：**
- 修改： `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java`

- [ ] **步骤 1： 添加辅助方法**

暴露包级可见辅助方法：
- `static boolean applyRootSizeOverride(Object viewRootImpl)`
- `static String buildRootOverrideLog(Object viewRootImpl)`

行为：
- 读取当前 `mWidth/mHeight`
- 读取 `VirtualDisplayState.get()`
- 若覆盖值存在且不同，则将 `mWidth/mHeight` 设为 `widthPx/heightPx`
- 返回是否发生变更

- [ ] **步骤 2： 更新 `performTraversals()` Hook**

在 `chain.proceed()` 前：
- 记录当前尺寸（限次）
- 尝试覆盖
- 若已变更，记录 `ViewRoot override: width=... -> ..., height=... -> ...`
- 然后继续执行

- [ ] **步骤 3：重新运行聚焦测试**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ProbeHookInstallerTest`
预期： 通过.

### 任务 3： 全量验证与 APK 构建

**文件：** 无

- [ ] **步骤 1： 运行验证**

运行： `./gradlew :app:testDebugUnitTest :app:assembleDebug`
预期： 通过.

- [ ] **步骤 2：准备设备检查清单**

安装并重启后，预期新增日志：
- `ViewRoot override: width=1080 -> 900, height=2376 -> 1840`

---

计划已完成并保存至 `docs/superpowers/plans/2026-04-14-viewroot-root-size-spoof.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 我为每个任务分发一个新的子代理，在任务间评审，快速迭代

**2. 内联执行** - 在当前会话内使用 executing-plans 执行任务，按检查点批量推进

选择哪种方式？



