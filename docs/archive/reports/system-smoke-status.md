# System 冒烟状态（归档）

## 当时验证资产

- `legacysmoke`：legacy-only 传统模块。
- `DPIS`：当时待验证的混合模块。

## 当时已验证结论

1. `DPIS` 能被 LSPosed 识别，APK 路径有效，且作用域显示包含 `system`。
2. `legacysmoke` 也能被 LSPosed 识别并标记 `system` 作用域。
3. `InxLocker` 源码显示其 system hook 通过 `loadSystem` + `YLog` 实现，不是手写 legacy `IXposedHookLoadPackage`。

## 补充说明

`yukismoke` 属于历史实验模块，当前已从主线与测试基线中移除。

## 2026-04-16 非主路径对齐 v9

- 构建标记：`2026-04-16-non-primary-alignment-v9`
- 构建：`:singleapk:assembleDebug` 通过
- 安装：`adb -s aeec529f install -r singleapk\build\outputs\apk\debug\singleapk-debug.apk` 通过
- 日志文件：`logs/system-server-v9.txt`

当时阻塞点：

- 该窗口内未抓到 `entry=activity-start` 或 `entry=config-dispatch`。
- 因此无法判断这两个入口的 `actual vs target` 是否对齐。

当时下一步动作：

- 复现目标应用冷启动后重新抓取 DPIS 日志，重点检查：
  - `entry=activity-start` 的 `system_server probe` 中 `actual` 是否与 `target` 对齐；
  - `entry=config-dispatch` 的 `system_server probe` 中 `actual` 是否与 `target` 对齐。
