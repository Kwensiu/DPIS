# 真机验证指南

## 第一版 PoC 已验证范围

- 版本边界：第一版仅做内容缩放，不做窗口/显示信息伪装。
- 当前目标包：
  - `bin.mt.plus.canary`
  - `com.max.xiaoheihe`
- 当前目标 DPI：`560`

## 当前验证结论

1. 两个目标包都已在真机上命中模块作用域。
2. `ResourcesManager.applyConfigurationToResources(...)` 已将 `Configuration.densityDpi` 从 `480` 覆写到 `560`。
3. `ResourcesImpl.updateConfiguration(...)` 已将 `DisplayMetrics.densityDpi/density/scaledDensity` 同步到目标值。
4. `remote preferences` 读取链路正常，状态页展示值与 Hook 日志一致。
5. 非目标包会被正确跳过，例如日志中已出现 `package not configured: package=com.google.android.webview.dev`。

## 推荐验证步骤

1. 先启动模块应用一次，确保 `remote preferences` 已初始化。
2. 启动或重启目标包，然后过滤 `DPIS` 日志：
   - 命令：`adb -s 192.168.5.130:5555 logcat | findstr DPIS`
3. 观察关键日志：
   - `module loaded: process=<package>`
   - `target package matched: package=<package>, targetDensityDpi=560`
   - `ResourcesManager override: densityDpi 480 -> 560`
   - `ResourcesImpl override: configDensityDpi 560 -> 560, metricsDensityDpi=560`
4. 观察目标应用的界面缩放是否符合预期，并确认非目标包没有命中日志。

## 下一步设计：最小可编辑配置页（只支持两个固定包）

1. 目的：把当前 PoC 推进成可用最小版，让 `bin.mt.plus.canary` 与 `com.max.xiaoheihe` 可以分别修改目标 DPI。
2. 约束：只支持这两个固定包，不做动态包列表，也不做窗口/显示信息伪装。
3. 页面结构：
   - 顶部说明当前是模块配置页
   - 两个固定包各自一块编辑区域
   - 每块区域包含：包名、当前目标值、当前生效值、DPI 输入框、保存按钮
   - 底部继续保留 Hook 列表、作用域说明与验证提示
4. 交互约束：
   - 输入正整数表示新的目标 DPI
   - 留空表示禁用该包目标 DPI
   - 保存后需要重启目标 应用 进程再观察生效结果

