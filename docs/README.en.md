# DPIS
<img src="https://raw.githubusercontent.com/Xposed-Modules-Repo/io.github.kwensiu.dpis/refs/heads/main/icon.png" height='70px'>

![GitHub Release](https://img.shields.io/github/v/release/Kwensiu/DPIS)
![License](https://img.shields.io/github/license/Kwensiu/DPIS)

[中文说明](../README.md) | English

DPIS is an LSPosed/Xposed-based Android module for per-app interface scale, smallest width, and font size tuning. It keeps display changes scoped to the target app as much as possible instead of changing global system display settings.

## Core Capabilities

- Configure per-app interface scale (`30-300%`), the default entry for new configs
- Configure per-app smallest width (`dp`) when a fixed width target is needed
- Configure per-app font scale (`50-300%`)
- Interface scale apply strategy: `Auto`, `System`, or `Compat`
- Font mode: `System` or `Compat`
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
   - Interface scale (`30-300%`, recommended default entry)
   - Or switch to smallest width (`dp`)
   - Font scale (`50-300%`)
   - Font mode (`System` / `Compat`)
4. Save, then restart the target app process. Reboot the device if needed.

For most apps, start with the defaults: interface scale, system font mode, and the automatic interface scale strategy. If you explicitly use the system-layer route, also enable `system` scope in LSPosed. Compat routes usually do not need it.

## Interface Scale and Smallest Width

| Setting | Input range | Best for | Notes |
| --- | --- | --- | --- |
| Interface scale | `30-300%` | Scaling the whole app UI up or down | Recommended default. DPIS computes the target width from the current display environment. |
| Smallest width | Positive integer `dp` | Targeting a fixed width class | Useful for legacy configs, tablet layouts, or app-specific known-good values. |

## Apply Strategy

Interface scale can use one of three apply strategies. Keep `Auto` unless you have a reason to force a route.

| Strategy | Characteristics | Best for | Notes |
| --- | --- | --- | --- |
| `Auto` | Prefers the system route and falls back silently | Default choice | Verify the final behavior in the target app |
| `System` | Closer to native system behavior, usually more natural | System-like rendering consistency | Depends on system-layer hooks; some apps do not support it |
| `Compat` | Uses in-process field rewrite to adjust scaling directly | Regular apps or apps where the system route fails | May cause layout drift or scaling glitches |

> Note: older UI labels used "Emulation / Replacement". The UI now shows "System / Compat".

## Fonts and Hook Chains

Font scale accepts `50-300%`. Unconfigured apps use system font mode by default; switch to `Compat` only when in-process rewriting is needed.

Font `Compat` uses the recommended hook-chain set, including Resources, TextView, Paint, WebView, Flutter settings, and HyperOS native Flutter supplement routes. DPIS uses scheduling and provenance guards to reduce repeated scaling.

If a specific app scales incorrectly, open the target app details and use `Hook chain` to disable individual routes. Changes take effect the next time the target app process starts.

## System-Layer Hook and Safe Mode

- `Off`: only uses in-process overrides for the target app. Recommended with `Compat`.
- `On`: enables the full `system_server` path, useful for debugging and comparison.
- `On + Safe mode`: limits hooks to lower-risk entries (`activity-start`), recommended as the default.

System-layer hooks mainly affect the system route and the automatic interface scale strategy. If `system` scope is not selected in LSPosed, the system route may not take effect. Compat routes can usually work with only the target app selected.

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
docs/agents/              Agent collaboration config
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
- UI guidelines: [ui-guidelines.md](ui-guidelines.md)
- Agent collaboration config: [agents/](agents/)
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
