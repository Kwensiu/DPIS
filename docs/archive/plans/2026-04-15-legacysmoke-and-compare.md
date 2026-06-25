# Legacy 冒烟模块与对比计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 新增一个独立的传统 Xposed 冒烟测试 APK，并文档化其打包方式与 DPIS 及已知可用参考模块的差异。

**架构：** 引入一个新的 Gradle Android 应用模块，仅包含旧式 Xposed 元数据、`assets/xposed_init` 和单一 `IXposedHookLoadPackage` 入口（在 `system` 加载时打日志）。同时基于 AppSettingsR、SetAppFull、InxLocker 与当前 DPIS 应用的 manifest 和入口资源产出对比文档。

**技术栈：** Android Gradle 应用模块、legacy Xposed API、JUnit4、Markdown 文档

---



