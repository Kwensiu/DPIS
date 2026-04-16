# 虚拟视口伪装 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 将当前 per-app 目标 DPI 配置改为 per-app 虚拟 dp 视口配置，让目标应用在不改变物理屏幕参数的前提下，看到一个更小的可用 dp 视口，从而使内容整体显得更大并触发布局断点变化。

**架构：** 保留 `ResourcesManager.applyConfigurationToResources(...)` 与 `ResourcesImpl.updateConfiguration(...)` 两个主 Hook 点，在进入资源链路时优先改写 `Configuration.screenWidthDp`、`screenHeightDp`、`smallestScreenWidthDp`，必要时同步 compat 字段。配置页只暴露两个固定包的“虚拟宽度 dp”输入框，并根据当前 `Configuration` 的纵横比在运行时推导高度 dp 与 smallest width。

**技术栈：** Android SDK (Java), libxposed/api, libxposed/service remote preferences, Gradle unit tests, native Android UI.

---

### 任务 1： 为虚拟视口计算添加失败测试

**文件：**
- 新增： `app/src/test/java/com/dpis/module/ViewportOverrideTest.java`
- 修改： `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **步骤 1： 编写用于 viewport 推导与配置存储的失败测试**

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

- [ ] **步骤 2： 运行 `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.DpiConfigStoreTest` 并确认失败**，因为 `ViewportOverride` 和新的 store API 尚不存在。

### 任务 2： 实现 viewport 配置模型与存储

**文件：**
- 新增： `app/src/main/java/com/dpis/module/ViewportOverride.java`
- 修改： `app/src/main/java/com/dpis/module/DpiConfig.java`
- 修改： `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- 修改： `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **步骤 1： 添加一个聚焦辅助方法，用于从当前配置推导目标视口尺寸**

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

规则：
- `targetWidthDp <= 0` 表示禁用
- `heightDp` 通过保持当前 `screenHeightDp / screenWidthDp` 比例推导
- `smallestWidthDp = min(widthDp, heightDp)`
- 存在 compat 字段时也一并更新

- [ ] **步骤 2： 将种子/运行时键模型从 `dpi.<pkg>.target` 替换为 `viewport.<pkg>.width_dp`**

`DpiConfigStore` 应暴露：
- `Integer getTargetViewportWidthDp(String packageName)`
- `void setTargetViewportWidthDp(String packageName, int widthDp)`
- `void clearTargetViewportWidthDp(String packageName)`
- `Integer getEffectiveViewportWidthDp(String packageName)`
- `void ensureSeedConfig(Map<String, Integer> seedViewportWidthDps)`

- [ ] **步骤 3： 在 `DpiConfig` 中设置新的种子值**

为两个固定包使用同一个种子虚拟宽度值，例如：

```java
static final int SEED_TARGET_VIEWPORT_WIDTH_DP = 360;
```

- [ ] **步骤 4： 重新运行聚焦测试并确保通过**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ViewportOverrideTest --tests com.dpis.module.DpiConfigStoreTest`
预期： 通过.

### 任务 3： 将 Hook 逻辑从“density 优先”切换为“viewport 优先”

**文件：**
- 修改： `app/src/main/java/com/dpis/module/ModuleMain.java`
- 修改： `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- 修改： `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- 修改： `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **步骤 1： 为以下内容添加失败测试：configuration dp 字段覆盖**

添加测试以验证：
- 配置的虚拟宽度会改变 `screenWidthDp`
- 推导得到的 `screenHeightDp` 遵循比例
- `smallestScreenWidthDp` 会更新
- 禁用配置时保持数值不变

- [ ] **步骤 2： 更新 `ModuleMain` 日志和包门控逻辑，改用 viewport 配置**

预期日志形态：

```java
DpisLog.i("target package matched: package=" + packageName
        + ", targetViewportWidthDp=" + targetWidthDp);
```

- [ ] **步骤 3： 在 `ResourcesManager` Hook 中，于继续执行前应用 viewport 覆盖**

使用 `ViewportOverride.derive(config, targetWidthDp)` 后回写：
- `config.screenWidthDp`
- `config.screenHeightDp`
- `config.smallestScreenWidthDp`
- 在适用位置同步 compat 对应字段

- [ ] **步骤 4： 在 `ResourcesImpl` Hook 中，确保上游更新后配置字段仍保持同步**

保留现有 density 同步代码仅作为次级一致性层（如有需要），但主要可观察变化必须是 viewport dp 字段。

- [ ] **步骤 5： 重新运行 Hook 测试与完整单测套件**

运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
预期： 通过.

运行： `./gradlew :app:testDebugUnitTest`
预期： 通过.

### 任务 4： 更新原生配置页以支持虚拟宽度编辑

**文件：**
- 修改： `app/src/main/java/com/dpis/module/MainActivity.java`
- 修改： `app/src/main/res/layout/activity_status.xml`
- 修改： `app/src/main/res/values/strings.xml`

- [ ] **步骤 1： 将标签从 DPI 改为虚拟宽度 dp**

每个包区块应展示：
- 当前虚拟宽度
- 存在配置时的推导生效视口摘要
- 输入提示，例如 `例如 360` 或 `留空禁用`

- [ ] **步骤 2： 更新保存处理器，写入 viewport 宽度而非目标 DPI**

```java
store.setTargetViewportWidthDp(packageName, widthDp);
```

- [ ] **步骤 3： 优化文案，弱化 PoC/测试语气并增加初始化状态**

展示 remote preferences 是否已初始化，并明确保存后需 kill/restart 目标应用进程。

- [ ] **步骤 4：运行 `./gradlew :app:assembleDebug`**
预期： 通过.

### 任务 5： 最终验证与 APK 构建

**文件：** 无

- [ ] **步骤 1：运行完整验证命令**

运行： `./gradlew :app:testDebugUnitTest :app:assembleDebug`
预期： 通过.

- [ ] **步骤 2： 构建设备验证清单**

使用：
- `adb -s 192.168.5.130:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb -s 192.168.5.130:5555 logcat | findstr DPIS`

预期日志应提及 `targetViewportWidthDp=<value>` 以及与视口相关的覆盖。

---

计划已完成并保存至 `docs/superpowers/plans/2026-04-14-virtual-viewport-spoofing.md`。两种执行方式：

**1. 子代理驱动（推荐）** - 我为每个任务分发一个新的子代理，在任务间评审，快速迭代

**2. 内联执行** - 在当前会话内使用 executing-plans 执行任务，按检查点批量推进

选择哪种方式？



