# 文档入口（当前有效）

本文档用于快速定位当前仍在使用的文档，并给出归档入口。

## 当前有效文档

- [project-visual-map-2026-04-19.md](./project-visual-map-2026-04-19.md)
  - 项目可视化展开图，按入口、职责、链路和修改落点快速理解 DPIS。
- [modern101-runtime-resync.md](./modern101-runtime-resync.md)
  - DPIS 生命文档。modern101/libxposed 视口链路追踪说明；修改 101 或共享视口/runtime 路线前必须阅读，并记录每次路线探索、调整和失败实验。
- [compat100-runtime-resync.md](./compat100-runtime-resync.md)
  - DPIS 生命文档。compat100 视口链路追踪说明，记录 compat100 特有系统路线和实验。
- [lsposed-diagnostics.md](./lsposed-diagnostics.md)
  - LSPosed 模块日志诊断说明。记录两个 flavor 通用的 modules_*.log / verbose_*.log 拉取和过滤路径。
- [font-routing.md](./font-routing.md)
  - 字体模式、自定义 Hook 链路与内部调度域的语义边界。
- [java-toolchain-policy.md](./java-toolchain-policy.md)
  - JDK 运行环境、Java 兼容级别与 Android API 可用性的边界。
- [font-hook-execution-plan-refactor.md](./font-hook-execution-plan-refactor.md)
  - 字体/视口 hook 执行计划重构说明，记录 planner/orchestrator 边界、debug 覆盖归属和后续迁移顺序。
- [custom-per-app-font-hook-domain-plan.md](./custom-per-app-font-hook-domain-plan.md)
  - 按应用自定义字体兼容链路说明，记录 hook domain 存储、调度、UI 和实验入口清理决策。
- [font-domain-arbitration-provenance-plan.md](./font-domain-arbitration-provenance-plan.md)
  - 字体链路调度与来源标记说明，记录 TextView/Paint provenance、Compose/resources_font 调度和后续 Compose-native 方向。
- [compose-resources-font-scheduling-slice.md](./compose-resources-font-scheduling-slice.md)
  - Compose/resources_font 调度切片说明，记录当前自动抑制重复 Resources 字体缩放的证据和边界。
- [final-validation-checklist-2026-04-17.md](./final-validation-checklist-2026-04-17.md)
  - 最终构建、测试、设备回归验收清单。
- [ui-guidelines.md](./ui-guidelines.md)
  - DPIS UI 变更约定，记录新增控件、弹窗、spacing/style 资源和测试边界的默认规则。
- [agents/](./agents/)
  - Agent 协作配置，记录 issue tracker、triage 标签、领域文档读取规则和 DPIS runtime 路线排查流程。

## 历史文档归档入口

- [archive/README.md](./archive/README.md)
- [archive/](./archive/)
- [archive/reports/docs-curation-2026-04-20.md](./archive/reports/docs-curation-2026-04-20.md)
  - 本轮按“完成即归档”执行结果与保留项说明。
- [archive/reports/docs-curation-2026-04-17.md](./archive/reports/docs-curation-2026-04-17.md)
  - 本轮文档保留/归档决策与执行记录。

## 使用约定

- `compat100-runtime-resync.md` 与 `modern101-runtime-resync.md` 是视口/runtime hook 路线生命文档。后续新增、修改、移除路线时，先阅读相关文档；触碰共享代码时两个文档都读。
- 新路线探索、路线细节调整、失败实验、未采用方案和关键运行证据都要合理记录。非必要不删除历史记录，优先用"inactive / superseded / rejected"一类状态保留经验。
- LSPosed 模块日志诊断路径见 `lsposed-diagnostics.md`，两个 flavor 通用。
- 新文档先判断是否属于“当前有效”；若否，直接放入 `docs/archive/` 对应子目录。
- 与当前主线无直接关系的历史方案/计划，优先归档而非继续追加到活文档。
- 已完成或阶段性失效的 superpowers 计划/设计放入 `docs/archive/superpowers/`。
