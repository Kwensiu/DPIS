# ResourcesImpl Hook 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 添加 ResourcesImpl 的 `updateConfiguration` Hook，与现有 ResourcesManager 覆盖逻辑保持镜像，并在原方法执行前同步 configuration/DisplayMetrics 的 density 值。

**架构：** 新增 installer 通过反射进入 `ResourcesImpl`，拦截 `updateConfiguration`，并通过可被单元测试覆盖的辅助方法复用 `DensityOverride` 逻辑。`ModuleMain` 为目标包同时调用两个 installer。

**技术栈：** Java、Android SDK（`Configuration`、`DisplayMetrics`）、Xposed 接口、Gradle 单元测试。

---

### 任务 1： 通过测试验证 density override 辅助方法

**文件：**
- 修改： `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- 新增： `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **步骤 1： 编写失败测试**  
  添加 `ResourcesImplHookInstallerTest`，包含两个聚焦用例：

  ```java
  package com.dpis.module;

  import android.content.res.Configuration;
  import android.util.DisplayMetrics;

  import org.junit.Test;

  import static org.junit.Assert.assertEquals;

  public class ResourcesImplHookInstallerTest {
      @Test
      public void configurationDensityOverridesWhenMetricsNull() {
          Configuration config = new Configuration();
          config.densityDpi = 320;
          config.fontScale = 1.1f;
          ResourcesImplHookInstaller.applyDensityOverride(config, null);
          assertEquals(DpiConfig.TARGET_DENSITY_DPI, config.densityDpi);
      }

      @Test
      public void displayMetricsFieldsUpdatedWhenPresent() {
          Configuration config = new Configuration();
          config.densityDpi = 320;
          config.fontScale = 1.25f;
          DisplayMetrics metrics = new DisplayMetrics();
          metrics.densityDpi = 320;
          metrics.density = 2.0f;
          metrics.scaledDensity = 2.5f;
          ResourcesImplHookInstaller.applyDensityOverride(config, metrics);
          assertEquals(DpiConfig.TARGET_DENSITY_DPI, metrics.densityDpi);
          assertEquals(DensityOverride.densityFromDpi(DpiConfig.TARGET_DENSITY_DPI), metrics.density, 0.0001f);
          assertEquals(DensityOverride.scaledDensityFrom(DpiConfig.TARGET_DENSITY_DPI, config.fontScale), metrics.scaledDensity, 0.0001f);
      }
  }
  ```

- [ ] **步骤 2： 运行新测试并观察失败**  
  运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
  预期： 失败（编译错误，因为 `applyDensityOverride` 当前是 private，测试无法访问）。

- [ ] **步骤 3： 让辅助方法可访问并实现 density 同步逻辑**  
  更新 `ResourcesImplHookInstaller`，将 `applyDensityOverride` 改为包级可见，并包含完整覆盖逻辑：

  ```java
  static void applyDensityOverride(Configuration config, DisplayMetrics metrics) {
      if (config == null) {
          return;
      }
      int originalDpi = config.densityDpi;
      int targetDpi = DensityOverride.resolveDensityDpi(DpiConfig.TARGET_DENSITY_DPI, originalDpi);
      if (targetDpi == originalDpi) {
          return;
      }
      config.densityDpi = targetDpi;
      float targetDensity = DensityOverride.densityFromDpi(targetDpi);
      float targetScaledDensity = DensityOverride.scaledDensityFrom(targetDpi, config.fontScale);
      if (metrics != null) {
          metrics.densityDpi = targetDpi;
          metrics.density = targetDensity;
          metrics.scaledDensity = targetScaledDensity;
      }
      DpisLog.i("override densityDpi in ResourcesImpl: "
              + originalDpi + " -> " + targetDpi
              + ", density=" + targetDensity
              + ", scaledDensity=" + targetScaledDensity);
  }
  ```

- [ ] **步骤 4：重新运行测试并确保通过**  
  运行： `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`  
  预期： 通过.

- [ ] **步骤 5： 提交 helper/测试改动**  
  ```bash
  git add app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
  git commit -m "test: add ResourcesImpl density helper tests"
  ```

### 任务 2： 在 package ready 阶段安装新 Hook

**文件：**
- 修改： `app/src/main/java/com/dpis/module/ModuleMain.java`
- 修改： `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`

- [ ] **步骤 1： 更新 `ModuleMain.onPackageReady`，注册两个 installer**  
  将现有代码块替换为：

  ```java
  try {
      ResourcesManagerHookInstaller.install(this);
      ResourcesImplHookInstaller.install(this);
      DpisLog.i("installed ResourcesManager and ResourcesImpl hooks for " + param.getPackageName());
  } catch (Throwable throwable) {
      DpisLog.e("failed to install Resources hooks", throwable);
  }
  ```

- [ ] **步骤 2： 运行完整单测套件，确保无回归**  
  运行： `./gradlew :app:testDebugUnitTest`  
  预期： 通过；验证新增测试与现有测试均可在更新后代码上通过。

- [ ] **步骤 3： 提交 Hook 安装改动**  
  ```bash
  git add app/src/main/java/com/dpis/module/ModuleMain.java
  git commit -m "feat: register ResourcesImpl hook alongside ResourcesManager"
  ```

### 任务 3： 最终验证

**文件：** 无 (commands only)

- [ ] **步骤 1： 按需运行 lint/format 检查**  
  运行： `./gradlew :app:check` （可选，但可发现回归）。

- [ ] **步骤 2：确认工作树状态**  
  运行： `git status -sb`

- [ ] **步骤 3：准备评审摘要**  
  查看 `git diff`，确保所有变更与规格一致。

---

计划已完成并保存至 `docs/superpowers/plans/2026-04-14-resourcesimpl-hook-plan.md`。两种执行方式：

1. **子代理驱动（推荐）** - 通过 `superpowers:subagent-driven-development` 为每个任务分发新子代理，并在任务间评审。
2. **内联执行** - 在当前会话使用 `superpowers:executing-plans` 推进，按检查点批量执行任务。

选择哪种方式？



