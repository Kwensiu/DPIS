# Legacy 模块对比记录（归档）

## 对比范围

本文用于对比四种模块打包形态：

- `DPIS`
- `AppSettingsR`
- `SetAppFull`
- `InxLocker`

目标是定位：为什么 `DPIS` 在 LSPosed 中被记录到 `system`，但实际没有在 `system` 执行。

## 已知可用模块的共同点

这些已知可用模块普遍采用传统执行路径：

- 存在 `assets/xposed_init` 且指向单一 Java 入口类。
- Manifest 包含 `xposedmodule`、`xposedminversion`、`xposeddescription`、`xposedscope`。
- 包可作为普通应用使用（启动器入口或模块设置页）。

## 参考模块摘要

### AppSettingsR

- 包名：`ru.bluecat.android.xposed.mods.appsettings`
- 入口：`ru.bluecat.android.xposed.mods.appsettings.hooks.Core`
- 结构：传统 `xposed_init` + Manifest `meta-data`

### SetAppFull

- 包名：`ss.colytitse.setappfull`
- 入口：`ss.colytitse.setappfull.MainHook`
- 结构：传统 `xposed_init` + Manifest `meta-data` + `MODULE_SETTINGS`

### InxLocker

- 包名：`io.github.chimio.inxlocker`
- 入口：`io.github.chimio.inxlocker.hook.HookEntry_YukiHookXposedInit`
- 结构：传统 `xposed_init` + Manifest `meta-data` + `MODULE_SETTINGS` + launcher alias

## 当时 DPIS 的形态

当时 `DPIS` 是混合打包策略：

- 传统 `assets/xposed_init`
- 传统 Manifest `meta-data`
- 现代 `META-INF/xposed/java_init.list`
- 现代 `META-INF/xposed/module.prop`
- 现代 `META-INF/xposed/scope.list`
- libxposed provider：`io.github.libxposed.service.XposedProvider`

因此它并非“纯 legacy”模块，而是混合模块。

## 当时工作假设

保守解释是：LSPosed 能正确识别包与作用域，但该设备上混合模块在 `system` 的真实执行链路没有被调度起来。

## 对照实验

为剔除 DPIS 本身变量，提出构建独立 `legacy-only` APK，要求：

- 独立包名；
- 仅保留 `assets/xposed_init` 与传统 Manifest `meta-data`；
- 不包含 libxposed provider；
- 不包含 `META-INF/xposed/*`；
- 仅保留一个 `IXposedHookLoadPackage` 入口并输出稳定日志标记。

若该 APK 能在 `system` 执行，问题更偏向 DPIS 打包形态，而非设备本身不支持 legacy 执行。

## 补充说明

`YukiSmoke` 曾作为历史对照实验存在，现已退役并移出当前测试基线。
