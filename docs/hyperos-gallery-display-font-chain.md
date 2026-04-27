# HyperOS Gallery DP and Font Logic Chain

本文解释 `com.miui.gallery` 在 DPIS 中的 DP、字体伪装、字体替换、HyperOS Rust/Flutter native hook 的完整链路。它基于当前代码、实机调试日志和相册最新包验证结果整理。

## 当前结论

- `com.miui.gallery` 不是内置白名单目标；只有用户在 DPIS 中配置后才进入 `target_packages`。
- 普通 Java 应用和 Gallery 的生效链路不同，不能把 Gallery 的问题泛化为所有应用都必须重启设备。
- 普通 Java 应用通常在目标进程重启后就能重新加载 app 进程 hook，因此覆盖安装 DPIS 后往往可以即时调整。
- Gallery 的关键 DP/字体链路额外依赖 `system_server` 和 HyperOS RustProcess hook；覆盖安装 DPIS 不会让已运行的 `system_server` 自动重新加载新模块代码。
- 普通字体伪装主要通过 `Configuration.fontScale` 下发，但 Gallery 的 Flutter 文本不会完全服从这条 Java 链路。
- Gallery 的最终可控字体入口在 HyperOS Rust/Flutter native 链路：`RustProcessImpl.startRustProcess` → sibling `libdpis_native.so` → `libhyper_os_flutter.so` 的 paragraph 构建入口。
- “重置后仍保持旧字体”的根因曾是 native system property 残留，而不是 DPIS UI 配置未删除；当前已通过 `HyperOsNativeFontPropertySyncer` 修复。

## 1. 配置层

Gallery 的配置由 `dpi_config` 保存，核心 key 如下：

- `target_packages`：目标包集合，Gallery 只有被配置后才会加入。
- `viewport.com.miui.gallery.width_dp`：目标视口宽度。
- `viewport.com.miui.gallery.mode`：DP/视口模式，常见为 `system_emulation` 或 `field_rewrite`。
- `font.com.miui.gallery.scale_percent`：目标字体百分比，范围 `50..300`。
- `font.com.miui.gallery.mode`：字体模式，常见为 `system_emulation` 或 `field_rewrite`。
- `target.com.miui.gallery.dpis_enabled`：临时禁用开关；`false` 表示保留配置但运行时不应用。
- `font.hyperos_flutter_hook_enabled`：实验性 HyperOS Flutter 字体 hook 总开关。

关键代码：

- `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- `app/src/main/java/com/dpis/module/AppConfigSaveHandler.java`
- `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`
- `app/src/main/java/com/dpis/module/TargetViewportWidthResolver.java`

### 保存与重置

- 清空输入框只是修改 UI 状态；真正写入发生在点击保存后。
- 清空 DP 输入并保存，会移除 `viewport.*.width_dp` 和 `viewport.*.mode`。
- 清空字体输入并保存，会移除 `font.*.scale_percent` 和 `font.*.mode`，并调用 `HyperOsNativeFontPropertySyncer.clearFontTargetAsync(...)` 清理 native 属性。
- 如果 DP 和字体都清空，Gallery 会从 `target_packages` 移除。
- 点击“已启用/启用修改”这类 DPIS 开关，写的是 `target.<pkg>.dpis_enabled=false`；它不会删除旧配置，只是运行时屏蔽。

## 2. DP / 视口链路

DPIS 对 Gallery 的 DP/视口有两条路径：

1. app 进程 Java hook：处理 `Resources`、`Display`、`WindowMetrics` 等 Java 层查询。
2. system_server hook：处理启动、窗口、DisplayInfo、Configuration 等系统侧来源。

system_server 的主要入口是 `SystemServerDisplayEnvironmentInstaller`：

- 每次命中 hook 时通过 `PerAppDisplayConfigSource` 重新读配置。
- 如果 `target.com.miui.gallery.dpis_enabled=false`，`source.get("com.miui.gallery")` 返回 `null`。
- 如果 Gallery 有有效视口宽度，计算目标 `Configuration.screenWidthDp`、`screenHeightDp`、`smallestScreenWidthDp`、`densityDpi`。
- 对部分路径还会调整 frame、DisplayInfo、WindowMetrics 相关对象。

恢复默认的条件：

- 删除 `viewport.com.miui.gallery.*` 并保存。
- 或把 `target.com.miui.gallery.dpis_enabled=false` 作为临时屏蔽。
- 重启 Gallery，必要时重启 system_server/设备以清掉运行中缓存。

## 3. 普通字体链路

字体配置由 `font.com.miui.gallery.scale_percent` 和 `font.com.miui.gallery.mode` 决定。

### 伪装模式

`font.mode=system_emulation` 时，system_server/app Java 链路会尝试写入或伪装 `Configuration.fontScale`。

这条链路对普通 Android View/TextView 更有效，但 Gallery 的关键界面大量走 HyperOS Flutter/Rust，因此只靠 `fontScale` 不能保证最终文本变化。

### 替换/兜底模式

`font.mode=field_rewrite` 时，system_server 不把它当成 `fontScale` 伪装配置；Java app 进程中会走 `Paint`、`TextPaint`、`TextView`、span、WebView 等兜底 hook。

这条链路对普通 Java 文本有效，但 Gallery 的 Flutter 文本仍可能绕过 Java hook。

## 4. HyperOS Rust / Flutter native 链路

Gallery 的关键字体效果最终由 native 链路控制。

### 启动拦截

`HyperOsRustProcessHookInstaller` 在 system_server 中 hook：

- 类：`android.os.RustProcessImpl`
- 方法：`startRustProcess`
- package 参数：`args[1]`
- 原始 Rust so 路径：`args[20]`
- env 参数：`args[21]`

对 Gallery，如果配置有效，它会：

- 发布 `debug.dpis.font.a55b5fe1=<percent>`。
- 发布 `debug.dpis.rustbin.a55b5fe1=<原始 libapp_gallery.so 路径>`。
- 尝试把启动 binary path 改为原始 so 同目录下的 sibling `libdpis_native.so`。
- 追加 `DPIS_PACKAGE`、`DPIS_FONT_SCALE_PERCENT`、`DPIS_RUST_BINARY` 到 Rust env 字符串中。

Gallery 的 hash：

- `com.miui.gallery` → `a55b5fe1`
- 字体属性：`debug.dpis.font.a55b5fe1`
- 原始 binary 属性：`debug.dpis.rustbin.a55b5fe1`

### Native proxy

`libdpis_native.so` 必须位于 Gallery 原始 native lib 目录，例如：

```text
/data/app/MIUIGallery/lib/arm64/libdpis_native.so
```

原因是 HyperOS Rust spawner 的 namespace 更容易接受目标应用 native 目录中的 sibling so。DPIS APK 自己携带的 so 只能保证 DPIS 自身加载，不能保证 Gallery Rust 进程能直接加载。

当前 DPIS 做了检测提示：

- `HyperOsNativeProxyStatus` 读取 `ApplicationInfo.nativeLibraryDir`。
- 检查目标目录中是否存在 `libdpis_native.so`。
- 应用配置弹窗显示“已检测到 / 未检测到 / 无法检测”。

### Flutter 文本 hook

`app/src/main/cpp/dpis_native.cpp` 的主要逻辑：

- 读取 `debug.dpis.forcefont`，作为最高优先级调试覆盖。
- 尝试读取 env / cmdline 中的 `DPIS_FONT_SCALE_PERCENT`。
- 读取 `debug.dpis.font.<hash>` 作为正式字体目标。
- 读取 `debug.dpis.rustbin.<hash>` 并 `dlopen()` 原始 `libapp_gallery.so`。
- 转发原始 `app_entry_point()`，保证 Gallery 正常启动。
- hook `libhyper_os_flutter.so` 的文本入口。

当前关键 offset：

- `ParagraphBuilder::Create`：`0x81c368`
- `ParagraphBuilder::pushStyle`：`0x82370c`

当前稳定主路线是 `ParagraphBuilder::Create`；`pushStyle` 更依赖函数签名和寄存器约定，只应作为实验路径。

## 5. Gallery 生效条件

### DP / 视口生效

- Gallery 在 `target_packages` 中，或有有效 per-package 配置。
- `target.com.miui.gallery.dpis_enabled` 没有被设为 `false`。
- `viewport.com.miui.gallery.width_dp` 有效。
- mode 最终不是 `off`。
- app 进程和 system_server 对应 hook 已安装。
- 重启 Gallery 后读取新配置。

### 普通字体伪装生效

- `font.com.miui.gallery.scale_percent` 有效。
- `font.com.miui.gallery.mode=system_emulation`。
- system_server/app Java hook 能改到 `Configuration.fontScale`。
- 目标文字确实走 Android Java 文本路径。

### HyperOS Flutter 字体生效

- Gallery 字体百分比有效。
- system_server hook 能拦到 `RustProcessImpl.startRustProcess`。
- `debug.dpis.font.a55b5fe1` 发布为正数。
- `debug.dpis.rustbin.a55b5fe1` 指向原始 `libapp_gallery.so`。
- Gallery native 目录存在 sibling `libdpis_native.so`。
- Gallery 重启后通过代理启动。
- `libhyper_os_flutter.so` 版本仍匹配当前 offset。

## 6. 恢复默认条件

完整恢复默认需要清理三类状态。

### 配置层

- 清空 DP 输入并保存。
- 清空字体输入并保存。
- 或禁用该目标包的 DPIS 修改。

### Native 属性层

当前代码会自动清理：

- `debug.dpis.font.a55b5fe1=0`
- `debug.dpis.rustbin.a55b5fe1=0`

如果曾做过手动调试，还应确认：

- `debug.dpis.forcefont=0`
- `debug.dpis.pushstyle` 为空或关闭。

### 运行中进程

- force-stop / 重启 Gallery。
- 如果 system_server hook 代码刚更新，可能需要重启设备或 system_server。
- 如果要完全退出 native proxy 链路，需要移除 Gallery native 目录中的 sibling `libdpis_native.so` 并重启 Gallery。

## 7. 本轮实机经验

这次“相册重置后仍不像默认”的证据链：

- `dpi_config.xml` 中已经没有 `com.miui.gallery`，说明 UI 配置清理成功。
- 设备上仍存在 `debug.dpis.font.a55b5fe1=50`，说明 native 属性层残留。
- Gallery 目录仍存在 `/data/app/MIUIGallery/lib/arm64/libdpis_native.so`，所以 native proxy 仍可能参与启动。
- 手动把 `debug.dpis.font.a55b5fe1`、`debug.dpis.rustbin.a55b5fe1`、`debug.dpis.forcefont` 置为 `0` 后，用户确认相册恢复正常。
- 因此根因不是配置保存失败，而是 native 属性没有随配置重置同步清理。

对应修复：

- `HyperOsNativeFontPropertySyncer.clearFontTargetAsync(...)` 在字体配置清空和目标包禁用时清理 native 属性。
- `HyperOsFlutterFontBridge.clearTarget(...)` 同时清理 font 和 rustbin 属性。
- 清理值统一写为 `0`，避免空字符串在 shell/root 环境中转义出错。

## 8. 覆盖安装 DPIS 后的分叉链路

同版本 debug APK 覆盖安装 DPIS 后，Android 会替换 APK，并重启 DPIS 自己的 app 进程。

这时要分两类：

- 普通 Java 应用：目标 app 重启后通常会重新加载 DPIS app 进程 hook，因此可以立刻调整。
- Gallery / HyperOS Rust 应用：关键路径还依赖已运行的 `system_server` 中的 RustProcess/system_server hook；覆盖安装不会让 `system_server` 自动重新加载新模块代码，因此可能需要重启设备或重启 system_server/模块运行时。

这解释了用户观察到的现象：普通 Java 软件覆盖安装后可立即调整，但 HyperOS 相册需要重启后才正确应用。

## 8.1 本轮深挖：重装后不重启设备的可行旁路

两名子代理和本地实机链路核对后结论一致：不要把方向切到更底层的 RustPaint、RustCanvas、字体缓存或 Java TextView 扩散 hook。当前最稳的是把 native proxy 视为稳定加载器，把字体百分比做成可热发布配置。

关键观察：

- 当前相册进程已经加载 `/data/app/MIUIGallery/lib/arm64/libdpis_native.so`、`libapp_gallery.so`、`libhyper_os_flutter.so`。
- 当前属性为 `debug.dpis.font.a55b5fe1=100`，`debug.dpis.rustbin.a55b5fe1=/data/app/MIUIGallery/lib/arm64/libapp_gallery.so`。
- `dpis_native.cpp` 的 `multiplier_for(...)` 会在 paragraph 命中时刷新并读取 `debug.dpis.font.<hash>` / `debug.dpis.forcefont`。
- 因此，若只是修改字体百分比，理论上不必等待 system_server 再次发布；DPIS app 可以直接用 root `setprop debug.dpis.font.a55b5fe1 <percent>`，然后重启 Gallery 即可让 native proxy 读到新值。

本轮代码修复：

- `AppConfigSaveHandler` 在保存非空且字体模式启用时调用 `HyperOsNativeFontPropertySyncer.publishForceFontTargetAsync(...)`。
- `HyperOsNativeFontPropertySyncer` 新增 root `setprop` 发布路径，只更新 `debug.dpis.font.<hash>`，不清理 `debug.dpis.rustbin.<hash>`。
- 字体模式关闭或字体输入清空时仍走清理路径，把 font/rustbin 属性置为 `0`，避免 stale multiplier 残留。

这条旁路能解决的是“配置值变化后让已部署 native proxy 读到新值”。它不能解决的是“覆盖安装后 system_server 仍运行旧 Java hook 代码”。如果修改了 `HyperOsRustProcessHookInstaller` 本身、RustProcess 参数处理或 native proxy 部署逻辑，仍需要重启 system_server/设备。
## 9. 风险和边界

- Gallery 更新可能替换 `libhyper_os_flutter.so`，导致固定 offset 失效。
- Gallery 更新可能覆盖 native lib 目录，导致 sibling `libdpis_native.so` 消失，需要重新检测/部署。
- `RustProcessImpl.startRustProcess` 参数位置依赖当前 HyperOS 实现，系统更新后可能变化。
- 运行中 Gallery 可能缓存旧 `Configuration`、Resources、DisplayInfo、env 或 native property，需要重启进程。
- `dpis_enabled=false` 是临时屏蔽，不是删除配置；重新启用会恢复原值。
- 100% 字体通常等同无变化，但配置仍可能存在。
- 无 root 时 native 属性清理器的 `su setprop` 兜底可能失败；Java 层 system property 清理是否成功取决于运行进程权限。

## 10. 快速排查命令

Gallery 当前属性：

```powershell
adb -s d7121fb5 shell su -c getprop debug.dpis.font.a55b5fe1
adb -s d7121fb5 shell su -c getprop debug.dpis.rustbin.a55b5fe1
adb -s d7121fb5 shell su -c getprop debug.dpis.forcefont
adb -s d7121fb5 shell su -c getprop debug.dpis.pushstyle
```

Gallery native proxy：

```powershell
adb -s d7121fb5 shell su -c "ls -l /data/app/MIUIGallery/lib/arm64/libdpis_native.so"
```

Gallery 配置残留：

```powershell
adb -s d7121fb5 shell su -c "grep -n 'com.miui.gallery\|font\.hyperos\|target_packages' /data/user/0/io.github.kwensiu.dpis/shared_prefs/dpi_config.xml"
```

恢复默认的手动兜底：

```powershell
adb -s d7121fb5 shell su -c setprop debug.dpis.font.a55b5fe1 0
adb -s d7121fb5 shell su -c setprop debug.dpis.rustbin.a55b5fe1 0
adb -s d7121fb5 shell su -c setprop debug.dpis.forcefont 0
adb -s d7121fb5 shell su -c am force-stop com.miui.gallery
```
