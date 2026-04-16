# Legacy Xposed 元数据 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 让 DPIS 可被识别为传统 Xposed 模块，以便 LSPosed 在 `system` 中执行 legacy 入口。

**架构：** 保持现有 modern 与 legacy 入口并行，仅补充经典 Xposed manifest 元数据、声明 legacy scope 资源，并加入一个小型回归测试锁定这些打包要求。

**技术栈：** Android 应用 manifest/资源、JUnit4 单元测试、Gradle

---

### 任务 1： 用单元测试锁定 legacy 元数据要求

**文件：**
- 新增： `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`
- 测试： `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`

- [ ] **步骤 1： 编写失败测试**
- [ ] **步骤 2： 运行测试并确认失败**
- [ ] **步骤 3： 添加最小 manifest/资源元数据**
- [ ] **步骤 4： 运行测试并确认通过**
- [ ] **步骤 5： 运行定向构建验证**



