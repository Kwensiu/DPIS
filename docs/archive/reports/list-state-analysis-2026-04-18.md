# 列表状态分析与修复记录（2026-04-18）

## 背景

本记录覆盖以下问题：

- 列表初次加载体感偏慢
- 旋转屏幕后页面重建并重复加载
- “已配置应用”分页偶发空白

本文档已按当前代码更新，并同步记录已落地修复。

## 当前实现（修复前后通用事实）

页面结构是：

- 一个 `ViewPager2`（`ALL_APPS` / `CONFIGURED_APPS` 两页）
- 一个 `TabLayout`（通过 `TabLayoutMediator` 绑定）
- 一份全量内存快照 `allApps`
- 每次 `applyFilter()` 对两页分别过滤并提交到 `AppListPagerAdapter`

“已配置应用”不是独立数据源，它仍然来自同一份 `allApps` 快照，只是过滤条件不同。

## 根因结论

### 1. 并发加载与结果乱序覆盖（主因）

修复前 `MainActivity` 的加载是“每次触发就直接开新线程”，且没有请求版本控制。  
在 `onCreate`、`onStart` 立即通知、服务状态变化、配置保存后都可能触发加载，导致：

1. 多条加载线程并发运行
2. 新旧结果返回顺序不可控
3. 旧结果可能覆盖较新的快照
4. `CONFIGURED_APPS` 在某些时序下显示为空或不完整

### 2. 旋转后的状态恢复缺失

修复前没有保存/恢复搜索词、分页位置、过滤开关，也没有保留已加载快照，导致旋转后回到默认状态并触发重载。

### 3. 重复加载放大了“偏慢”感知

单次加载链路本身较重（安装应用枚举、label/icon 读取、配置读取、排序），叠加重复触发后，启动和状态切换阶段更容易感知“慢”。

## 已落地修复

### 1. 引入加载协同器，消除并发乱序

新增 `AppLoadCoordinator`（`app/src/main/java/com/dpis/module/AppLoadCoordinator.java`）：

- 同时只允许一个活跃加载
- 加载中收到新请求时只记录“最新请求版本”
- 当前加载完成后：
  - 若存在更新请求：丢弃旧结果并只启动一次最新加载
  - 若无更新请求：应用结果到 `allApps`

这保证“旧结果不会在新请求之后落地”。

### 2. MainActivity 改为“请求式加载”

`MainActivity` 中：

- `loadAppsAsync()` 替换为 `requestAppsLoad()` + `startAppsLoad()` + `onAppsLoadFinished()`
- 所有原刷新入口（服务状态变化、scope 变更、配置保存/禁用）统一走 `requestAppsLoad()`
- 线程命名改为 `dpis-load-apps-<requestId>`，便于日志追踪

### 3. 旋转状态恢复与快照保留

新增两层恢复：

- `onSaveInstanceState()` 保存：
  - `currentQuery`
  - 当前页索引
  - 四个过滤开关
- `onRetainNonConfigurationInstance()` 保留：
  - `allApps` 快照
  - 查询词
  - 过滤状态
  - 当前页索引

`onCreate()` 优先恢复这些状态；有快照时直接 `applyFilter()`，避免旋转后立刻全量重扫。

### 4. 降低不必要的“立即重载”

`onStart()` 注册服务监听时，改为仅在列表为空时使用 `notifyImmediately=true`。  
有已恢复快照时不再强制立即重载，减少重复加载。

## 影响评估

### 已直接改善

- “已配置应用”偶发空白：主因（乱序覆盖）已被约束
- 旋转后的状态丢失与重扫：已恢复并避免不必要重载
- 启动/切换阶段体感偏慢：因重复加载减少而缓解

### 仍可继续优化（非本次必需）

- 图标与 label 的缓存策略（进一步降低单次加载成本）
- UI loading/empty 状态分离（降低“空列表=加载中”误判）

## 测试与验证

新增单测：`app/src/test/java/com/dpis/module/AppLoadCoordinatorTest.java`

覆盖点：

- 首次请求立即启动
- 加载中追加请求时，旧结果被丢弃
- 多次追加请求会合并为一次“最新请求”重跑

建议回归命令：

- `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppLoadCoordinatorTest`
- `./gradlew :app:testDebugUnitTest`
