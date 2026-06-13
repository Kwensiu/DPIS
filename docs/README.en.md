# DPIS
<img src="https://raw.githubusercontent.com/Xposed-Modules-Repo/io.github.kwensiu.dpis/refs/heads/main/icon.png" height='70px'>

![GitHub Release](https://img.shields.io/github/v/release/Kwensiu/DPIS)
![License](https://img.shields.io/github/license/Kwensiu/DPIS)
[![Telegram Group](https://img.shields.io/badge/Group-DPIS_Chat-26A5E4?logo=telegram&logoColor=white)](https://t.me/dpis_chat)

[中文说明](../README.md) | English

DPIS is an LSPosed/Xposed-based Android module for per-app interface scale, smallest width, and font size tuning. It is useful when you want specific apps to appear larger, smaller, or closer to a tablet-style layout without changing the global system display settings.

## Main Features

- Per-app interface scale, from `30-300%`
- Per-app smallest width, for targeting a fixed `dp` width class
- Per-app font scale, from `50-300%`
- App search, configured-app filtering, and a landscape / large-screen detail panel
- Global Prefill and Quick Templates for reusing common settings
- Modern and Legacy builds for different LSPosed/Xposed environments

## Requirements

- Android 8.0 or later
- Rooted device
- LSPosed/Xposed installed and enabled

## Quick Start

1. Enable the DPIS module in LSPosed.
2. Select the target app in scope.
3. Open DPIS, go to the app list, and choose the target app.
4. Configure interface scale, smallest width, or font size.
5. Save, then restart the target app for the configuration to take effect.

Most users can keep the default strategy and only adjust interface scale or font size. Saving means DPIS has written the configuration, but the target app usually needs a process restart before it reads the new values. 

## Configuration Tips

- To scale the whole app UI up or down, start with `Interface scale`.
- To make an app use a tablet-like or fixed-width layout, use `Smallest width`.
- To change only text size, adjust `Font size`.
- If you are unsure which strategy to choose, keep `Auto`.
- If the default path does not work or the app displays incorrectly, try `Compat`.
- If you use system-layer behavior, also select the `system` scope in LSPosed.

When entering a small window, resizing it, or rotating the screen, DPIS tries to keep the app's scaling stable. Different apps handle window changes differently, so if the display looks wrong, restart the target app first.

## Templates and Prefill

- `Global Prefill` prepares draft values for apps that have not been configured yet. It does not modify existing app configurations.
- `Quick Templates` save a full set of common values and apply them to multiple apps at once.
- Applying a template in batch may overwrite existing target-app configurations. Check the confirmation dialog before applying.

## Version Choice

| Variant | File name | Environment |
| --- | --- | --- |
| Modern | `DPIS_{version}.apk` | LSPosed / libxposed modern API environments |
| Legacy | `DPIS_{version}_legacy.apk` | Classic Xposed-compatible frameworks |

Prefer the Modern build. Use the Legacy build only when the Modern build cannot load or your framework does not support the libxposed modern API. The two variants share the same package name and cannot be installed side by side.

For the Legacy build, always follow the main repository Releases page. The LSPosed / Xposed module repository usually syncs only the Modern APK.

## Documentation

### User Docs

- Active docs: [README.md](README.md)
- Chinese README: [../README.md](../README.md)

### Development and Diagnostics

- Development guide: [AGENTS.md](../AGENTS.md)
- Font routing notes: [font-routing.md](font-routing.md)
- UI guidelines: [ui-guidelines.md](ui-guidelines.md)

### Archive

- Archived docs: [archive/README.md](archive/README.md)

## License

DPIS is released under [GPL-3.0-or-later](../LICENSE).

## References and Thanks

### Translation

Thanks to the following people for providing translations for the project, helping DPIS internationalization

- Russian: [@Osean22](https://github.com/Osean22)

### Project

DPIS references ideas and implementation patterns from the following open-source projects:

- [libxposed/api](https://github.com/libxposed/api)
- [LSPosed](https://github.com/LSPosed/Lsposed)
- [AdClose](https://github.com/zjyzip/AdClose)
- [App Settings（Xposed-Modules-Repo）](https://github.com/Xposed-Modules-Repo/ru.bluecat.android.xposed.mods.appsettings)
- [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)
- [InxLocker](https://github.com/Chimioo/inxlocker)
- [视界调节](https://www.coolapk.com/feed/70930481?s=OGJiYmE1YjEyYmQ1MmZnNjllOTNiNWF6a1610b3)

## Disclaimer

DPIS runs in a Root/LSPosed environment and carries stability and compatibility risks. Back up important data first and evaluate the risks before use.
