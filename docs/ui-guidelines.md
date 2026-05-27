# DPIS UI 变更约定

本文档约束 DPIS 新增或修改 UI 时的判断流程。它不是完整的设计系统，也不是整体视觉重设计方案。

目标是让维护者和 AI 代理在添加控件、弹窗、设置项或调试入口时，能先归类、再复用、最后才新增资源，避免每次 UI 变更都临时决定 spacing、style、layout 和测试边界。

## 默认规则

现有 UI 是第一基准。新增 UI 前先查找同类实现，再决定是否复用现有 layout、style、dimen、color、drawable 或 string。

新增控件时必须先说明它属于哪一类，例如设置开关、设置入口、普通确认、危险确认、选项列表、长文本说明或调试入口。归类不清时，不应直接套用相似 XML。

默认遵守现有模式；例外必须有理由。常见理由包括：控件承担不同语义、需要适配长内容、受 Material 组件默认行为限制、或目标页面已有特殊结构。

以下情况需要先询问维护者：

- 新增全新类型的 dialog，或把现有 dialog 模式用于语义不同的场景。
- 基于现有模板新增普通 dialog，但内容长度、滚动、底部按钮可见性或屏幕高度适配不确定。
- 新增危险、停止、清除、覆盖、导入等高风险操作。
- 新增复杂表单、长文本区域或可滚动内容。
- 新增全新的视觉模式、按钮类型、列表项类型或容器样式。
- 调整会影响多个页面的 dimen、style、color 或 drawable。
- 无法把需求明确归入现有 UI 类型。

## 变更检查表

| 需求语言 | UI 分类 | 优先代码落点 | 注意事项 |
| --- | --- | --- | --- |
| 加一个设置开关 | 设置开关 | `item_settings_switch.xml` | 文案进 `strings.xml`；debug-only 入口必须用 `BuildConfig.DEBUG` 控制。 |
| 加一个设置入口 | 设置入口 | `item_settings_entry.xml` | 图标优先使用现有 `ic_*_24.xml` 风格；保持页面已有分组和分隔规则。 |
| 加一个普通确认弹窗 | 普通确认 dialog | 先查 `dialog_*_confirm.xml` | 内容短、动作少；优先复用 `dialog_surface_*` 与 dialog button style。 |
| 加一个危险确认弹窗 | 危险/警告 dialog | 先查 process/config confirm 类弹窗 | 操作语义必须体现在按钮 style 和字符串中；不要只靠颜色表达风险。 |
| 加一组选项 | 选项列表 | 先查 language/typeface option 模式 | 选项较多或文本较长时，先判断是否需要滚动或搜索。 |
| 加一段说明或日志 | 长文本说明 | 独立评估布局 | 先确认内容是否会变长、是否可滚动、底部操作是否始终可见。 |
| 加一个调试入口 | 调试入口 | 对应设置页或 debug-only 分组 | 必须使用 `debug_only` 命名，并保持 release 布局静态分隔不破坏。 |

## 现有样板索引

这些文件是新增 UI 时的优先参考点。它们不代表已经完全规范化；如果文件内仍有局部数值，应先判断这些数值是否需要在本次变更中整理。

| UI 类型 | 参考文件 | 使用边界 |
| --- | --- | --- |
| 设置开关 | `item_settings_switch.xml` | 用于标题、说明、图标和右侧 switch 的设置行。 |
| 设置入口 | `item_settings_entry.xml` | 用于进入二级页面、弹窗或工具流程的设置行。 |
| 普通 dialog | `dialog_config_backup.xml`、`dialog_config_backup_confirm.xml` | 用于短内容、少量动作的确认或选择。 |
| 进程操作确认 | `dialog_process_action_confirm.xml` | 用于停止、重启、启动等带风险语义的确认动作。 |
| 语言选项 | `dialog_language_selection.xml`、`item_language_option_button.xml` | 用于少量互斥选项。 |
| 字体选项 | `dialog_typeface_selection.xml` | 用于 tab 加滚动选项的复杂选择；整理前不要直接作为普通 dialog 样板。 |
| 字体域设置 | `dialog_font_hook_domains.xml`、`item_font_hook_domain.xml` | 用于领域配置，不按普通选项列表直接套用。 |
| 调试统计 | `dialog_font_debug_stats.xml` | 用于调试和长文本输出；涉及滚动、高度和按钮可见性时必须单独验证。 |
| 筛选底部面板 | `dialog_list_filters.xml` | 用于底部筛选；不等同于常规 dialog。 |
| 应用配置底部弹窗 | `dialog_app_config.xml` | 复杂主流程容器，不作为一般新增控件样板。 |
| 更新弹窗 | `dialog_update_available.xml` | 独立处理，不作为长内容 dialog 的通用参考。 |

## 按钮样式索引

按钮样式集中定义在 `app/src/main/res/values/styles.xml`。新增按钮前先按动作语义选择现有 style。

| Style | 语义 | 常见位置 |
| --- | --- | --- |
| `Widget.Dpis.DialogActionButton.Filled` | 主操作、确认、继续 | 普通 dialog、引导、保存动作 |
| `Widget.Dpis.DialogActionButton.Outlined` | 次操作、管理、取消的强调版本 | 普通 dialog、配置入口 |
| `Widget.Dpis.DialogActionButton.Outlined.Error` | 错误或破坏性操作 | 需要明确停止、删除、清除语义时 |
| `Widget.Dpis.DialogActionButton.Outlined.Warn` | 警告操作 | 需要提示风险但不一定破坏数据时 |
| `Widget.Dpis.DialogActionButton.Outlined.WarnOutline` | 低强调警告操作 | 风险提示弱于 `Warn` 时 |
| `Widget.Dpis.DialogActionButton.Outlined.WarnBg` | 高可见度警告操作 | 进程确认、恢复默认等需要突出风险的动作 |
| `Widget.Dpis.DialogActionButton.Outlined.Success` | 成功或启动类操作 | 与停止、重启等并列的正向进程动作 |
| `Widget.Dpis.DialogActionButton.Process` | 紧凑进程操作按钮基类 | 应用配置底部弹窗的多按钮行 |
| `Widget.Dpis.DialogActionButton.Process.Error` | 紧凑停止类操作 | 停止进程 |
| `Widget.Dpis.DialogActionButton.Process.Warn` | 紧凑重启类操作 | 重启进程 |
| `Widget.Dpis.DialogActionButton.Process.Success` | 紧凑启动类操作 | 启动进程 |
| `Widget.Dpis.DialogActionButton.LanguageOption` | 选项按钮基类 | 语言、字体等选项列表 |
| `Widget.Dpis.DialogActionButton.LanguageOption.Default` | 默认选项按钮 | `item_language_option_button.xml` |
| `Widget.Dpis.DialogActionButton.TypefaceOption` | 更紧凑的字体选项按钮 | 字体选择 dialog |

如果新增按钮无法清楚落入上表，不要直接写局部颜色或圆角。先说明动作语义，再决定是否新增 style。

## Spacing And Dimensions

常规 spacing、padding、margin 优先使用 4dp 步进。这个规则用于保持布局节奏，不用于机械替换所有数值。

不要建立 `space_4`、`space_8`、`padding_12dp` 这类纯数字 token。进入 `dimens.xml` 的值应该表达用途，例如 `dialog_body_spacing`、`dialog_surface_padding_horizontal`。

符合以下任一条件时，优先放入 `dimens.xml`：

- 被多个 layout 复用。
- 表达组件或页面语义。
- 调整后会影响同类 UI 的一致性。
- 测试或代码需要稳定引用。

以下情况可以保留局部数值：

- 单个控件内部的微调。
- Material 组件默认行为所需的兼容值。
- 图标、触控区域、文字基线、state layer 等有组件语义的尺寸。
- 只在一个布局中出现，且抽成 token 反而降低可读性。

不要为了“全部 token 化”制造大量只使用一次、没有语义的 dimen。

## Dialogs

Dialog 必须先按内容和操作归类。

短确认 dialog 用于少量说明加一到两个动作。它应优先使用 `dialog_surface_padding_horizontal`、`dialog_surface_padding_top`、`dialog_surface_padding_bottom`、`dialog_body_spacing`、`dialog_action_spacing_top` 和 `dialog_action_spacing_between`。

危险或警告 dialog 用于停止、覆盖、删除、清除、导入替换等操作。风险要通过标题、正文、按钮文案和按钮 style 共同表达，不要只依赖颜色。

选项 dialog 用于语言、字体等互斥选择。选项按钮应优先复用现有 `Widget.Dpis.DialogActionButton.LanguageOption` 或相邻模式；新增样式前先确认现有样式为什么不适合。

长内容 dialog 用于更新日志、说明、统计或调试信息。这类 dialog 必须先确认三个问题：内容如何滚动、底部操作是否始终可见、不同屏幕高度下最大高度如何计算。

更新弹窗暂时独立处理，不作为通用 dialog 样板。修改更新弹窗前需要单独设计和验证。

## Buttons And Actions

按钮样式应先按动作语义归类。

主操作使用 filled button。次操作使用 outlined 或 text button，取决于所在 dialog 的密度和层级。

危险、警告、成功类操作优先复用 `Widget.Dpis.DialogActionButton.Outlined.Error`、`Warn`、`WarnOutline`、`WarnBg` 或 `Success`。如果这些命名无法表达实际语义，先讨论是否新增样式，而不是直接写局部颜色。

关闭、取消、稍后等非破坏性退出动作通常使用 text button。不要让退出动作在视觉上压过主操作或危险确认操作。

按钮文本应清楚表达结果。高风险操作不要只写“确定”，应写出动作，例如停止、覆盖、导入或清除。

## Tests And Review Gates

UI 变更至少要考虑资源 smoke test。影响 layout id、字符串、debug-only 入口、dialog 结构或关键资源命名时，应新增或更新对应测试。

常见测试落点：

| 变更范围 | 优先检查或更新 |
| --- | --- |
| 字符串资源、语言选项、基础资源一致性 | `StringResourceParityTest` |
| 系统设置页、关于页、备份 dialog、设置行 | `SystemServerSettingsLayoutSmokeTest` |
| 进程操作确认 dialog | `ProcessActionHandlerSourceSmokeTest` |
| 应用配置底部弹窗、字体选择、字体域设置 | `AppConfigDialogBinderSourceSmokeTest` |
| 更新弹窗布局和 release notes 容器 | `AboutActivitySourceSmokeTest` |
| 启动免责声明、运行时重载提示、主界面入口 | `MainActivitySourceSmokeTest`、`MainActivityLayoutSmokeTest` |
| 筛选底部面板 | `FilterSheetLayoutSmokeTest` |
| 帮助教程 dialog | `HelpTutorialDialogLayoutSmokeTest` |

推荐验证顺序：

1. 先运行与变更相关的定向单元测试。
2. 再运行 `./gradlew :app:testAllDebugUnitTests`。
3. 涉及真实显示、滚动、高度、底部操作可见性时，补充真机或截图验证。

AI 代理在提交 UI 改动说明时，应包含：

- UI 归类。
- 复用的现有模式。
- 新增或例外的 dimen/style/color/drawable。
- 执行过的测试。
- 未覆盖的显示风险。

## 后续整理批次

UI 规范化应按小批次推进。每个批次只处理一种 UI 类型，并在开始前说明本批次的归类、涉及文件和不处理范围。

推荐顺序：

1. **普通 dialog 收敛**：只处理短确认、备份、语言选择这类结构简单的 dialog，目标是复用 `dialog_surface_*`、`dialog_body_spacing` 和 dialog button style。
2. **设置行收敛**：整理 `item_settings_switch.xml` 和 `item_settings_entry.xml` 的 spacing、图标尺寸、标题/副标题间距，并决定是否新增 settings row 语义 dimen。
3. **按钮语义收敛**：检查 dialog action button 的高度、圆角、状态色和文字规则，必要时新增少量有语义的 button dimen。
4. **复杂选择 dialog 评估**：单独评估 `dialog_typeface_selection.xml`，重点是 tab、滚动区域、底部按钮和屏幕高度适配。
5. **调试/统计 dialog 评估**：单独评估 `dialog_font_debug_stats.xml`，重点是长文本区域、monospace 内容、清除/关闭动作和真机显示。
6. **底部弹窗另案处理**：`dialog_app_config.xml` 和 `dialog_list_filters.xml` 作为 bottom sheet 类 UI，单独建立规则，不混入普通 dialog 批次。

暂时不要把更新弹窗加入以上批次。它需要先重新确认内容展开、窗口高度、底部按钮可见性和设备验证方式。

## Out Of Scope

第一版规范不处理以下事项：

| 暂不处理事项 | 跟踪方式 |
| --- | --- |
| UI 技术栈迁移 | 不进入当前 UI 规范化计划。 |
| 整体视觉重设计 | 不进入当前 UI 规范化计划。 |
| 更新弹窗结构修复 | 需要单独设计，不能跟普通 dialog 批次合并。 |
| app 配置底部弹窗重构 | 归入“底部弹窗另案处理”批次。 |
| 字体域编辑器重构 | 归入“复杂选择/领域配置 dialog 评估”批次。 |
| 统计面板重构 | 归入“调试/统计 dialog 评估”批次。 |
| 引导气泡和复杂交互说明 | 后续建立复杂交互规则时再处理。 |

这些 UI 需要单独设计，不能按常见控件规则直接批量套用。

## 剩余复杂 UI 评估

当前 UI 收敛刻意把几类复杂界面留在通用 dialog 规则之外。

| 界面 | 当前判断 | 下一步安全动作 |
| --- | --- | --- |
| `dialog_font_debug_stats.xml` | 调试 bottom sheet，包含筛选按钮、长文本、overlay 控制、清除和关闭动作。 | 保持现有行为；用 `font_debug_dialog_*` dimen 记录当前值。改变统计面板高度策略前，需要真机验证。 |
| `dialog_font_hook_domains.xml` | 领域编辑器，包含 tab、已知/未知分组、恢复推荐和动态生成行。 | 按领域编辑器处理。未审查 `FontHookDomainDialog` 前，不套普通选项列表规则。 |
| `dialog_list_filters.xml` | 紧凑筛选 bottom sheet，不是普通 dialog。 | 先建立 bottom sheet spacing 规则；保持拖拽条和纯开关结构。 |
| `dialog_app_config.xml` | 主应用配置 bottom sheet，包含输入、分段切换、进程操作、保存和高级控制。 | 避免大范围 XML 格式化或 spacing 替换。只在具体 app 配置工作流需要时修改。`viewport` 采用双草稿持久化：`scale` 与 `width` 分别保存，当前模式只决定生效目标；空值清除对应草稿，非法值不覆盖已存草稿。 |
| `dialog_help_tutorial.xml` | 说明型卡片 dialog，有自定义卡片和 badge 样式。 | 按帮助/教程内容处理，不强行套普通 dialog spacing。 |
| `dialog_update_available.xml` | 更新 dialog，包含可展开更新日志和安装/取消动作。 | 继续排除；等高度、滚动和底部动作可见性重新设计并真机验证后再处理。 |
