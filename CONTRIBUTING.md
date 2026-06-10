# 贡献指南 / Contributing

感谢你帮助改进 DPIS。DPIS 是一个 LSPosed/Xposed 模块，所以高质量问题反馈和小而可验证的改动，比大范围猜测式修改更有价值。

Thanks for helping improve DPIS. This project is an LSPosed/Xposed module, so
good reports and small, verified changes matter more than large speculative
patches.

## 反馈问题 / Report a Bug

提交问题前，如果条件允许，请先尝试最新 release 或 debug build。有些运行时问题可能已经在 stable APK 之外修复。

Before opening a bug, try the latest release or debug build when practical. Some
runtime issues may already be fixed outside the stable APK.

请尽量包含这些信息 / Include these details:

- DPIS 版本和 APK 类型 / DPIS version and APK variant: standard `modern101` or
  legacy `compat100`.
- Android 版本、ROM/OEM 系统、LSPosed 或 Xposed 版本、Root 方式 / Android
  version, ROM/OEM skin, LSPosed or Xposed version, and root method.
- 目标应用包名和版本 / Target app package name and version.
- 该应用使用的 DPIS 配置 / The DPIS settings used for that target app:
  viewport, font, route,
  and any custom hook-chain settings.
- 尽量从重新启动目标应用开始描述复现步骤 / Exact reproduction steps from a
  fresh app launch when possible.
- 预期行为和实际行为 / Expected behavior and actual behavior.
- 相关 LSPosed 模块日志 / Relevant LSPosed module logs from
  `/data/adb/lspd/log/modules_*.log` or
  `verbose_*.log`.

普通 `logcat` 可以作为辅助证据，但不能单独证明 DPIS hook 是否运行。判断 hook 安装和 callback 是否触发时，以 LSPosed 模块日志为主。

Plain `logcat` is useful as supporting evidence, but it is not enough to prove
that a DPIS hook did or did not run. LSPosed module logs are the primary source
for hook installation and callback evidence.

不要附带私人 token、完整设备备份、无关应用数据，或包含个人信息的原始日志。

## 功能建议 / Request a Feature

请先描述用户问题，再描述你希望 DPIS 支持的行为。涉及运行时路线或应用兼容性时，请包含目标包名、当前 DPIS 配置、必要的截图或录屏，以及现有路线为什么不够。

Describe the user problem first, then the behavior you want DPIS to support.
For runtime-route or app-compatibility requests, include the target package,
current DPIS settings, screenshots or screen recordings if useful, and why the
existing routes are not enough.

DPIS 会优先考虑可复用的 route、policy 或配置能力；包名特化行为通常是较晚的兜底方案。

## 提交 PR / Submit a Pull Request

请保持改动聚焦且原子化。代码、测试和相关文档应一起更新。

提交前请运行 / Before submitting:

```bash
./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
./gradlew :app:testAllDebugUnitTests
```

For narrow iteration, use a real flavor test task such as:

```bash
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.ModulePackagePlanTest
```

如果修改 UI 结构、资源 id、共享 binder、导航或 layout 归属，请同步检查相关 source/layout smoke tests。常见触点包括
`MainActivitySourceSmokeTest`, `MainActivityLayoutSmokeTest`,
`AppConfigDialogBinderSourceSmokeTest`, and related `*SourceSmokeTest` files.

如果修改 runtime hooks、route scheduling、viewport、font scaling、`system_server`、
`ActivityThread`、`Resources`、`Display`、WebView 或共享 route 代码，请更新相关 living route document:

- `docs/compat100-runtime-resync.md`
- `docs/modern101-runtime-resync.md`
- `docs/font-routing.md`

有价值的失败实验也请记录。将其标记为 inactive、superseded 或 rejected，而不是静默删除证据。

## 本地产物 / Local Artifacts

不要提交本地诊断文件、生成的截图、Frida 文件、Autofish 状态、keystore、token、`.debug-*` 证据目录，或
`docs/private/` 下的文件。

Do not commit local diagnostics, generated screenshots, Frida files, Autofish
state, keystores, tokens, `.debug-*` evidence directories, or files under
`docs/private/`.

所有 Markdown 和代码文件必须使用 UTF-8 without BOM。
