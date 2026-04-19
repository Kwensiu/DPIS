# 设置页「其他 / 关于 / 隐藏桌面图标」设计（2026-04-19）

## 目标
- 在现有 `SystemServerSettingsActivity` 中新增「其他」分组。
- 分组内新增：
  - `关于` 入口，点击打开独立页面。
  - `隐藏桌面图标` 开关，逻辑参考 `archive/AdClose`（`activity-alias + setComponentEnabledSetting`）。
- 关于页内容参考 `archive/InstallerX-Revived` 的信息组织方式，优先实现“可立即落地”的最小可用版本。

## 当前基线（已核对）
- 设置页由 `SystemServerSettingsActivity` + `activity_system_server_settings.xml` 构成，采用卡片分组 + `item_settings_switch` / `item_settings_entry`。
- 当前 `MainActivity` 同时声明了 `LAUNCHER` 与 `MODULE_SETTINGS`，不利于“仅隐藏桌面图标、不影响 LSPosed 设置入口”。
- 配置存储集中在 `DpiConfigStore`，并通过 `DpisApplication.migrateConfig` 做本地/远端同步迁移。

## 方案对比

### 方案 A（推荐）：独立 `AboutActivity` + `activity-alias` 控制桌面入口
- 做法：
  - `SystemServerSettingsActivity` 增加“其他”分组与两条设置项。
  - 新增 `AboutActivity` + `activity_about.xml`。
  - Manifest 拆分入口：`MainActivity` 保留 `MODULE_SETTINGS`，新增 `MainActivityLauncher` alias 承载 `LAUNCHER`。
  - 开关只控制 alias 的启用状态。
- 优点：
  - 与 `AdClose` 实践一致，风险最低。
  - 不破坏 LSPosed 模块设置入口。
  - 与现有 Java + XML 架构一致，不引入新依赖。
- 缺点：
  - 关于页是静态信息，不含在线更新 API。

### 方案 B：关于页使用 WebView 加载本地 Markdown/HTML
- 优点：展示灵活，后续改文案成本低。
- 缺点：新增 WebView 生命周期/样式适配复杂度；对当前项目属于过度设计。

### 方案 C：关于内容做成底部弹窗而非页面
- 优点：改动小。
- 缺点：可扩展性差，不利于后续增加开源许可/反馈入口；与用户要求“打开一个页面”不一致。

## 推荐结论
- 采用 **方案 A**。
- 分两层实现：
  - **MVP（本次）**：关于页 + 隐藏图标开关完整闭环。
  - **扩展（可选）**：开源许可明细、本地日志导出、在线更新检查。

## 信息架构设计

### 1) 设置页结构
- 常规（保留现状）：
  - 系统层 Hook
  - 安全模式
  - 日志输出
  - 调试悬浮窗
- 其他（新增）：
  - 关于（entry）
  - 隐藏桌面图标（switch）

### 2) 关于页内容（参考 InstallerX）
- 顶部信息卡：
  - 应用名（`app_name`）
  - 版本（`BuildConfig.VERSION_NAME` + `VERSION_CODE`）
  - 模块简介（`module_description`）
- 关于分组（MVP）：
  - 查看源代码（GitHub 仓库）
  - 检查更新（GitHub Releases）
  - 问题反馈（GitHub Issues）
- 关于分组（扩展候选）：
  - 开源许可证列表（独立页或对话框）
  - 日志导出（需先补本地日志持久化链路）

## “隐藏桌面图标”设计

### Manifest 结构调整
- `MainActivity`：
  - 保留 `ACTION_MAIN + de.robv.android.xposed.category.MODULE_SETTINGS`
  - 移除 `android.intent.category.LAUNCHER`
- 新增 `activity-alias`（例如 `.MainActivityLauncher`）：
  - `targetActivity=".MainActivity"`
  - 仅声明 `ACTION_MAIN + android.intent.category.LAUNCHER`

### 开关行为
- `关闭`（默认）：alias `ENABLED`，桌面图标可见。
- `开启`：alias `DISABLED`，桌面图标隐藏。
- 执行 API：
  - `PackageManager.setComponentEnabledSetting(component, state, DONT_KILL_APP)`

### 持久化与一致性
- 在 `DpiConfigStore` 新增 key（建议：`ui.hide_launcher_icon`）。
- 设置页打开时：
  - 先读取 store 值设置 switch；
  - 再用 `PackageManager.getComponentEnabledSetting` 做一次状态对齐（防止外部改动导致漂移）。
- 切换失败时回滚 switch 并 toast。

### 风险控制
- 开启隐藏前弹确认提示：明确“可从 LSPosed 模块设置入口进入”。
- 不禁用 `MainActivity` 本体，确保不会失去模块设置入口。

## 关于页配置项清单（本次可落地）

### 必做配置项
- `about_source_url`：`https://github.com/Kwensiu/DPIS`
- `about_releases_url`：`https://github.com/Kwensiu/DPIS/releases`
- `about_issues_url`：`https://github.com/Kwensiu/DPIS/issues`
- `about_version_format`：例如 `版本：%1$s (%2$d)`
- `about_section_title` / `about_row_*` 文案

### 可选配置项（后续）
- `about_telegram_group_url` / `about_telegram_channel_url`
- `about_open_source_license_url` 或本地 license 数据源
- `about_enable_file_logging`（需日志落盘能力支持）

## 验收标准
- 设置页出现“其他”分组，含“关于”和“隐藏桌面图标”。
- 点击“关于”可进入独立页面并打开 3 个外部链接（源码/更新/反馈）。
- 隐藏图标开关可持久化，切换后桌面图标显示状态正确变化。
- LSPosed 模块设置入口始终可用。
