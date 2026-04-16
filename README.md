# DPIS

DPIS 是一个基于 LSPosed/Xposed 的 Android 模块，用于按应用进行虚拟视口（DPI/viewport）覆写，在不修改全局系统显示参数的前提下调整单应用观感。

## 使用方式

1. 在 LSPosed 中勾选本模块与目标应用（建议勾选 `system`）。
2. 打开 DPIS 应用，给目标应用设置虚拟宽度 `dp`。
4. 重启系统/目标应用进程即可生效。

## 构建

```powershell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/debug/app-debug.apk" }
```
