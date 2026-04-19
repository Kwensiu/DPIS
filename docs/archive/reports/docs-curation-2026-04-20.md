# 文档整理记录（2026-04-20）

## 本轮目标

- 清理 `docs/` 下已完成的实施计划与阶段性分析，减少活文档噪声。
- 保留当前仍有执行价值的文档，并补充状态说明。

## 归档决策

### 1) 归档到 `docs/archive/reports/`

- `docs/list-state-analysis-2026-04-18.md`
- `docs/structure-batch-tracker-2026-04-17.md`

理由：两份均为已完成问题的过程记录，当前更多承担追溯价值。

### 2) 归档到 `docs/archive/superpowers/plans/`

- `2026-04-17-font-independent-engine-refactor.md`
- `2026-04-17-font-stability-borrowing-checklist.md`
- `2026-04-17-per-app-font-size-sheet.md`
- `2026-04-18-app-list-topbar-and-paging.md`
- `2026-04-19-settings-other-about-hide-icon.md`

理由：对应功能已在代码与测试中落地，文档职责转为历史方案记录。

### 3) 归档到 `docs/archive/superpowers/specs/`

- `2026-04-18-app-list-topbar-and-paging-design.md`
- `2026-04-19-app-detail-actions-design.md`
- `2026-04-19-settings-other-about-hide-icon-design.md`

理由：设计约束已进入实现，不再是当前开发约束源。

## 保留并调整

- 保留：`docs/project-visual-map-2026-04-19.md`
- 保留：`docs/final-validation-checklist-2026-04-17.md`
- 保留：`docs/superpowers/plans/2026-04-18-font-hook-code-cleanup.md`
  - 已新增“当前状态（2026-04-20）”，明确哪些子任务已落地、哪些仍待收口。

## 入口同步

- 已更新 `docs/README.md`：
  - 移除已归档文档的“当前有效”入口。
  - 增加本次归档记录链接。
- 已更新 `docs/archive/README.md`：
  - 新增 `2026-04-20 归档` 小节与迁移清单。
