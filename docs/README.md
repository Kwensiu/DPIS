# 文档入口

这里用于快速找到 DPIS 仍在维护的文档。根目录 `README.md` 面向普通用户；这里负责把用户入口、开发入口和历史资料分开。

## 普通用户

- [中文 README](../README.md)：安装前快速了解 DPIS 能做什么、怎么开始使用。
- [English README](./README.en.md)：English introduction and quick start.

## 开发者

- [AGENTS.md](../AGENTS.md)：项目结构、构建测试命令和协作约定。
- [project-visual-map-2026-04-19.md](./project-visual-map-2026-04-19.md)：项目结构和主要修改落点速览。

## 开发与排障

- [modern-runtime-resync.md](./modern-runtime-resync.md)：Modern/libxposed 视口与 runtime 路线记录。
- [legacy-runtime-resync.md](./legacy-runtime-resync.md)：Legacy/传统 Xposed 兼容路线记录。
- [lsposed-diagnostics.md](./lsposed-diagnostics.md)：LSPosed 模块日志拉取、过滤和判断方式。
- [font-routing.md](./font-routing.md)：字体模式、自定义 Hook 链路和内部调度边界。
- [java-toolchain-policy.md](./java-toolchain-policy.md)：JDK、Java 兼容级别和 Android API 使用边界。
- [ui-guidelines.md](./ui-guidelines.md)：DPIS UI 修改约定。
- [agents/](./agents/)：Agent 协作配置、issue tracker 信息和运行时排查流程。

## 计划与历史记录

- [font-hook-execution-plan-refactor.md](./font-hook-execution-plan-refactor.md)
- [custom-per-app-font-hook-domain-plan.md](./custom-per-app-font-hook-domain-plan.md)
- [font-domain-arbitration-provenance-plan.md](./font-domain-arbitration-provenance-plan.md)
- [compose-resources-font-scheduling-slice.md](./compose-resources-font-scheduling-slice.md)
- [final-validation-checklist-2026-04-17.md](./final-validation-checklist-2026-04-17.md)

## 归档

- [archive/README.md](./archive/README.md)
- [archive/](./archive/)
- [archive/reports/docs-curation-2026-04-20.md](./archive/reports/docs-curation-2026-04-20.md)
- [archive/reports/docs-curation-2026-04-17.md](./archive/reports/docs-curation-2026-04-17.md)

## 维护约定

- 修改视口/runtime hook 路线前，先读对应的 Modern 或 Legacy 路线文档；触碰共享代码时两个都读。
- 新路线探索、失败实验、未采用方案和关键运行证据应记录到对应路线文档。
- 新文档先判断是否仍然有效；已经过期或只用于历史追踪的内容放入 `docs/archive/`。
