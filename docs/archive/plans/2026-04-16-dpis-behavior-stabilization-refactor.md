# DPIS 行为稳定化重构 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 在不改变当前 DPI 生效结果（`com.max.xiaoheihe` 观感已正确）的前提下，整理链路代码并降低闪屏/卡顿抖动。

**架构：** 先锁定“现有正确行为”的合同测试，再把重复 override 逻辑收敛为单一流水线，最后仅做等价稳定化（去重写入、抑制无效覆盖、减少日志热点）。所有稳定化改动都通过测试与设备日志对比证明“语义不变”。

**技术栈：** Java、Android/Xposed（LSPosed）、JUnit4、Gradle、adb/logcat

---

## 当前行为分析

- 已确认正确：
  - `system_server apply` 已出现，`config-dispatch` 路径可把 `360/480` 改到 `300/576`。
  - app 侧 `ResourcesManager/Display` override 可命中，小黑盒观感接近直接改系统 DPI。
- 未完全解决：
  - 仍有闪屏和卡顿，表现为冷启动和界面切换时短暂抖动。
- 代码层面风险点（当前观察）：
  - 多入口分别计算 override（`ResourcesManager`、`ResourcesImpl`、`Display`、`system_server`），策略重复且分散。
  - `VirtualDisplayState` 是全局静态单值，跨调用路径频繁覆盖，容易产生瞬态不一致。
  - 资源创建链路与配置更新链路并行，虽然已补钩，但仍缺“行为等价性”的自动化回归保护。
  - 日志量在热路径较高，可能放大卡顿体感。

## 范围与非目标

- 范围内：
  - 不改目标包配置语义（继续以 viewport width=300 生效）。
  - 通过重构提升可维护性，并减少无效写入/抖动。
- 范围外：
  - 不引入新的 UI 功能。
  - 不扩大到全量应用兼容策略重写。

## 文件结构与职责

- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
  - 资源创建/更新路径入口，后续改为调用统一流水线。
- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
  - `ResourcesImpl.updateConfiguration` 的覆盖逻辑入口。
- 修改： `singleapk/src/main/java/com/dpis/module/DisplayHookInstaller.java`
  - `DisplayMetrics` 输出覆盖入口。
- 修改： `singleapk/src/main/java/com/dpis/module/VirtualDisplayState.java`
  - 全局显示状态容器，增加“等值去重”能力。
- 新增： `singleapk/src/main/java/com/dpis/module/DisplayOverridePipeline.java`
  - 新增统一计算/应用流水线（纯逻辑 + 小型结果对象）。
- 新增： `singleapk/src/test/java/com/dpis/module/DisplayOverridePipelineTest.java`
  - 锁定流水线输入输出语义。
- 新增： `singleapk/src/test/java/com/dpis/module/VirtualDisplayStateTest.java`
  - 锁定状态更新去重语义。
- 修改： `singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`
  - 迁移到新流水线后保持行为一致。
- 修改： `docs/system-smoke-status.md`
  - 记录重构前后设备行为对比证据。

### 任务 1： 用契约测试锁定当前正确行为

**文件：**
- 新增： `singleapk/src/test/java/com/dpis/module/DisplayOverridePipelineTest.java`
- 修改： `singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`
- 测试： `singleapk/src/test/java/com/dpis/module/DisplayOverridePipelineTest.java`

- [ ] **步骤 1： 编写失败测试**

```java
package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DisplayOverridePipelineTest {
    @Test
    public void returnsNullWhenTargetWidthInvalid() {
        DisplayOverridePipeline.Result result = DisplayOverridePipeline.compute(
                360, 736, 360, 480, 1080, 2208, 0
        );
        assertNull(result);
    }

    @Test
    public void computesExpectedViewportFor300dp() {
        DisplayOverridePipeline.Result result = DisplayOverridePipeline.compute(
                360, 736, 360, 480, 1080, 2208, 300
        );
        assertEquals(300, result.widthDp);
        assertEquals(576, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }
}
```

- [ ] **步骤 2： 运行测试并确认失败**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.DisplayOverridePipelineTest`  
预期： 失败 ，报 unresolved `DisplayOverridePipeline`。

- [ ] **步骤 3： 编写最小实现**

```java
// singleapk/src/main/java/com/dpis/module/DisplayOverridePipeline.java
package com.dpis.module;

final class DisplayOverridePipeline {
    static final class Result {
        final int widthDp;
        final int heightDp;
        final int smallestWidthDp;
        final int densityDpi;
        final int widthPx;
        final int heightPx;

        Result(int widthDp, int heightDp, int smallestWidthDp,
               int densityDpi, int widthPx, int heightPx) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.smallestWidthDp = smallestWidthDp;
            this.densityDpi = densityDpi;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }
    }

    static Result compute(int widthDp, int heightDp, int smallestWidthDp,
                          int densityDpi, int widthPx, int heightPx,
                          int targetWidthDp) {
        if (targetWidthDp <= 0 || widthDp <= 0 || heightDp <= 0 || densityDpi <= 0) {
            return null;
        }
        int targetDensity = Math.round(widthPx * 160f / targetWidthDp);
        int targetHeightDp = Math.round(heightPx * 160f / targetDensity);
        int targetSmallest = Math.min(targetWidthDp, targetHeightDp);
        return new Result(targetWidthDp, targetHeightDp, targetSmallest,
                targetDensity, widthPx, heightPx);
    }
}
```

- [ ] **步骤 4： 运行测试并确认通过**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.DisplayOverridePipelineTest`  
预期： 通过.

- [ ] **步骤 5： 提交**

```bash
git add singleapk/src/main/java/com/dpis/module/DisplayOverridePipeline.java singleapk/src/test/java/com/dpis/module/DisplayOverridePipelineTest.java singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
git commit -m "test(singleapk): lock current dpi override contract before refactor"
```

### 任务 2： 重构 Resources Hooks 使用单一流水线（不改行为）

**文件：**
- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- 修改： `singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`
- 测试： `singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **步骤 1： 编写失败测试**

```java
@Test
public void resourcesImplUsesPipelineResultWithoutChangingSemantics() {
    // Given baseline config 360/736/480 and target width 300
    // When applyDensityOverride runs
    // Then config becomes 300/613/576 and metrics width/height stay same as source px.
}
```

- [ ] **步骤 2： 运行测试并确认失败**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
预期： 失败 ，原因是旧断言与基于流水线的测试接缝不匹配。

- [ ] **步骤 3： 编写最小实现**

```java
// in ResourcesImplHookInstaller.applyDensityOverride(...)
DisplayOverridePipeline.Result pipeline = DisplayOverridePipeline.compute(
        originalWidthDp,
        originalHeightDp,
        originalSmallestWidthDp,
        originalDensityDpi,
        sourceWidthPx,
        sourceHeightPx,
        targetViewportWidth != null ? targetViewportWidth : 0
);
if (pipeline == null) {
    return;
}
ViewportOverride.apply(config, new ViewportOverride.Result(
        pipeline.widthDp,
        pipeline.heightDp,
        pipeline.smallestWidthDp,
        pipeline.densityDpi
));
```

```java
// in ResourcesManagerHookInstaller.applyViewportOverride(...)
DisplayOverridePipeline.Result pipeline = DisplayOverridePipeline.compute(
        originalWidthDp,
        originalHeightDp,
        originalSmallestWidthDp,
        originalDensityDpi,
        Math.round(originalWidthDp * (originalDensityDpi / 160.0f)),
        Math.round(originalHeightDp * (originalDensityDpi / 160.0f)),
        targetViewportWidth != null ? targetViewportWidth : 0
);
if (pipeline == null) {
    return;
}
```

- [ ] **步骤 4： 运行测试并确认通过**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
预期： 通过.

- [ ] **步骤 5： 提交**

```bash
git add singleapk/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java singleapk/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java singleapk/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
git commit -m "refactor(singleapk): unify resources override through shared pipeline"
```

### 任务 3： 稳定运行时状态写入以降低闪屏风险

**文件：**
- 修改： `singleapk/src/main/java/com/dpis/module/VirtualDisplayState.java`
- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- 修改： `singleapk/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- 新增： `singleapk/src/test/java/com/dpis/module/VirtualDisplayStateTest.java`
- 测试： `singleapk/src/test/java/com/dpis/module/VirtualDisplayStateTest.java`

- [ ] **步骤 1： 编写失败测试**

```java
package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualDisplayStateTest {
    @Test
    public void updateIfChangedSkipsEquivalentState() {
        VirtualDisplayOverride.Result first = new VirtualDisplayOverride.Result(576, 1080, 2208);
        VirtualDisplayOverride.Result same = new VirtualDisplayOverride.Result(576, 1080, 2208);
        assertTrue(VirtualDisplayState.updateIfChanged(first));
        assertFalse(VirtualDisplayState.updateIfChanged(same));
    }
}
```

- [ ] **步骤 2： 运行测试并确认失败**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.VirtualDisplayStateTest`  
预期： 失败 ，报 unresolved `updateIfChanged`。

- [ ] **步骤 3： 编写最小实现**

```java
// in VirtualDisplayState.java
static boolean updateIfChanged(VirtualDisplayOverride.Result next) {
    VirtualDisplayOverride.Result prev = current;
    if (prev != null && next != null
            && prev.densityDpi == next.densityDpi
            && prev.widthPx == next.widthPx
            && prev.heightPx == next.heightPx) {
        return false;
    }
    current = next;
    return true;
}
```

```java
// replace VirtualDisplayState.set(sharedResult) with:
VirtualDisplayState.updateIfChanged(sharedResult);
```

- [ ] **步骤 4： 运行测试并确认通过**

运行： `./gradlew :singleapk:testDebugUnitTest --tests com.dpis.module.VirtualDisplayStateTest`  
预期： 通过.

- [ ] **步骤 5： 提交**

```bash
git add singleapk/src/main/java/com/dpis/module/VirtualDisplayState.java singleapk/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java singleapk/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java singleapk/src/test/java/com/dpis/module/VirtualDisplayStateTest.java
git commit -m "refactor(singleapk): dedupe virtual display state writes to reduce runtime jitter"
```

### 任务 4： 验证无功能回归并记录稳定性证据

**文件：**
- 修改： `docs/system-smoke-status.md`
- 新增： `logs/xhh-refactor-before.txt`
- 新增： `logs/xhh-refactor-after.txt`
- 测试： 运行时验证（设备）

- [ ] **步骤 1： 在重构分支合并前采集基线日志**

运行：

```bash
adb -s aeec529f logcat -c
adb -s aeec529f shell am force-stop com.max.xiaoheihe
adb -s aeec529f shell monkey -p com.max.xiaoheihe -c android.intent.category.LAUNCHER 1
adb -s aeec529f logcat -d > logs/xhh-refactor-before.txt
```

预期： 包含 `system_server apply`、`ResourcesManager override`、`Display override(getMetrics)`。

- [ ] **步骤 2： 构建并安装重构版本，然后重新抓取日志**

运行：

```bash
./gradlew :singleapk:assembleDebug
adb -s aeec529f install -r singleapk/build/outputs/apk/debug/singleapk-debug.apk
adb -s aeec529f logcat -c
adb -s aeec529f shell am force-stop com.max.xiaoheihe
adb -s aeec529f shell monkey -p com.max.xiaoheihe -c android.intent.category.LAUNCHER 1
adb -s aeec529f logcat -d > logs/xhh-refactor-after.txt
```

预期： `sw300/576` 仍出现；不应丢失 `system_server apply`。

- [ ] **步骤 3： 对比关键信号并确认语义等价**

运行：

```bash
rg -n "system_server apply|ResourcesManager override|Display override\(getMetrics\)|sw300dp|576dpi" logs/xhh-refactor-before.txt logs/xhh-refactor-after.txt
```

预期： 关键信号均存在；DPI 目标值不回退到 `sw360/480` 主态。

- [ ] **步骤 4： 更新冒烟报告并记录稳定性说明**

```markdown
# docs/system-smoke-status.md
- Date: 2026-04-16
- Goal: code cleanup without behavior regression
- Functional checks:
  - xhh perceived DPI equals system-level DPI change: PASS/FAIL
  - system_server apply present: PASS/FAIL
  - resources/display override present: PASS/FAIL
- Stability checks:
  - cold-start flicker count (subjective rounds: 3): N
  - launch jank (subjective): low/medium/high
```

- [ ] **步骤 5： 提交**

```bash
git add docs/system-smoke-status.md logs/xhh-refactor-before.txt logs/xhh-refactor-after.txt
git commit -m "docs(singleapk): record no-regression and stability validation for refactor"
```

## 自检

1. 规格覆盖：
- “全量分析当前行为”: 已包含 `当前行为分析`。
- “不影响实际功能前提下整理代码”: Task 1/2/4 明确先锁语义再重构再验证。
- “依然有闪屏卡顿”: Task 3 针对抖动风险点（全局状态重复写入）做等价稳定化。

2. 占位符扫描：
- 所有任务都给了明确文件路径、命令与预期。
- 未使用 TBD/TODO/“类似任务X”。

3. 类型一致性：
- 新增统一使用 `DisplayOverridePipeline.Result`。
- 状态去重统一使用 `VirtualDisplayState.updateIfChanged(...)`。




