# DPIS 结构性问题分批跟踪（2026-04-17）

## 目的
- 固定当前“先修第 1 批”的共识，防止后续分析/改动跑偏。
- 每批都给出范围、问题、验收标准、完成状态。

## 全局约束
- 不改变 DPI 功能语义与当前可用行为。
- 先完成第 1 批，再进入第 2 批。
- 每次提交都要附带对应验证结果（构建 + 关键行为回归）。

## 批次状态总览
- 批次 1（入口与职责边界）: `已完成`
- 批次 2（system_server 热路径抖动）: `已完成`
- 批次 3（应用进程 Hook 一致性）: `已完成`
- 批次 4（配置语义统一）: `已完成`
- 批次 5（观测与测试覆盖）: `已完成`

---

## 批次 1：入口与职责边界（先修）

### 范围文件
- `app/src/main/java/com/dpis/module/ModuleMain.java`
- `app/src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java`
- `app/src/main/java/com/dpis/module/DpiConfigStore.java`（仅在语义收敛时触及）

### 已确认结构性问题（锁定）
1. `SystemServerDisplayEnvironmentInstaller` 文件过大（约 1200+ 行），承担安装、包名解析、快照抓取、环境计算、写回应用、日志节流等多职责，维护成本高。
2. `ModuleMain` 同时承担生命周期入口、策略判定、Hook 编排、日志桥接，入口层与策略层耦合。
3. `safe mode / hooks / probe / global log` 判定分散在多个点，语义来源不单一，理解成本高。
4. 开关键值是字符串直连，缺少“策略对象”承载统一解释，容易出现改一处漏一处。

### 第 1 批验收标准
- `ModuleMain` 只保留入口编排，不直接承载复杂策略分支。
- `SystemServerDisplayEnvironmentInstaller` 至少拆出 2-3 个明确职责单元（安装目标解析、环境应用、日志节流/诊断）。
- 开关判定收敛到单一策略读取点，行为与现状一致。
- 通过 `:app:assembleDebug`。
- 设备侧回归：目标应用 DPI 观感保持正确；无新增闪退。

### 非目标（本批不做）
- 不改 Hook 覆盖范围。
- 不改 DPI 算法结果。
- 不做 UI 改版。

### 执行清单
- [x] 拆分 `SystemServerDisplayEnvironmentInstaller` 职责并保持行为不变
- [x] 收敛入口策略判定到单点
- [x] 最小化改动完成构建验证
- [x] 输出回归记录（现象 + 结论）

### 已落地改动（第一轮）
- 新增 `HookRuntimePolicy`，统一开关语义读取（`system_server hooks` / `safe mode` / `global log` / `probe hooks`）。
- `ModuleMain` 改为使用 `HookRuntimePolicy`，入口仅做编排，不再散落组合判定。
- 新增 `SystemServerHookLogGate`，承接 system_server 高频日志去抖与热入口判定。
- `SystemServerDisplayEnvironmentInstaller` 改为委托 `SystemServerHookLogGate`，减少非核心职责堆积。

---

## 备注
- 只有当批次 1 状态改为 `已完成`，才允许进入批次 2 分析与优化。

---

## 批次 2：system_server 热路径抖动（已完成）

### 当前已实施（不改功能）
- 新增 `TargetViewportWidthResolver`，统一应用进程 Hook 的目标 viewport 配置读取与有效性判定。
- 统一旧命名：`seedTargetDensityDpis` -> `seedTargetViewportWidthDps`，并同步日志语义从 `target package` 到 `target app`。
- 移除过时别名 `getEffectiveViewportWidthDp`，统一使用 `getTargetViewportWidthDp`。
- 抽离 entry mutation/安装策略到 SystemServerMutationPolicy，installer 仅做委托。
- 拦截链路复用局部变量，避免重复读取 `chain.getThisObject()/getArgs()`。
- 日志关闭时不再构建高成本诊断字符串，减少热路径无效开销。
- 移除 `applyEnvironment` 后的第 3 次 `captureSnapshot` 反射抓取（仍使用同一对象引用读状态）。
- 抽离热路径命中判断到 `SystemServerHotPathInspector`，减少 installer 内部职责混杂。
- 修复 `SystemServerDisplayDiagnosticsTest` 的 marker 脆弱断言，改为引用 `BUILD_MARKER` 常量。

### 验证
- `:app:assembleDebug` 通过。
- `SystemServerDisplayEnvironmentInstaller*` 相关用例通过。
- `SystemServerDisplayDiagnosticsTest` 与 `SystemServerDisplayEnvironmentInstaller*` 用例通过。

### 收口结论
- 当前最新代码再次验证通过：`:app:assembleDebug` 与 `:app:testDebugUnitTest` 均为绿色。
- 批次 3 涉及的应用进程 Hook 编排与目标 viewport 解析已收敛到单点策略，不再存在旧语义入口并存问题。
- 批次 4 的术语与设置文案已统一（配置/日志/系统层开关语义一致）。
- 批次 5 已补齐最终验收清单文档：`docs/final-validation-checklist-2026-04-17.md`。

---

## 2026-04-18 补充收口

### 批次 3（system_server 回退与日志降噪）补充
- `safe mode` 语义保持不变并明确：仅保留 `activity-start` 安装目标。
- 日志采样窗口分级：`hot=1200ms`、`core=800ms`、`default=400ms`。
- system_server 高频日志 key 降维，减少高基数字段导致的刷屏。

### 批次 4（测试补齐与文档统一）补充
- 新增字体替换“回归参考值”测试：`ForceTextSizeRegressionReferenceTest`。
- 抽取 `FontFieldRewriteMath`，将可测计算与 Hook 绑定逻辑解耦，避免 JVM 单测受 Android 类加载限制。
- 根 README 与最终验收清单已同步当前口径（system_server 三档、日志开关、定向回归命令）。





