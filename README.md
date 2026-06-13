# DPIS
<img src="https://raw.githubusercontent.com/Xposed-Modules-Repo/io.github.kwensiu.dpis/refs/heads/main/icon.png" height='70px'>

![GitHub Release](https://img.shields.io/github/v/release/Kwensiu/DPIS)
![License](https://img.shields.io/github/license/Kwensiu/DPIS)
[![QQ Group](https://img.shields.io/badge/交流群-1081784676-12B7F5?logo=qq&logoColor=white)](https://qun.qq.com/universal-share/share?ac=1&authKey=IMGMoVIeXPmgs81WgvJEgurLGCeuV%2FWKh8wSxZIXIqtvAA6U%2BJLMtwzG0lrZj7CG&busi_data=eyJncm91cENvZGUiOiIxMDgxNzg0Njc2IiwidG9rZW4iOiI4RkllZjhUVnhGYlVXcEhHZDhyWHdzcDRkRDlGKzlBL0NId1lkMTN2WFJ4SStHa2Y1bmJMM1dlemY5eEFxdmg4IiwidWluIjoiMTA3MDU3NTUyMSJ9&data=8HydkH9NGrhzp1pPhpOFSYT4Qp1aeQZZDN-Y1nk8cp-RBsmsBnRwQ_vN2qXfmhqtTc_LXfOpR6Gv9UCswKRjKg&svctype=4&tempid=h5_group_info)
[![Telegram Group](https://img.shields.io/badge/交流群-DPIS_Chat-26A5E4?logo=telegram&logoColor=white)](https://t.me/dpis_chat)

中文说明 | [English](docs/README.en.md)

DPIS 是一个基于 LSPosed/Xposed 的 Android 模块，用来按应用单独调整界面比例、最小宽度和字体大小。它适合想让某些应用更大、更小，或更接近平板布局的用户，不需要修改全局系统显示设置。

## 主要功能

- 按应用调整界面比例，范围 `30-300%`
- 按应用设置最小宽度，适合固定到某个 `dp` 宽度档位
- 按应用调整字体大小，范围 `50-300%`
- 支持应用搜索、已配置应用筛选和横屏 / 大屏详情面板
- 支持全局预填和快捷模板，方便重复套用常用配置
- 提供 Modern 和 Legacy 两类 APK，覆盖不同 LSPosed/Xposed 环境

## 使用要求

- Android 8.0 或更高版本
- 已 Root
- 已安装并启用 LSPosed/Xposed

## 快速开始

1. 在 LSPosed 中启用 DPIS 模块。
2. 在作用域中勾选需要调整的目标应用。
3. 打开 DPIS，进入应用列表，选择目标应用。
4. 设置界面比例、最小宽度或字体大小。
5. 点击保存，然后重启目标应用让配置生效。

一般用户可以先使用默认策略，只调整界面比例和字体大小。保存表示 DPIS 已经写入配置，但目标应用通常需要重新启动进程后才会读取新配置。

## 配置建议

- 想整体放大或缩小应用界面：优先使用「界面比例」。
- 想让应用进入类似平板或固定宽度布局：使用「最小宽度」。
- 只想改变文字大小：只调整「字体大小」。
- 不确定策略怎么选：保持默认的「自动」。
- 默认方式无效或显示异常时，再尝试「兼容」。
- 如果使用系统层能力，请在 LSPosed 中额外勾选 `system` 作用域。

进入小窗、改变小窗大小或横竖屏切换时，DPIS 会尽量保持应用本身的缩放表现。不同应用对窗口变化的适配程度不同，遇到显示异常时建议先重启目标应用确认。

## 模板与预填

- 「全局预填」会为还没有配置过的应用准备默认草稿，不会修改已有应用配置。
- 「快捷模板」可以保存一套常用配置，并一次性应用到多个应用。
- 批量应用模板可能覆盖目标应用已有配置，请注意确认弹窗中的提示。

## 版本选择

| 版本 | 文件名 | 适用环境 |
| --- | --- | --- |
| Modern / 现代版 | `DPIS_{version}.apk` | LSPosed / libxposed modern API 环境 |
| Legacy / 传统版 | `DPIS_{version}_legacy.apk` | 传统 Xposed 兼容框架 |

优先使用 Modern 版。只有在 Modern 版无法加载，或你的框架不支持 libxposed modern API 时，再使用 Legacy 版。两个版本包名相同，不能同时安装，交叉安装会互相覆盖。

Legacy 版请始终以主仓库 Releases 为准。LSPosed / Xposed 模块仓库通常只同步 Modern APK。

## 文档导航

### 用户文档

- 当前文档入口：[docs/README.md](docs/README.md)
- 英文说明：[docs/README.en.md](docs/README.en.md)

### 开发与排障

- 开发协作约定：[AGENTS.md](AGENTS.md)
- 字体链路说明：[docs/font-routing.md](docs/font-routing.md)
- UI 变更约定：[docs/ui-guidelines.md](docs/ui-guidelines.md)

### 历史归档

- 历史文档归档：[docs/archive/README.md](docs/archive/README.md)

## 许可证

DPIS 以 [GPL-3.0-or-later](LICENSE) 许可发布。

## 引用与致谢

### 翻译

感谢以下人员为项目提供翻译，助力 DPIS 国际化

- 俄语：[@Osean22](https://github.com/Osean22)

### 项目

DPIS 在实现和演进过程中，参考了以下开源项目的思路与实践，感谢这些项目及其贡献者：

- [libxposed/api](https://github.com/libxposed/api)
- [LSPosed](https://github.com/LSPosed/Lsposed)
- [AdClose](https://github.com/zjyzip/AdClose)
- [App Settings（Xposed-Modules-Repo）](https://github.com/Xposed-Modules-Repo/ru.bluecat.android.xposed.mods.appsettings)
- [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)
- [InxLocker](https://github.com/Chimioo/inxlocker)
- [视界调节](https://www.coolapk.com/feed/70930481?s=OGJiYmE1YjEyYmQ1MmZnNjllOTNiNWF6a1610b3)

## 免责声明

DPIS 运行于 Root/LSPosed 环境，存在稳定性与兼容性风险。请先备份重要数据，并自行评估使用风险。
