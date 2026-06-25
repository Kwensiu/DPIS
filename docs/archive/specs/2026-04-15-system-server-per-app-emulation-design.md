# system_server 按应用显示环境伪装设计

## 背景

截至当前提交，`app-process emulation` 路线已经完成：

1. 按应用配置与作用域闭环
2. 应用 进程内的 `ResourcesManager` / `ResourcesImpl` / `Resources.getConfiguration()` / `Resources.getDisplayMetrics()` 伪装
3. 对 `com.max.xiaoheihe` 的有效突破：整体大小已经明显变化

但同样已经确认：

1. 该路线无法稳定等价系统开发者选项“最小宽度”
2. 混合栈、Flutter、WebView、自有工具类会在不同时间点读取不同入口
3. 后续继续推进会快速演变为目标应用 专项补丁，而不是通用 按应用显示环境方案

因此，新主线切换为：

> `system_server per-app emulation`

旧路线保留为：

> `app-process emulation fallback`

## 路线判断

### 旧路线状态

旧路线不是失败，而是已经完成其职责：

1. 证明 按应用目标宽度伪装是有价值的
2. 帮助定位真正差异点不在单一 `Resources` Hook，而在系统级环境分发链
3. 提供回退方案 / PoC / 对照样本

结论：

- 旧路线冻结，不再作为主线继续扩展
- 除非用于回归、对照或回退方案，不再继续深入目标应用 专项行为修补

### 新路线目标

新路线追求：

1. 在 `system_server` 按包名分发伪装后的显示环境
2. 让目标应用从系统首次获得的环境就是伪装结果
3. 尽量接近系统开发者选项“最小宽度”的行为，但仅对指定包生效

## 总体架构

新路线拆成四层。

### 1. 配置层

类建议：

- `PerAppDisplayConfig`
- `PerAppDisplayConfigSource`

职责：

1. 读取模块已有 按应用 配置
2. 对外暴露按包名查询的只读接口
3. 第一版只保留一个参数：`targetViewportWidthDp`

约束：

- 继续复用现有 `remote preferences`
- 不改模块 应用 数据结构

### 2. 计算层

类建议：

- `PerAppDisplayEnvironment`
- `PerAppDisplayOverrideCalculator`

职责：

1. 输入真实环境：
   - `Configuration`
   - 显示像素尺寸
   - 目标 `targetViewportWidthDp`
2. 输出目标环境：
   - `screenWidthDp`
   - `screenHeightDp`
   - `smallestScreenWidthDp`
   - `densityDpi`
   - 保持真实像素尺寸不变

原则：

1. 不再走“缩像素窗口”主线
2. 保持全屏窗口像素不变
3. 把逻辑宽度和 density 调整到目标值

### 3. system_server 注入层

类建议：

- `SystemServerProcess`
- `SystemServerDisplayEnvironmentInstaller`
- `SystemServerDisplayHooks`

职责：

1. 判断当前是否位于 `system_server`
2. 在 `system_server` 安装第一批 Hook
3. 对目标包名应用计算后的环境

第一批范围只包含：

1. 按应用 `Configuration` 分发
2. 按应用 `relayout / ClientWindowFrames` 结果分发
3. 必要时 `WindowMetrics` / `DisplayInfo` 暴露链路

第一批明确不做：

1. 所有技术栈一次性全覆盖
2. 应用 进程继续补更多专项逻辑
3. 新配置 UI

### 4. 诊断层

类建议：

- `SystemServerDisplayDiagnostics`

职责：

只记录高解释力日志，至少包含：

1. `packageName`
2. 入口名
3. 原始值
4. 目标值
5. 实际下发值

目标：

- 避免再次进入大范围盲试
- 快速确认是哪个 system_server 链路真正命中

## 第一批实现范围

第一批只做“通用框架 + system_server 入口摸排”，不追求一轮修成。

### 计划

1. 先在代码中落地通用配置层与计算层
2. 再增加 `system_server` 入口占位与进程判断
3. 第一批 Hook 点只求回答：
   - `system_server` 内 按应用 显示环境到底从哪几处发给目标包

### 成功标准

第一批成功不以“已经修好小黑盒”为标准，而以下面三点为准：

1. 已在结构上完成主线切换：旧路线冻结，新路线成为主线
2. 已在代码中具备独立的 system_server 配置与计算框架
3. 已明确第一批 system_server Hook 点与验证日志格式

## 迁移策略

### 对旧路线

保留：

1. 现有模块 应用 配置能力
2. 现有 `remote preferences`
3. 现有 app-process Hook 实现，作为回退方案 / 对照

冻结：

1. 不再新增旧路线专项行为修补
2. 不再继续围绕单个目标应用 追加更多 app-process 特例

### 对新路线

第一阶段过渡期允许：

1. 旧路线继续存在
2. 新路线先只加框架和诊断，不立即替换全部行为

等 system_server 路线打通后，再决定：

1. 旧路线是否完全下线
2. 哪些入口继续保留为回退方案

## 当前建议

立即开始以下两项：

1. 在代码中落地通用框架骨架
2. 开始第一批 `system_server` 入口摸排

这样可以把当前项目从“应用 进程内近似模拟”平滑切到“系统级 按应用 显示环境分发”主线。

## 新增对照样本结论

### 样本：InxLocker

`InxLocker` 说明了一个关键实现原则：

1. manifest 作用域 仍然声明 `android`
2. 运行时不手写判断 `processName == android`
3. 直接使用框架的系统入口能力，把普通应用进程与系统进程分开处理

对本项目的启发：

1. 手写 `system_server` 进程名判断只能作为回退方案
2. 正式路线应该优先切到“系统入口 API + system_server 安装器”

### 样本：AppSettingsR

`AppSettingsR` 说明了旧式按应用 DPI 模块要接近“最小宽度”效果时，至少会碰三层：

1. `attachBaseContext + createConfigurationContext(configuration)`
2. `Display.updateDisplayInfoLocked`
3. `android` 包内的 Activity 启动/记录链路

对本项目的启发：

1. 仅修改应用侧 `Resources / Configuration / DisplayMetrics` 不足以逼近系统开发者选项效果
2. 后续 system_server 诊断必须补上：
   - `ActivityStarter` 启动链路
   - `DisplayInfo.logicalDensityDpi` 相关链路

## 下一批只读诊断目标

在保持 `system_server.hooks_enabled=false` 默认关闭的前提下，下一批只读诊断优先关注：

1. `com.android.server.wm.ActivityStarter#execute`
2. `ActivityRecord` 配置下发链路
3. `DisplayInfo.logicalDensityDpi` / `DisplayContent` 相关链路

阶段目标不是立即改行为，而是回答下面两个问题：

1. 目标包在系统启动链路里第一次拿到显示环境是在 `ActivityStarter` 之前还是之后
2. 当前设备上真正主导“像开发者选项最小宽度那样重排”的，是 `Configuration` 下发，还是 `DisplayInfo` 逻辑密度链路

## 2026-04-15 阶段结论补充

### 已完成验证

当前真机与日志已经明确验证：

1. `android` 作用域下的 `system_server` 注入稳定可用
2. `com.max.xiaoheihe` 的包名可在 `system_server` 链路中稳定提取
3. `ActivityRecord#updateReportedConfigurationAndSend` (`config-dispatch`) 可被命中
4. 在该链路上，`Configuration` 确实可以被临时改写到目标值

### 已否定路线

下面这条路线已经完成验证，并明确否定为最终主线：

> 在 `ActivityRecord / Configuration dispatch` 下游分发点直接写回 按应用 `Configuration`

否定理由：

1. 真机出现明显黑白闪屏
2. 部分页面卡顿甚至闪退
3. 系统会持续把配置刷回原值，模块再重复改回目标值，形成重配抖动
4. 即使下游分发点被改写，也没有带来与系统开发者选项“最小宽度”等价的稳定布局变化

结论：

- `config-dispatch` 只保留为只读探针
- 不再继续把它作为主动写回点

### 新主线修正

主线从：

1. `ActivityRecord / Configuration dispatch`

切换为：

1. `DisplayInfo.logicalDensityDpi`
2. `DisplayContent`
3. `DisplayPolicy`
4. Oplus / ColorOS 显示兼容链

原因：

1. 开发者选项“最小宽度”更接近系统级显示逻辑密度变化，而不是单一 `Configuration` 下游分发点改写
2. `SetAppFull` 已证明 `DisplayPolicy` 在当前 ROM 上是稳定的 `android` 作用域入口
3. 继续在 `Configuration dispatch` 下游硬改，只会越来越像目标应用 专项补丁

### 新阶段目标

下一阶段保持只读探针，不再主动改值，目标只回答两个问题：

1. 当前设备上，`DisplayInfo.logicalDensityDpi` 是否在目标包可见链路里出现按应用差异
2. `DisplayPolicy` / `DisplayContent` 是否比 `ActivityRecord` 更接近真正的“最小宽度”上游源头

