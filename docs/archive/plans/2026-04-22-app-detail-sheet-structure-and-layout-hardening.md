# 应用详情 Sheet 结构与布局容错方案（2026-04-22）

## 目标

一次性解决两类问题，且不引入大规模 UX 改版：

1. `showEditDialog()` 结构过重、维护成本高。
2. `dialog_app_config` 在小屏/大字号下有可用性风险。

## 非目标

- 不改变当前动作语义：`保存`、`重置`、`作用域`、`DPIS`、`启动/重启/停止` 的职责保持不变。
- 不引入“外部状态监听/轮询刷新”。状态刷新仅由 Sheet 内部动作触发。
- 不重做视觉风格，不调整现有操作分组。

## 方案总览

### A. 控制器层轻量解耦（不改行为）

保留 `showEditDialog(AppListItem item)` 作为入口方法，但把职责拆分为小方法：

- `initDialogViews(...)`：只做 `findViewById` 与视图对象组装。
- `bindDialogInitialState(...)`：只做初始数据回填、初始状态文案渲染。
- `bindDialogValidation(...)`：只做输入校验与 `保存` 可用态控制。
- `bindDialogActions(...)`：只做点击事件绑定与动作分发。

建议引入一个内部状态载体（例如 `AppConfigDialogState`），集中管理：

- `scopeSelected`
- `dpisEnabled`
- 当前输入解析值（可选）

并明确“状态单一来源（Single Source of Truth）”约束：

- `AppConfigDialogState` 是 Sheet 交互态的唯一真值来源。
- UI 组件只读 `state` 渲染；点击事件只改 `state`，不直接散改多个 View。
- 所有状态变更后统一走 `render(state)`（或等价方法）刷新 UI。

目的：减少 `showEditDialog()` 中散落闭包状态与跨区域耦合，避免“拆分后仍有多处真值”。

### B. 布局层容错增强（保持视觉基本不变）

针对 `dialog_app_config.xml` 做最小结构增强：

1. 外层增加 `NestedScrollView`（`fillViewport=true`）。
2. 原有内容保留在内层 `LinearLayout`。
3. 将模式行从“固定高度”改为“自适应 + 最小高度”策略：
   - 行容器使用 `wrap_content`
   - 同时设置 `android:minHeight`（沿用当前 `56dp` 语义）

目的：在小屏和系统大字号下确保内容可滚动、按钮可达，避免挤压截断。

### C. 验收与回归

#### 手工验收

- 普通字号、常规屏幕：视觉与交互无变化。
- 小屏设备：底部动作区可滚动到可见并可点击。
- 大字号（系统字体放大）：输入框、模式切换、按钮不重叠。
- 回归关键链路：
  - 保存后不自动关闭 Sheet。
  - 仅 Sheet 内部动作触发状态刷新。

#### 自动化验收

- 增加/更新布局 smoke test：断言 `dialog_app_config` 包含 `NestedScrollView`。
- 增加 1 条行为回归断言：
  - 执行 `disable/reset` 后，viewport/font mode 都回到 `FIELD_REWRITE`。
  - 同时断言 `save` 按钮可用态与输入错误态同步更新。
- 增加 1 条布局回归断言：
  - 除“包含 `NestedScrollView`”外，断言模式行不再使用固定
    `layout_height=@dimen/dialog_mode_toggle_row_height`。
- 运行全量单测：`./gradlew :app:testDebugUnitTest`。

## 影响文件

- `app/src/main/java/com/dpis/module/MainActivity.java`
- `app/src/main/res/layout/dialog_app_config.xml`
- `app/src/main/res/values/dimens.xml`
- `app/src/test/java/com/dpis/module/*LayoutSmokeTest.java`（按现有测试组织补充）

## 风险与回滚

### 风险

- 布局层改动可能影响底部弹窗初始展开高度。
- 方法拆分可能引入遗漏绑定（可通过手工点击路径与 smoke test 兜底）。

### 回滚策略

- 布局改动与代码拆分分两个 commit 提交。
- 若出现 UI 异常，可先回滚布局 commit，保留代码解耦 commit。

## 预期收益

- 代码可维护性显著提升：`showEditDialog()` 不再继续膨胀。
- 低成本提升 UI 鲁棒性：极端显示条件下仍可操作。
- 用户感知变化极小：无需重新学习交互。
