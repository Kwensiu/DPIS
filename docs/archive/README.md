# 归档说明

此目录存放已不在当前主线执行路径上的历史规格、计划与报告。

## 2026-04-16 归档

- `superpowers/specs/2026-04-14-*`
- `superpowers/plans/2026-04-14-*`
- `superpowers/plans/2026-04-15-legacy-*`

## 归档原因

- 这些文档记录的是早期实验与迁移阶段信息。
- 将其移出活文档区可减少噪声，同时保留可追溯性。

## 重要说明

- 历史模块目录（`legacysmoke`、`yukismoke`）当时仍保留在仓库根目录。
- 原因：相关测试曾直接读取这些路径（例如 `singleapk/src/test/.../LegacySmokeModuleFilesTest`）。

## 2026-04-17 归档

将报告/状态文档从 `docs/` 迁移到 `docs/archive/reports/`，让顶层活文档聚焦当前实现，同时保留历史追踪。

- `docs/dpis-yuki-migration-status.md` -> `docs/archive/reports/dpis-yuki-migration-status.md`
- `docs/legacy-module-compare.md` -> `docs/archive/reports/legacy-module-compare.md`
- `docs/system-smoke-status.md` -> `docs/archive/reports/system-smoke-status.md`
- `docs/useful-files-full-analysis-2026-04-16.md` -> `docs/archive/reports/useful-files-full-analysis-2026-04-16.md`
- `docs/docs-curation-2026-04-17.md` -> `docs/archive/reports/docs-curation-2026-04-17.md`

## 2026-04-20 归档

按“完成即归档”整理文档，收敛活文档面，保留 `project-visual-map`、`final-validation-checklist` 与 1 份未收口的字体整理计划。

- 报告类迁移到 `archive/reports/`：
  - `docs/list-state-analysis-2026-04-18.md` -> `docs/archive/reports/list-state-analysis-2026-04-18.md`
  - `docs/structure-batch-tracker-2026-04-17.md` -> `docs/archive/reports/structure-batch-tracker-2026-04-17.md`
- superpowers 计划迁移到 `archive/superpowers/plans/`：
  - `docs/superpowers/plans/2026-04-17-font-independent-engine-refactor.md`
  - `docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md`
  - `docs/superpowers/plans/2026-04-17-per-app-font-size-sheet.md`
  - `docs/superpowers/plans/2026-04-18-app-list-topbar-and-paging.md`
  - `docs/superpowers/plans/2026-04-19-settings-other-about-hide-icon.md`
- superpowers 设计迁移到 `archive/superpowers/specs/`：
  - `docs/superpowers/specs/2026-04-18-app-list-topbar-and-paging-design.md`
  - `docs/superpowers/specs/2026-04-19-app-detail-actions-design.md`
  - `docs/superpowers/specs/2026-04-19-settings-other-about-hide-icon-design.md`
