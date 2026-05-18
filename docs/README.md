# 文档入口（当前有效）

本文档用于快速定位当前仍在使用的文档，并给出归档入口。

## 当前有效文档

- [project-visual-map-2026-04-19.md](./project-visual-map-2026-04-19.md)
  - 项目可视化展开图，按入口、职责、链路和修改落点快速理解 DPIS。
- [compat100-runtime-resync.md](./compat100-runtime-resync.md)
  - compat100 持久配置与运行态镜像重放说明，记录 100 版本的恢复边界和验证路径。
- [font-hook-execution-plan-refactor.md](./font-hook-execution-plan-refactor.md)
  - 字体/视口 hook 执行计划重构说明，记录 planner/orchestrator 边界、debug 覆盖归属和后续迁移顺序。
- [custom-per-app-font-hook-domain-plan.md](./custom-per-app-font-hook-domain-plan.md)
  - 按应用自定义字体兼容链路说明，记录 hook domain 存储、调度、UI 和实验入口清理决策。
- [final-validation-checklist-2026-04-17.md](./final-validation-checklist-2026-04-17.md)
  - 最终构建、测试、设备回归验收清单。

## 历史文档归档入口

- [archive/README.md](./archive/README.md)
- [archive/](./archive/)
- [archive/reports/docs-curation-2026-04-20.md](./archive/reports/docs-curation-2026-04-20.md)
  - 本轮按“完成即归档”执行结果与保留项说明。
- [archive/reports/docs-curation-2026-04-17.md](./archive/reports/docs-curation-2026-04-17.md)
  - 本轮文档保留/归档决策与执行记录。

## 使用约定

- 新文档先判断是否属于“当前有效”；若否，直接放入 `docs/archive/` 对应子目录。
- 与当前主线无直接关系的历史方案/计划，优先归档而非继续追加到活文档。
- 已完成或阶段性失效的 superpowers 计划/设计放入 `docs/archive/superpowers/`。
