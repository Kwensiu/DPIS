# DPIS

DPIS 是一个基于 LSPosed/Xposed 的 Android 模块，用于按应用进行虚拟视口（DPI/viewport）覆写，在不修改全局系统显示参数的前提下调整单应用观感。

## 使用方式

1. 在 LSPosed 中勾选本模块与目标应用（常规场景不需要勾选 `system`）。
2. 打开 DPIS 应用，为目标应用设置：
   - 虚拟宽度： `dp`
   - 字体大小 `%`（50-300）
   - 选择 `伪装`/`替换` 模式
3. 重启目标应用进程后生效（必要时重启设备）。

### 关于 `伪装` 与 `替换`

1. 适用场景：
   - 伪装：需要 Hook `系统框架(system)`，追求与系统级的 `最小宽度` / `字体大小` 一致的调节方案。
     排版更加精确，但是部分应用不支持。
   - 替换：适用于大部分应用，选用已定义的计算方式强制修改。
     部分场景观感不一致，大量文本可能会导致卡顿。

### 系统层 Hook

- `关闭`：建议选择替换模式，伪装模式效果此时不完整；。
- `开启`：启用完整 system_server 入口（调试/对照使用）。
- `开启 + 安全模式`：只启用低风险 system_server 入口（`activity-start`）（推荐默认）。

### 日志输出

- `日志输出` 默认建议关闭。
- 开启后，system_server 高频入口会按采样窗口与去重策略输出。
- 字体调试统计与悬浮窗仅作为诊断辅助，不影响实际生效链路。

## 构建

```powershell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

### Release 签名（固定证书）

`release` 构建必须使用固定 keystore，避免版本升级时出现签名不一致导致无法覆盖安装。

本地或 CI 需提供以下变量（Gradle 属性或环境变量同名）：

- `DPIS_RELEASE_STORE_FILE`
- `DPIS_RELEASE_STORE_PASSWORD`
- `DPIS_RELEASE_KEY_ALIAS`
- `DPIS_RELEASE_KEY_PASSWORD`

GitHub Actions `release` 工作流还需要：

- `DPIS_RELEASE_KEYSTORE_BASE64`（`.jks` 文件的 Base64）
- `DPIS_RELEASE_STORE_PASSWORD`
- `DPIS_RELEASE_KEY_ALIAS`
- `DPIS_RELEASE_KEY_PASSWORD`
