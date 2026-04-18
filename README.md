# DPIS

DPIS 是一个基于 LSPosed/Xposed 的 Android 模块，用于按应用进行虚拟视口（DPI/viewport）覆写，在不修改全局系统显示参数的前提下调整单应用观感。

## 使用方式

1. 在 LSPosed 中勾选本模块与目标应用（常规场景不需要勾选 `system`）。
2. 打开 DPIS 应用，为目标应用设置：
   - 虚拟宽度 `dp`
   - 字体模式（三选一）：`系统伪装` / `字段替换` / `关闭`
   - 字体大小 `%`（50-250）
3. 重启目标应用进程后生效（必要时重启设备）。

### system_server 开关口径（当前）

- `系统层 Hook=关闭`：仅目标应用进程内链路生效（推荐默认）。
- `系统层 Hook=开启 + 安全模式=开启`：只启用低风险 system_server 入口（`activity-start`）。
- `系统层 Hook=开启 + 安全模式=关闭`：启用完整 system_server 入口（调试/对照使用）。

### 日志口径（当前）

- `日志输出` 默认建议关闭。
- 开启后，system_server 高频入口会按采样窗口与去重策略输出（已降噪）。
- 字体调试统计与悬浮窗仅作为诊断辅助，不影响实际生效链路。

## 构建

```powershell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/debug/app-debug.apk" }
```
