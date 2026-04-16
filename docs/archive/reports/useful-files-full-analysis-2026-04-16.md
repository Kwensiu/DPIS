# DPIS 有用文件全量分析与整理计划（2026-04-16）

## 1. 范围与判定标准

- 本文按“当前交付主线 + 可复用验证资产 + 必要历史设计文档”定义 `有用文件`。
- 已排除：`**/build/**`、`.gradle/`、`refs/` 下外部镜像代码（仅参考，不参与构建）。
- 判定状态：
- `保留`：继续维护。
- `归档`：保留历史参考，但不参与主线演进。
- `移除`：建议从构建和仓库中移除。
- `合并后移除`：先确认内容已并入主线，再删除。

## 2. 当前行为总览（和卡顿/闪屏直接相关）

- 主线运行模块是 `singleapk`（`settings.gradle.kts` 已 include）。
- DPI 生效链路同时覆盖：`ResourcesManager/ResourcesImpl/Display/WindowMetrics + system_server 环境注入`。
- 当前性能风险点主要来自：
- system_server 高频入口拦截（`SystemServerDisplayEnvironmentInstaller`）
- 多个 Probe Hook（`ResourcesProbe/WindowManagerProbe/WindowSessionProbe/ViewRootProbe`）
- 日志大量输出（`DpisLog` + `SystemServerDisplayDiagnostics`）
- 这些点解释了“功能正确但仍有卡顿/偶发闪屏”的现状。

## 3. 根构建文件（全部 保留）

| 文件 | 作用 | 结论 |
|---|---|---|
| `settings.gradle.kts` | 模块装配入口（当前仅 include `singleapk`） | 保留 |
| `build.gradle.kts` | 根任务（清理） | 保留 |
| `gradle.properties` | 全局 Gradle/AndroidX 开关 | 保留 |
| `gradle/libs.versions.toml` | 依赖与插件版本目录 | 保留 |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper 版本锁定 | 保留 |
| `gradlew` | Unix 构建入口 | 保留 |
| `gradlew.bat` | Windows 构建入口 | 保留 |

## 4. 主线模块 `singleapk`（逐文件）

### 4.1 构建与清单/元数据

| 文件 | 作用 | 结论 |
|---|---|---|
| `app/build.gradle.kts` | 主线 APK 构建、依赖、打包策略 | 保留 |
| `app/proguard-rules.pro` | 混淆与保留规则 | 保留 |
| `app/src/main/AndroidManifest.xml` | 应用与模块组件声明 | 保留 |
| `app/src/main/resources/META-INF/xposed/java_init.list` | Xposed 入口声明 | 保留 |
| `app/src/main/resources/META-INF/xposed/module.prop` | 模块元信息 | 保留 |
| `app/src/main/resources/META-INF/xposed/scope.list` | 默认作用域声明 | 保留（动态作用域策略已由运行态控制） |
| `app/src/main/resources/META-INF/yukihookapi_init` | Yuki 兼容入口标记 | 保留（兼容层） |

### 4.2 UI 资源

| 文件 | 作用 | 结论 |
|---|---|---|
| `app/src/main/res/layout/activity_status.xml` | 主界面（搜索、列表、入口按钮） | 保留 |
| `app/src/main/res/layout/activity_system_server_settings.xml` | system_server 设置页 | 保留 |
| `app/src/main/res/layout/dialog_app_config.xml` | 应用参数弹窗 | 保留 |
| `app/src/main/res/layout/item_app_entry.xml` | 应用列表项 | 保留 |
| `app/src/main/res/values/strings.xml` | 文案与提示 | 保留 |
| `app/src/main/res/values/styles.xml` | 主题样式 | 保留 |
| `app/src/main/res/drawable/badge_circle.xml` | 状态圆点 | 保留 |
| `app/src/main/res/drawable/ic_search_24.xml` | 搜索图标 | 保留 |
| `app/src/main/res/drawable/ic_settings_24.xml` | 设置齿轮图标 | 保留 |

### 4.3 主线 Java（运行时核心）

| 文件 | 作用 | 结论 |
|---|---|---|
| `app/src/main/java/com/dpis/module/ModuleMain.java` | 模块总入口；按包安装 hook；触发 system_server 安装 | 保留 |
| `app/src/main/java/com/dpis/module/MainActivity.java` | 可视化配置与作用域管理 UI | 保留 |
| `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java` | `hooks_enabled` / `safe_mode` 开关页 | 保留 |
| `app/src/main/java/com/dpis/module/DpisApplication.java` | Service 与配置存储生命周期管理 | 保留 |
| `app/src/main/java/com/dpis/module/ConfigStoreFactory.java` | 配置存储创建策略 | 保留 |
| `app/src/main/java/com/dpis/module/DpiConfigStore.java` | 配置读写（目标包/宽度/system_server 开关） | 保留 |
| `app/src/main/java/com/dpis/module/DpiConfig.java` | DPI 配置模型 | 保留 |
| `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java` | 单包配置模型 | 保留 |
| `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java` | 配置读取适配层 | 保留 |
| `app/src/main/java/com/dpis/module/PerAppDisplayEnvironment.java` | 目标显示环境模型 | 保留 |
| `app/src/main/java/com/dpis/module/PerAppDisplayOverrideCalculator.java` | 从目标宽度推导环境参数 | 保留 |
| `app/src/main/java/com/dpis/module/DisplayOverridePipeline.java` | 覆写计算统一入口 | 保留 |
| `app/src/main/java/com/dpis/module/ViewportOverride.java` | Configuration 维度变换 | 保留 |
| `app/src/main/java/com/dpis/module/VirtualDisplayOverride.java` | 像素与 density 推导 | 保留 |
| `app/src/main/java/com/dpis/module/VirtualDisplayState.java` | 虚拟显示状态缓存 | 保留 |
| `app/src/main/java/com/dpis/module/DensityOverride.java` | density/scaledDensity 计算 | 保留 |
| `app/src/main/java/com/dpis/module/WindowFrameOverride.java` | 窗口 frame 变换控制 | 保留 |
| `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java` | ResourcesManager 主链路 hook | 保留 |
| `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java` | ResourcesImpl 更新链路 hook | 保留 |
| `app/src/main/java/com/dpis/module/DisplayHookInstaller.java` | Display metrics/size hook | 保留 |
| `app/src/main/java/com/dpis/module/WindowMetricsHookInstaller.java` | WindowMetrics bounds hook | 保留 |
| `app/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java` | system_server 多入口环境注入（高频关键） | 保留（重点控频优化） |
| `app/src/main/java/com/dpis/module/SystemServerDisplayDiagnostics.java` | system_server 诊断日志构造与回放 | 保留（需总开关治理） |
| `app/src/main/java/com/dpis/module/SystemServerProcess.java` | system_server 进程判定 | 保留 |
| `app/src/main/java/com/dpis/module/DisplayHookInstaller.java` | 客户端 Display 侧覆写 | 保留 |
| `app/src/main/java/com/dpis/module/ResourcesProbeHookInstaller.java` | 资源探针（观测 + 可覆写） | 保留（默认低频/可关） |
| `app/src/main/java/com/dpis/module/WindowManagerProbeHookInstaller.java` | WindowManager 探针 | 保留（默认低频/可关） |
| `app/src/main/java/com/dpis/module/WindowSessionProbeHookInstaller.java` | WindowSession 探针 | 保留（默认低频/可关） |
| `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java` | ViewRoot 探针 + frame 调整观察 | 保留（默认低频/可关） |
| `app/src/main/java/com/dpis/module/CallerTrace.java` | 调用栈裁剪辅助 | 保留 |
| `app/src/main/java/com/dpis/module/DpisLog.java` | 统一日志出口（Log + XposedBridge） | 保留（新增全局日志开关） |
| `app/src/main/java/com/dpis/module/AppListFilter.java` | 列表筛选规则 | 保留 |
| `app/src/main/java/com/dpis/module/AppStatusFormatter.java` | 状态文案格式化 | 保留 |
| `app/src/main/java/com/dpis/module/XSharedPreferencesAdapter.java` | 跨进程配置适配 | 保留 |
| `app/src/main/java/com/dpis/module/WindowMetricsHookInstaller.java` | WindowMetrics 结果改写 | 保留 |
| `app/src/main/java/com/dpis/module/WindowManagerProbeHookInstaller.java` | WindowManager 探针记录 | 保留（观测） |
| `app/src/main/java/com/dpis/module/WindowSessionProbeHookInstaller.java` | WindowSession 探针记录 | 保留（观测） |
| `app/src/main/java/com/dpis/module/LegacyModuleMainHook.java` | legacy 入口兼容桥接 | 保留（过渡期） |
| `app/src/main/java/com/dpis/module/LegacySmokeProbe.java` | legacy 冒烟辅助 | 保留（过渡期） |
| `app/src/main/java/com/dpis/module/LegacySystemServerGate.java` | legacy 侧 system_server 闸门 | 保留（过渡期） |

### 4.4 单测文件（全部 保留）

| 文件 | 作用 | 结论 |
|---|---|---|
| `app/src/test/java/com/dpis/module/FakePrefs.java` | SharedPreferences 测试替身 | 保留 |
| `app/src/test/java/com/dpis/module/AppListFilterTest.java` | 列表筛选测试 | 保留 |
| `app/src/test/java/com/dpis/module/AppStatusFormatterTest.java` | 状态文案测试 | 保留 |
| `app/src/test/java/com/dpis/module/CallerTraceTest.java` | 调用栈裁剪测试 | 保留 |
| `app/src/test/java/com/dpis/module/DensityOverrideTest.java` | density 推导测试 | 保留 |
| `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java` | 配置存储测试 | 保留 |
| `app/src/test/java/com/dpis/module/DisplayOverridePipelineTest.java` | 主计算管线测试 | 保留 |
| `app/src/test/java/com/dpis/module/DisplayHookInstallerTest.java` | Display hook 行为测试 | 保留 |
| `app/src/test/java/com/dpis/module/ViewportOverrideTest.java` | viewport 变换测试 | 保留 |
| `app/src/test/java/com/dpis/module/VirtualDisplayOverrideTest.java` | 虚拟显示推导测试 | 保留 |
| `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java` | ResourcesImpl 覆写测试 | 保留 |
| `app/src/test/java/com/dpis/module/ProbeHookInstallerTest.java` | Probe 安装策略测试 | 保留 |
| `app/src/test/java/com/dpis/module/WindowManagerProbeHookInstallerTest.java` | WindowManager 探针测试 | 保留 |
| `app/src/test/java/com/dpis/module/WindowSessionProbeHookInstallerTest.java` | WindowSession 探针测试 | 保留 |
| `app/src/test/java/com/dpis/module/ViewRootProbeHookInstallerTest.java` | ViewRoot 探针测试 | 保留 |
| `app/src/test/java/com/dpis/module/PerAppDisplayOverrideCalculatorTest.java` | 环境推导测试 | 保留 |
| `app/src/test/java/com/dpis/module/SystemServerDisplayDiagnosticsTest.java` | 诊断日志格式测试 | 保留 |
| `app/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest.java` | 环境选择逻辑测试 | 保留 |
| `app/src/test/java/com/dpis/module/SystemServerDisplayEnvironmentInstallerMutationPolicyTest.java` | 变异策略测试 | 保留 |
| `app/src/test/java/com/dpis/module/SystemServerProcessTest.java` | system_server 判定测试 | 保留 |
| `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java` | legacy 元数据测试 | 保留 |
| `app/src/test/java/com/dpis/module/LegacySmokeModuleFilesTest.java` | legacy 文件完整性测试 | 保留 |
| `app/src/test/java/com/dpis/module/LegacySmokeProbeTest.java` | legacy probe 测试 | 保留 |
| `app/src/test/java/com/dpis/module/LegacySystemServerGateTest.java` | legacy gate 测试 | 保留 |

## 5. 旧模块 `app`（逐文件，建议 合并后移除）

结论：`app` 与 `singleapk` 在包名、实现、测试上高度重叠，容易制造“改了 A 忘了 B”的分叉风险。建议在确认差异已并入 `singleapk` 后整体移除。

### 5.1 `app` 文件清单（逐项）

- `app/build.gradle.kts`: 与 `app/build.gradle.kts` 同类构建脚本。`合并后移除`
- `app/proguard-rules.pro`: 与主线同类。`合并后移除`
- `app/src/main/AndroidManifest.xml`: 模块清单。`合并后移除`
- `app/src/main/resources/META-INF/xposed/java_init.list`: Xposed 元数据。`合并后移除`
- `app/src/main/resources/META-INF/xposed/module.prop`: 元信息。`合并后移除`
- `app/src/main/resources/META-INF/xposed/scope.list`: 作用域声明。`合并后移除`
- `app/src/main/resources/META-INF/yukihookapi_init`: 兼容入口。`合并后移除`
- `app/src/main/res/drawable/badge_circle.xml`: UI 资源。`合并后移除`
- `app/src/main/res/layout/activity_status.xml`: UI 页面。`合并后移除`
- `app/src/main/res/layout/dialog_app_config.xml`: UI 弹窗。`合并后移除`
- `app/src/main/res/layout/item_app_entry.xml`: 列表项。`合并后移除`
- `app/src/main/res/values/arrays.xml`: 旧资源项。`合并后移除`
- `app/src/main/res/values/strings.xml`: 文案。`合并后移除`
- `app/src/main/res/values/styles.xml`: 主题。`合并后移除`
- `app/src/main/java/com/dpis/module/AppListFilter.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/AppStatusFormatter.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/CallerTrace.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ConfigStoreFactory.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DensityOverride.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DisplayHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DpiConfig.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DpiConfigStore.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DpisApplication.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/DpisLog.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/LegacyModuleMainHook.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/LegacySmokeProbe.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/LegacySystemServerGate.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/MainActivity.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ModuleMain.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/PerAppDisplayEnvironment.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/PerAppDisplayOverrideCalculator.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ResourcesProbeHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/SystemServerDisplayDiagnostics.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/SystemServerProcess.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ViewportOverride.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/ViewRootProbeHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/VirtualDisplayOverride.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/VirtualDisplayState.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/WindowFrameOverride.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/WindowManagerProbeHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/WindowMetricsHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/WindowSessionProbeHookInstaller.java`: 逻辑重复。`合并后移除`
- `app/src/main/java/com/dpis/module/XSharedPreferencesAdapter.java`: 逻辑重复。`合并后移除`
- `app/src/test/java/com/dpis/module/AppListFilterTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/AppStatusFormatterTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/CallerTraceTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/DensityOverrideTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/DisplayHookInstallerTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/FakePrefs.java`: 重复测试替身。`合并后移除`
- `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/LegacySmokeModuleFilesTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/LegacySmokeProbeTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/LegacySystemServerGateTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/PerAppDisplayOverrideCalculatorTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/ProbeHookInstallerTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/SystemServerDisplayDiagnosticsTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/SystemServerProcessTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/ViewportOverrideTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/ViewRootProbeHookInstallerTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/VirtualDisplayOverrideTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/WindowManagerProbeHookInstallerTest.java`: 重复测试。`合并后移除`
- `app/src/test/java/com/dpis/module/WindowSessionProbeHookInstallerTest.java`: 重复测试。`合并后移除`

## 6. 冒烟模块 `legacysmoke` / `yukismoke`（逐文件）

结论：这两个模块用于历史试验与回归对照，不应继续与主线并列构建；建议 `归档` 到 `docs/archive/` 描述后移出 `settings.gradle.kts`。

### 6.1 `legacysmoke`

- `legacysmoke/build.gradle.kts`: 旧冒烟构建。`归档`
- `legacysmoke/src/main/AndroidManifest.xml`: 旧清单。`归档`
- `legacysmoke/src/main/assets/xposed_init`: legacy 入口。`归档`
- `legacysmoke/src/main/java/com/dpis/legacysmoke/LegacySmokeHook.java`: 旧 hook 验证。`归档`
- `legacysmoke/src/main/java/com/dpis/legacysmoke/MainActivity.java`: 旧 UI。`归档`
- `legacysmoke/src/main/res/values/arrays.xml`: 旧资源。`归档`
- `legacysmoke/src/main/res/values/strings.xml`: 旧资源。`归档`

### 6.2 `yukismoke`

- `yukismoke/build.gradle.kts`: Yuki 冒烟构建。`归档`
- `yukismoke/src/main/AndroidManifest.xml`: 冒烟清单。`归档`
- `yukismoke/src/main/assets/xposed_init`: legacy init。`归档`
- `yukismoke/src/main/resources/META-INF/yukihookapi_init`: Yuki init。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/HookEntry.kt`: Yuki 入口。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/MainActivity.kt`: 冒烟 UI。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/SystemServerDisplayEnvironmentInstaller.java`: 历史验证实现。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/ConfigStoreFactory.java`: 历史验证实现。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/DpiConfigStore.java`: 历史验证实现。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/DpisLog.java`: 历史验证实现。`归档`
- `yukismoke/src/main/java/com/dpis/yukismoke/XSharedPreferencesAdapter.java`: 历史验证实现。`归档`
- `yukismoke/src/main/res/values/arrays.xml`: 冒烟资源。`归档`
- `yukismoke/src/main/res/values/strings.xml`: 冒烟资源。`归档`

## 7. 文档文件（逐文件）

### 7.1 直接保留（当前有用）

- `docs/system-smoke-status.md`: 设备侧验证状态。`保留`
- `docs/legacy-module-compare.md`: legacy 与主线对照。`保留`
- `docs/dpis-yuki-migration-status.md`: 迁移进度。`保留`
- `docs/superpowers/specs/2026-04-15-system-server-per-app-emulation-design.md`: system_server 设计基线。`保留`
- `docs/superpowers/specs/2026-04-15-phase-2-first-batch-hook-targets.md`: 入口目标定义。`保留`
- `docs/superpowers/plans/2026-04-16-dpis-behavior-stabilization-refactor.md`: 稳定性计划。`保留`
- `docs/superpowers/plans/2026-04-16-system-server-non-primary-path-alignment.md`: 非主路径对齐计划。`保留`

### 7.2 历史参考（归档即可）

- `docs/superpowers/specs/2026-04-14-per-app-dpi-lsposed-design.md`: 早期设计稿。`归档`
- `docs/superpowers/specs/2026-04-14-phase-2-window-environment-spoofing.md`: 早期方案。`归档`
- `docs/superpowers/specs/2026-04-14-read-only-status-design.md`: 早期 UI 方案。`归档`
- `docs/superpowers/specs/2026-04-14-real-device-validation.md`: 历史验证记录。`归档`
- `docs/superpowers/specs/2026-04-14-resourcesimpl-hook-design.md`: 早期 hook 方案。`归档`
- `docs/superpowers/plans/2026-04-14-per-app-dpi-poc.md`: POC 计划。`归档`
- `docs/superpowers/plans/2026-04-14-read-only-status-plan.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-14-remote-preferences-dpi-config.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-14-resourcesimpl-hook-plan.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-14-viewroot-root-size-spoof.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-14-virtual-viewport-spoofing.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-14-windowmetrics-display-spoof.md`: 早期任务单。`归档`
- `docs/superpowers/plans/2026-04-15-legacy-smoke-module.md`: legacy 任务单。`归档`
- `docs/superpowers/plans/2026-04-15-legacy-xposed-metadata.md`: legacy 任务单。`归档`
- `docs/superpowers/plans/2026-04-15-legacysmoke-and-compare.md`: legacy 任务单。`归档`

## 8. 不影响功能的整理执行顺序（完整计划）

1. 先做日志治理（主线不变）：在 `DpiConfigStore` 增加“全局日志开关”，`DpisLog` 统一读取，Probe/system_server 复用。
2. 降低高频路径写放大：`SystemServerDisplayEnvironmentInstaller` 对 `intercept enter/probe/apply` 做采样与去抖。
3. 统一 Probe 入口开关：将 `ResourcesProbe/WindowManagerProbe/WindowSessionProbe/ViewRootProbe` 并到单一布尔配置。
4. 清理构建歧义：`settings.gradle.kts` 已移除 `:legacysmoke`、`:yukismoke`（已完成）。
5. 迁移验证：确认 `app` 与 `singleapk` 无剩余差异后，将 `app` 从构建与仓库移除。
6. 文档归档：`归档` 文档已迁入 `docs/archive/`，并新增索引页（已完成）。
7. 最终收敛：仓库只保留 `singleapk` 主线 + 必要文档 + 最小测试集。

## 9. 回到主线（正确 DPI 调整）的判断标准

- 目标应用在“开启 system_server hooks + safe mode”时，视觉与直接改系统 DPI 一致。
- 进入/退出帖子页不再出现 1s 后补闪。
- 冷启动到首屏可交互耗时稳定，无明显抖动峰值。
- 日志量在默认配置下显著下降（仅保留关键状态日志）。

---

本文已覆盖当前仓库内所有“有用文件”并逐项给出处置建议，后续可直接按第 8 节顺序执行。


