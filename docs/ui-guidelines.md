# DPIS UI 变更约定

新增或修改 UI 时先归类、再复用共享 chrome，最后才加资源。外观契约在
`CONTEXT.md` **Appearance**，这里只写怎么落地。

这不是视觉重设计方案，也不再以 XML layout 为默认样板。

## 默认规则

现有 Compose 界面是第一基准。新增 UI 前先找同类实现：二级页、分段列表行、确认
dialog、编辑器 sheet。

必须先说明控件属于哪一类：设置开关、设置入口、普通确认、危险确认、选项列表、
长文本，或调试入口。归类不清时不要新开一套圆角和颜色。

颜色只来自 `MaterialTheme.colorScheme`（由 `ComposeDesignSystem` 注入）。成功/
警告用 `LocalSemanticColors`，不要为 Compose 新增 `R.color.dpis_*`，不要导出
Theme Builder 静态色板，不要再引一套 Material 3 组件库。

间距优先 `LocalSpacing`（4/8/12/16/24/32）。不要建 `space_16` 或新的
`*UiTokens` 去包一个 16.dp。多个工作区共用的页面 chrome 只放
`PageChromeTokens`。测过的特征几何（编辑器 peek、拖动手柄）留在该特征包。

名称要短、目录要浅。共享控件用 `SegmentedRow`、`FeedbackButton`、`ModalDialog`、
`SecondaryPageScaffold`。不要加 `ui/theme/tokens` 这类分层，也不要把
`Dpis`/`Material3`/`Policy` 叠进类型名。

以下情况先问维护者：

- 新的 dialog 类型，或把现有确认框用到语义不同的操作上。
- 危险、停止、清除、覆盖、导入等高风险操作。
- 会改多个页面的颜色、间距或列表行形状。
- 无法归入现有 UI 类型。

## 变更检查表

| 需求语言 | UI 分类 | 优先落点 | 注意事项 |
| --- | --- | --- | --- |
| 加一个设置开关 | 设置开关 | `SettingsWorkspaceContent` 分段行；后续收口到共享 `SegmentedRow` | 文案进 `strings.xml`；debug-only 用 `BuildConfig.DEBUG`。 |
| 加一个设置入口 | 设置入口 | 同上 | 图标用现有 `ic_*_24`；保持分组。 |
| 加一个普通确认 | 普通确认 | `ConfirmAlertDialog` / `ModalDialog` | 短内容、少动作。不要新开 `MaterialAlertDialogBuilder`。 |
| 加一个危险确认 | 危险确认 | `ConfirmAlertDialog`，按钮语义用 success/error 角色 | 风险靠标题、正文和按钮文案，不只靠颜色。 |
| 加一组选项 | 选项列表 | 已有 choice menu / segmented row | 选项多或文案长时再考虑滚动或搜索。 |
| 加一段说明或日志 | 长文本 | 独立评估滚动和底部动作 | 底部操作必须始终可见。 |
| 加一个调试入口 | 调试入口 | 对应设置页的 debug-only 分组 | 名称带 `debug_only`。 |

Java 宿主若仍通过 `ConfirmDialog.show` 调确认框，保持该 Java 表面，不要在特征页再包一层 AlertDialog。

## 样板索引

| UI 类型 | 参考 | 使用边界 |
| --- | --- | --- |
| 二级页 | `SecondaryPageScaffold` | 标题、返回、inset、折叠顶栏。不要手写普通 `TopAppBar`。 |
| 工作区页 | `PageScaffold` | Home / Tools / Settings / App / Template。 |
| 分段列表行 | 现有 `SegmentedListItem` + `dpisSegmentedShapes` | 容器色用 `surfaceBright`。新代码不要再复制一份 `segmentedColors`。 |
| 按钮 | `FeedbackButton` / `FeedbackOutlinedButton` | 触感走这里。形状和色用 Material 默认或共享 success/danger，不要每页传 `RoundedCornerShape(16)`。 |
| 普通确认 | `ConfirmAlertDialog`、`ModalDialog` | Compose state 拥有可见性。 |
| 筛选 sheet | `FilterSheet` | 不等于普通 dialog。横向芯片溶入 `owningSurfaceColor`。 |
| 应用配置 sheet | `AppConfigEditorContent`、`AppConfigSheetUiTokens` | 复杂主流程。只保留测过的 peek/手势数字。 |
| 主题设置 | `ThemeSettingsContent` | 改种子色、palette、spec 时必须走 `ColorSchemeFactory`。 |

XML `item_settings_*.xml`、`dialog_*.xml`、`Widget.Dpis.DialogActionButton.*` 是遗留。不要作为新 UI 的起点。还没迁完的 Java 宿主可以继续调用，但不要扩展这些 layout。

## 模板工作区

模板工作区是配置型工作区，不是通用卡片样板。改 `TemplateWorkspaceList` 时保持克制、可扫读、接近 Material；不要做成仪表盘。

- 全局预填卡片表达“未配置应用的默认填入值”，不是批量操作区。
- 快捷模板用设置页分类级别的标题；卡片比全局预填更紧凑。
- 摘要只显示已配置项；缺失字体用警告，不要为未配置维度占位。

## 按钮语义

主操作 filled，次操作 outlined 或 text。危险/警告/成功用共享角色，不要在调用点写 `colorResource(R.color.dpis_success_container)`。

退出动作用 text button，不要压过主操作或危险确认。高风险操作的按钮文案写出结果（停止、覆盖、导入），不要只写“确定”。

## Scroll edge fades

两种溢出处理不要混用。

| 类型 | 语义 | 代码落点 | 颜色规则 |
| --- | --- | --- | --- |
| 内容溶入表面 | 被裁掉的内容溶进所在容器 | `HorizontalScrollWithEdgeFade`、`FilterSheetScrollChipRow`、`dialogListContentFade`、`AppIdentityMarqueeText` | 传 `owningSurfaceColor`，用该容器的 `colorScheme` 表面色。 |
| 列表边界遮罩 | 列表与 chrome 交界的浅阴影 | `edgeOcclusionFade` | 主题无关的黑色低透明，留在 `EdgeOcclusionFadeTokens`。 |

内容溶入表面时，可见度只缩放该表面色的 alpha（`owningSurfaceFadeColor`）。不要写成独立的 `Color.Black`。对应测试：`OwningSurfaceFadePolicyTest`。

## Compose 点击与触觉

离散成功操作使用 `rememberDpisConfirmAction` / `FeedbackButton`。禁用操作、只读信息和滑条连续拖动不触发确认震动。滑条只在跨档时发 tick。

## Tests And Review Gates

UI 变更至少考虑 source smoke。改结构、字符串、debug-only 入口或共享 chrome 时更新对应测试。

| 变更范围 | 优先检查或更新 |
| --- | --- |
| 字符串、语言选项 | `StringResourceParityTest` |
| 设置工作区、主题行 | `ComposeShellSourceSmokeTest`、`SystemServerSettingsLayoutSmokeTest` |
| 配色生成 | `ColorSchemeFactoryTest` |
| 确认 dialog | `ComposeConfirmDialogTest` |
| 应用配置 sheet、字体/Hook 子页 | `AppConfigDialogBinderSourceSmokeTest` |
| 更新弹窗 | `UpdatePromptDialogCoordinatorSourceSmokeTest`、`AboutActivitySourceSmokeTest` |
| 启动免责、主界面入口 | `MainActivitySourceSmokeTest`、`MainActivityLayoutSmokeTest` |
| 滚动边缘溶入表面 | `OwningSurfaceFadePolicyTest` |

推荐验证：定向单测 → `./gradlew :app:testAllDebugUnitTests` → 涉及滚动/高度/底部动作时真机或截图。

提交说明里写清：UI 归类、复用的 chrome、是否新增颜色/间距例外、跑过的测试、未覆盖的显示风险。

## 后续整理顺序

按自底向上推进，不要先改 Settings 或编辑器来“带出”契约。

1. 锁死 `CONTEXT.md` **Appearance**（本文件与代码注释跟它对齐）。不改特征页外观。
2. 在 `ui/` 收口共享 chrome：分段行、确认框视觉、Feedback 的 success/danger。旧调用保持可编译。
3. 按页改特征调用并删除无引用的 `*UiTokens` 和 XML 样板。编辑器 peek/手势留下。

包名与目录对齐（`ui.compose` → 物理路径）是单独一切，不和外观或 chrome 收口混在一个 PR。

## Out Of Scope

| 暂不处理 | 原因 |
| --- | --- |
| 整体视觉重设计 | 先收口现有 Material 3，不换皮肤。 |
| 再引入 Theme Builder / 第二套 MD3 库 | 与 `ColorSchemeFactory` 抢权。 |
| 一次改完编辑器 sheet | 几何已测过，留给特征页自己的 PR。 |
| 把 `composeMaterial3` 立刻钉回 BOM | Expressive API 稳定性单独评估。 |
