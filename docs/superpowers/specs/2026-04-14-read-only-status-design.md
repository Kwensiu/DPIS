# 原生只读状态页设计

## 目标
1. 让 `MainActivity` 成为一个只读状态页，展示当前的 DPI 针对目标包的概况，而不是原本那句程序性输出。
2. 页面只读、不允许输入或多 app 列表，内容包括目标包名、目标 DPI、已安装的两个 Hook、静态作用域描述和真机验证提示。
3. 补一份简短的真机验证文档，列出关键 logcat 关键字和感知现象，方便运行人员确认模块是否起效。

## UI 架构
- 使用 `res/layout/activity_status.xml` 定义一个 `ScrollView` 包裹垂直 `LinearLayout`，在 `LinearLayout` 中依次放置：一种引导文字、目标信息块、Hook 名称块、静态作用域说明块、真机验证提示块。
- 每个信息块由标题 TextView 和正文 TextView 组成，使用合理的 padding/spacing，借助 DeviceDefault 主题和 `textAppearance` 样式列出标题与正文，确保内容在竖向滚动时可辨。
- 不提供任何可编辑控件、多包列表或交互菜单，仅用于展示静态信息。

## Activity 逻辑
- `MainActivity` 通过 `setContentView(R.layout.activity_status)` 加载 layout，然后依次查找 `R.id.status_target_info`、`R.id.status_hooks` 等正文 TextView。
- 目标信息块的文本由 `DpiConfig.TARGET_PACKAGE` 与 `DpiConfig.TARGET_DENSITY_DPI` 拼接而成；Hook 名称块列出 `ResourcesManager.applyConfigurationToResources` 与 `ResourcesImpl.updateConfiguration`。
- 静态作用域说明写在资源文件中，内容描述 `DpiConfig.shouldHandlePackage` 只对目标包返回 `true`，Hook 均只修改 `Configuration.densityDpi`（以及 `DisplayMetrics`），不会影响其它包。
- 验证提示中的消息同样来自字符串资源，提醒团队留意 `DPIS` 记号的 logcat 条目，观察目标 app 的 density 是否变为 560，而系统其他 app 继续用默认 DPI。
- 所有可见文本可复用少量 `strings.xml` 条目，便于未来翻译。

## 日志与作用域说明
- 运行时通过 `ModuleMain` 的 `DpisLog.i("installed ResourcesManager and ResourcesImpl hooks for ...")` 确认是否成功安装了两个 Hook。
- `ResourcesManagerHookInstaller` 打印 `override densityDpi in ResourcesManager: ...`，`ResourcesImplHookInstaller` 在 `applyDensityOverride` 中更新 `DisplayMetrics`，这些 `DPIS` logcat 输出表明 Hook 正在替换 `densityDpi`。
- 页面会强调只有在 `DpiConfig` 指定的目标包才会执行上述 Hook，其他 app 不会触发 hook 思路，静态 scope 只在目标包内生效。

## 真机验证提示
- 查看 logcat 时，仅保留 `DPIS` TAG，确认出现如下关键词：`installed ResourcesManager and ResourcesImpl hooks`、`override densityDpi in ResourcesManager`、`override densityDpi in ResourcesImpl`。这些 log 同时确认钩子被触发并修改 `densityDpi`。
- 观察目标包 UI 渲染结果是否等效于 560dpi：可以在目标 app 的显示设置里看 DPI 指示，或通过截图与默认 DPI 结果对比；也可辅助通过 `DensityOverride` 生成的 `density`/`scaledDensity` log。
- 验证其他 app（例如 Settings 以外的系统应用）仍然使用原始 DPI，确保 Hook 仅作用于目标包。

请先 review 这个 spec；我写完后会请您在文件里确认然后继续实施计划。
