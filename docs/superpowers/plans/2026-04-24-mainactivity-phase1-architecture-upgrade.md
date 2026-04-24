# MainActivity Two-Phase Architecture Upgrade Plan (Consolidated)

## Document Meta
- Updated: 2026-04-24 (implementation in progress branch)
- Type: How-to + Reference
- Scope: `MainActivity` 架构升级、更新链路统一、状态持久化拆分、测试与发布收口

## Goal
把 `MainActivity` 从巨型控制器收敛为 `render + dispatch`，并将更新能力统一为可复用组件；同时把“已完成 / 未完成 / 待决策”放在同一份可执行清单里。

## Current Snapshot
- 工作区状态：存在大量未提交改动（`A/M/MM/??`），已进入实质实现阶段。
- 已完成验证（当前分支实测）：
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug --no-configuration-cache`（固定 `JAVA_HOME` 到 temurin17）
- 当前策略：按“功能端到端（B）”分组提交。

## Completed (Organized by Commit-Ready Batches)

### Batch A: Phase 1 架构骨架（状态层 + 分发层）
- [x] 新增 `MainUiState` / `MainUiAction` / `MainUiEffect`
- [x] 新增 `MainViewModel` 并接入 `MainActivity`
- [x] `MainActivity` 已具备 action dispatch / state render 主路径
- [x] 新增 `MainViewModelTest`

Files (main):
- `app/src/main/java/com/dpis/module/MainUiState.java`
- `app/src/main/java/com/dpis/module/MainUiAction.java`
- `app/src/main/java/com/dpis/module/MainUiEffect.java`
- `app/src/main/java/com/dpis/module/MainViewModel.java`
- `app/src/main/java/com/dpis/module/MainActivity.java`

Files (test):
- `app/src/test/java/com/dpis/module/MainViewModelTest.java`
- `app/src/test/java/com/dpis/module/MainActivitySourceSmokeTest.java`

### Batch B: Phase 2 已落地部分（配置弹窗与进程动作下沉）
- [x] `showEditDialog` 主体流程已迁移到 `AppConfigDialogCoordinator`
- [x] 新增配置保存/绑定辅助类，降低 `MainActivity` 责任
- [x] 进程操作已下沉到 `ProcessActionHandler`

Files:
- `app/src/main/java/com/dpis/module/AppConfigDialogCoordinator.java`
- `app/src/main/java/com/dpis/module/AppConfigDialogBinder.java`
- `app/src/main/java/com/dpis/module/AppConfigSaveHandler.java`
- `app/src/main/java/com/dpis/module/ProcessActionHandler.java`
- `app/src/main/java/com/dpis/module/MainActivity.java`
- `app/src/test/java/com/dpis/module/AppConfigDialogBinderSourceSmokeTest.java`

### Batch C: 更新链路拆分与关键修复
- [x] 引入 `UpdateCoordinator`（更新状态决策中心）
- [x] 引入 `UpdateDownloadCoordinator`（下载执行编排）
- [x] `AboutActivity` 下载状态源修复（不再固定 `State.empty()`）
- [x] 启动失败回滚下载状态，避免“假下载中”卡死
- [x] 冷启动检查相关行为测试已补
- [x] 新增共享 `UpdateManifestFetcher`，About/Main 启动检查复用同一 manifest 拉取实现
- [x] 修复 `MainActivity.applyDownloadState(...)` 空实现，补齐下载状态闭环

Files:
- `app/src/main/java/com/dpis/module/UpdateCoordinator.java`
- `app/src/main/java/com/dpis/module/UpdateDownloadCoordinator.java`
- `app/src/main/java/com/dpis/module/UpdateManifestFetcher.java`
- `app/src/main/java/com/dpis/module/AboutActivity.java`
- `app/src/main/java/com/dpis/module/StartupUpdateCheckCoordinator.java`
- `app/src/test/java/com/dpis/module/UpdateCoordinatorTest.java`
- `app/src/test/java/com/dpis/module/UpdateDownloadCoordinatorSourceSmokeTest.java`
- `app/src/test/java/com/dpis/module/UpdateManifestFetcherSourceSmokeTest.java`
- `app/src/test/java/com/dpis/module/StartupUpdateCheckCoordinatorTest.java`
- `app/src/test/java/com/dpis/module/AboutActivitySourceSmokeTest.java`

### Batch D: 状态存储拆分（已创建，待最终收口验证）
- [x] 新增 `UpdateStateStore`
- [x] `MainActivity` 已接入 store 字段
- [x] 新增 `UpdateStateStoreTest`

Files:
- `app/src/main/java/com/dpis/module/UpdateStateStore.java`
- `app/src/main/java/com/dpis/module/MainActivity.java`
- `app/src/test/java/com/dpis/module/UpdateStateStoreTest.java`

## Delivery Checklist (Commit-Ready)

### 1. 主线实现落地
- [x] `MainActivity` Phase 1/2 主线骨架与关键下沉类已落地
- [x] 更新能力统一到共享流水线：manifest -> download -> verify -> install
- [x] `UpdateStateStore` 按决策 A 接入“当前更新流程”

### 2. 测试与构建
- [x] 单元与 smoke 测试补强（含 UpdateManifestFetcher / 下载状态闭环）
- [x] `./gradlew :app:testDebugUnitTest` 通过
- [x] `./gradlew :app:assembleDebug --no-configuration-cache` 通过

### 3. 文档同步
- [x] `docs/README.md` 已保留主线计划入口
- [x] 本计划文档已更新为实现态

### 4. 提交前最后动作
- [x] 本轮改为一次性单 commit 收口（含代码、测试、文档）
- [ ] 手测：列表加载、筛选、编辑配置、更新检查、取消/重试、安装链路

## Decision Queue (Need User Choices)
- [B] 提交策略：
  - 方案 A：按 4 个批次提交（A/B/C/D）
  - 方案 B：按“功能端到端”提交（每个功能含代码+测试）
- [B] `refs/xposed-modules-repo/` 处理方式：
  - 方案 A：纳入本次变更
  - 方案 B：排除，单独后续处理
- [A] `UpdateStateStore` 推进深度：
  - 方案 A：只覆盖当前更新流程
  - 方案 B：扩展覆盖所有 update 相关持久化读取入口

## Recommended Execution Order
1. 先完成手测链路。
2. 以单 commit 方式提交当前收口改动。
3. 结束分支收口。
