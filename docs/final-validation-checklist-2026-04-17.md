# DPIS 最终验收清单（2026-04-17）

## 1) 构建与安装
- 构建命令（PowerShell）：
```powershell
./gradlew :app:assembleDebug
```
- 构建+安装（PowerShell）：
```powershell
./gradlew :app:assembleDebug; if ($LASTEXITCODE -eq 0) { adb install -r "app/build/outputs/apk/debug/app-debug.apk" }
```
- 全量单测：
```powershell
./gradlew :app:testDebugUnitTest
```

## 2) 已验证结果
- `:app:assembleDebug` 通过（2026-04-17）。
- `:app:testDebugUnitTest` 全量通过（2026-04-17）。

## 3) 设备侧回归步骤（手工）
1. 打开 DPIS 设置页，确认三个开关可切换并可持久化。
2. 冷启动目标应用（例如小黑盒）。
3. 进入帖子页，再退出帖子页。
4. 重复步骤 2-3 三次，观察是否出现明显卡顿、闪屏、闪退。
5. 关闭/开启 `系统层 Hook` 与 `安全模式` 各测一轮，确认行为符合预期。

## 4) 通过判定
- 无新增闪退。
- 无明显闪屏回归。
- DPI 观感保持正确（与既有目标一致）。
- 设置页 UI 调整不影响开关功能行为。

## 5) 当前已知提醒
- Gradle 警告 `android.disallowKotlinSourceSets=false` 为实验项提示，不影响构建结果。
- 在 `cmd` 中请使用 `&&`；在 PowerShell 中可使用 `; if ($LASTEXITCODE -eq 0) { ... }`，避免把 `assembleDebug;` 误解析为任务名。
