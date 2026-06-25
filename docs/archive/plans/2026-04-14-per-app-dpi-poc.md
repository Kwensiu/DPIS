# 每应用 DPI PoC 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 构建一个最小化 Android Xposed 模块 PoC，在 `ResourcesManager.applyConfigurationToResources(...)` 上安装首个 Hook，并为一个目标应用覆盖 `densityDpi`。

**架构：** 使用一个小型 Android 应用模块，将 `libxposed/api` 作为 `compileOnly` 依赖，在 `onPackageReady()` 中安装 Hook，并把 density 计算放在纯 Java 辅助类中，以便在接线 Hook 前通过本地单元测试验证。

**技术栈：** Android Gradle Plugin、Java、libxposed API、JUnit 4

---



