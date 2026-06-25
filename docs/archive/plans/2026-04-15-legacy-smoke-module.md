# Legacy 冒烟模块计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 将 DPIS 的 legacy 入口缩减为最小旧式冒烟检查，用于证明 LSPosed 会在 `system` 中执行它。

**架构：** 保持当前打包方式，但简化 legacy 代码路径，使成功标准变为一个明确无歧义的 `XposedBridge.log`/`logcat` 标记，并与 DisplayPolicy 和应用配置逻辑解耦。

**技术栈：** Android Java、legacy Xposed API、JUnit4、Gradle

---



