# DPIS
<img src="https://raw.githubusercontent.com/Xposed-Modules-Repo/io.github.kwensiu.dpis/refs/heads/main/icon.png" height='70px'>

![GitHub Release](https://img.shields.io/github/v/release/Kwensiu/DPIS)
![License](https://img.shields.io/github/license/Kwensiu/DPIS)

[中文说明](../README.md) | English

DPIS is an LSPosed/Xposed-based Android module for per-app display tuning (`smallest width + font size`) without changing global system display settings.

## Core Capabilities

- Configure per-app smallest width (`dp`)
- Configure per-app font scale (`50-300%`)
- Both width and font support `System` and `Compat` modes
- App list search and filtering (`All apps` / `Configured apps`)
- System-layer hook toggle and safe mode
- Per-app custom font compatibility chains, including Flutter / HyperOS native supplement routes

## Requirements

- Android 8.0+ (`minSdk 26`)
- Rooted device
- LSPosed/Xposed installed and enabled

## Quick Start

1. Enable the DPIS module in LSPosed.
2. Select the target app in scope. In regular cases you do not need `system`.
3. Open DPIS and configure:
   - Smallest width (`dp`)
   - Font scale (`50-300%`)
   - Width mode and font mode (`System` / `Compat`)
4. Save, then restart the target app process. Reboot the device if needed.

If you use `System`, also enable `system` scope in LSPosed. `Compat` usually does not need it.

## Modes

| Mode | Characteristics | Best for | Notes |
| --- | --- | --- | --- |
| `System` | Closer to native system behavior, usually more natural | System-like rendering consistency | Depends on system-layer hooks; some apps do not support it |
| `Compat` | Uses in-process compatibility hooks to adjust scaling more directly | Most regular apps | May cause layout drift or scaling glitches |

> Note: older UI labels used "Emulation / Replacement". The UI now shows "System / Compat".

Font `Compat` uses a conservative default chain to avoid duplicate scaling from downstream TextView/Paint fallbacks. For apps that need extra routes, open the target app details and use `Custom chain` in advanced actions. Changes take effect the next time the target app process starts.

## System-Layer Hook and Safe Mode

- `Off`: only uses in-process overrides for the target app. Recommended with `Compat`.
- `On`: enables the full `system_server` path, useful for debugging and comparison.
- `On + Safe mode`: limits hooks to lower-risk entries (`activity-start`), recommended as the default.

If you use `System`, make sure LSPosed scope includes `system`. `Compat` can usually work with only the target app selected.

## Logs and Diagnostics

- `Log output` is recommended off by default to reduce overhead.
- When enabled, high-frequency `system_server` entries are sampled and deduplicated.
- Font debug stats and overlay are diagnostic tools only and are not required for the normal apply path.

## Build and Test

```powershell
./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
./gradlew :app:testAllDebugUnitTests
```

Optional install commands (Windows PowerShell):

```powershell
./gradlew :app:assembleModern101Debug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk" }
./gradlew :app:assembleCompat100Debug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/compat100/debug/app-compat100-debug.apk" }
```

## Project Structure

```text
app/                      Main Android module
  src/main/java/          Shared production code
  src/main/res/           Shared resources and UI
  src/modern101/java/     libxposed API 101 specific code
  src/compat100/java/     Legacy Xposed compatibility code
  src/test/java/          Unit tests
docs/                     Active documentation
docs/archive/             Historical archived documentation
refs/                     Local references (LSPosed / AOSP / libxposed)
```

## Version Notes

| Variant | File name | Environment |
| --- | --- | --- |
| Standard | `DPIS_{version}.apk` | LSPosed (`libxposed API 101+`) |
| Legacy | `DPIS_{version}_legacy.apk` | Classic Xposed / frameworks without `libxposed API 101` support |

Both variants target the same user-facing feature set. The main differences are framework integration, download entry, and update behavior. Prefer the standard build and only use the legacy build when the standard one cannot load.

For the legacy build, always follow the main repo Releases page. The LSPosed / Xposed module repository only syncs the standard APK.

The standard and legacy builds cannot coexist. They share the same package name, so cross-installing them overwrites the other one and may reset existing state or configuration.

## Documentation

- Chinese README: [../README.md](../README.md)
- Active docs: [README.md](README.md)
- Archived docs: [archive/README.md](archive/README.md)

## License

DPIS is released under [GPL-3.0-or-later](../LICENSE).

## References and Thanks

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
