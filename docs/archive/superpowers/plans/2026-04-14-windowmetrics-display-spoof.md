# WindowMetrics 与 Display 伪装 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 在保持 per-app `virtualWidthDp` 单配置项不变的前提下，同时伪装 `Configuration`、`WindowMetrics` 和 `Display` 结果，让目标应用更接近“运行在更小屏幕上”，而不修改物理屏幕全局状态。

**架构：** 继续使用 `virtualWidthDp` 作为唯一输入。运行时从当前 `Configuration.screenWidthDp` 计算 dp 缩放比例，用这个比例同时导出两类结果：1) viewport 配置结果（widthDp/heightDp/smallestWidthDp + densityDpi）；2) window/display 像素结果（widthPx/heightPx）。Hook 层在 `ResourcesManager` / `ResourcesImpl` 中继续覆盖配置，在 `WindowMetrics.getBounds()` 与 `Display.getMetrics/getRealMetrics/getSize/getRealSize` 中覆盖窗口和显示尺寸。

**技术栈：** Android SDK (Java), libxposed/api, libxposed/service, Gradle unit tests, reflection for hidden/final Android framework fields.

---

### 任务 1： 为统一 viewport/display 推导添加失败测试

**文件：**
- 新增： `app/src/test/java/com/dpis/module/VirtualDisplayOverrideTest.java`
- 修改： `app/src/test/java/com/dpis/module/ViewportOverrideTest.java`

- [ ] **步骤 1： 编写失败测试**

添加测试以验证单个 `virtualWidthDp` 会产生：
- 更小的 `widthDp/heightDp/smallestWidthDp`
- 更高的 `densityDpi`
- 更小的 `widthPx/heightPx`

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

- [ ] **步骤 2： 运行 `./gradlew :app:testDebugUnitTest --tests com.dpis.module.VirtualDisplayOverrideTest` 并确认失败**，因为 `VirtualDisplayOverride` 尚不存在。

### 任务 2： 实现共享虚拟显示推导辅助方法

**文件：**
- 新增： `app/src/main/java/com/dpis/module/VirtualDisplayOverride.java`
- 修改： `app/src/main/java/com/dpis/module/ViewportOverride.java`
- 修改： `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **步骤 1： 添加 `VirtualDisplayOverride` 辅助类**

它应从以下输入推导统一结果：
- 源宽/高 dp
- 源 densityDpi
- 源宽/高 px
- 目标虚拟宽度 dp

结果应包含：
- `widthDp`
- `heightDp`
- `smallestWidthDp`
- `densityDpi`
- `widthPx`
- `heightPx`

规则：
- `dp` 高度保持原始 dp 比例
- `densityDpi` 按 `sourceWidthDp / targetWidthDp` 缩放
- `px` 宽/高按 `targetWidthDp / sourceWidthDp` 缩放

- [ ] **步骤 2： 让 `ViewportOverride` 委托给共享辅助类**，使 configuration 与 display 伪装使用同一套计算。

- [ ] **步骤 3：重新运行聚焦测试**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.VirtualDisplayOverrideTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
预期： 通过.

### 任务 3：挂钩 WindowMetrics 与 Display 结果

**文件：**
- 新增： `app/src/main/java/com/dpis/module/WindowMetricsHookInstaller.java`
- 新增： `app/src/main/java/com/dpis/module/DisplayHookInstaller.java`
- 修改： `app/src/main/java/com/dpis/module/ModuleMain.java`

- [ ] **步骤 1： 添加像素结果应用的失败辅助测试**

测试会修改以下对象的辅助方法：
- `DisplayMetrics.widthPixels/heightPixels/densityDpi/density/scaledDensity`
- `Point.x/y`
- `Rect`

- [ ] **步骤 2： Hook `android.view.WindowMetrics.getBounds()`**

返回一个由统一结果推导的新 `Rect`，并按原始 bounds 比例缩放。

- [ ] **步骤 3： Hook `android.view.Display` 方法**

优先覆盖：
- `getMetrics(DisplayMetrics)`
- `getRealMetrics(DisplayMetrics)`
- `getSize(Point)`
- `getRealSize(Point)`

使用统一结果就地修改 out-params。

- [ ] **步骤 4： 在现有 resources hooks 之后，由 `ModuleMain` 注册两个 installer**

预期日志摘要应包含：
- ResourcesManager
- ResourcesImpl
- WindowMetrics
- Display

### 任务 4： 使 UI 与日志对齐二阶段语义

**文件：**
- 修改： `app/src/main/java/com/dpis/module/MainActivity.java`
- 修改： `app/src/main/res/values/strings.xml`

- [ ] **步骤 1： 更新标签，说明单个值现在同时驱动 configuration、window bounds 和 display metrics**

- [ ] **步骤 2： 补充简短说明：变更需要 kill/restart 目标应用**

- [ ] **步骤 3： 保持 UI 为每包一个字段（`virtualWidthDp`）**

### 任务 5： 最终验证与 APK 构建

**文件：** 无

- [ ] **步骤 1：运行完整验证命令**

运行： `./gradlew :app:testDebugUnitTest :app:assembleDebug`
预期： 通过.

- [ ] **步骤 2：准备设备验证清单**

使用：
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

预期日志应包含：
- viewport 覆盖（`widthDp/heightDp/smallestWidthDp/densityDpi`）
- window bounds 覆盖
- display metric 覆盖

---

计划已完成并保存至 `docs/superpowers/plans/2026-04-14-windowmetrics-display-spoof.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 我为每个任务分发一个新的子代理，在任务间评审，快速迭代

**2. 内联执行** - 在当前会话内使用 executing-plans 执行任务，按检查点批量推进

选择哪种方式？



