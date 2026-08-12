# 诊断性能证据架构

本文定义 DPIS 反馈诊断从“运行日志”演进为“模块实际执行证据”的目标
语义。本文先于实现，避免把当前 `FeedbackDiagnosticRuntimeEvents` 或
`FeedbackDiagnosticRuntimeTransport` 的类结构误认为最终领域模型。

## 已确认的产品决策

- 诊断由用户一次启动。
- DPIS 重启目标应用并开始采集。
- 用户复现后返回 DPIS，返回动作同时结束运行证据采集和 Perfetto 采集。
- 不要求用户再次点击“继续采集”或“停止采集”。
- 一般诊断时长不超过 1 分钟；Perfetto 采集预算按约 60 秒设计。
- 如果用户更早返回，立即停止并打包；不为了凑满 60 秒而延迟结束。
- 目标进程负责记录实际 hook 执行证据；DPIS UI 进程只负责控制会话、停止采集和导出。
- 首期覆盖所有 runtime route，而不是只覆盖字体 route：
  `font`、`viewport`、`typeface`、`wechat_dpi` 和 `system_server`。
- 诊断模式允许较宽松的采集粒度，但普通运行路径不能承担诊断级开销。
- 默认不记录文本内容；可以记录类名、数值输入输出、调用者摘要和慢调用
  样本。
- 诊断只能报告观察和时间关联，不能仅凭一次命中直接断言“DPIS 导致卡顿”。

## 核心领域对象

### DiagnosticSession

一次用户可见的反馈诊断会话。它由 DPIS UI 进程创建和关闭，拥有全局
session id、目标包、开始时间、结束原因和采集状态。

它不是实际 hook 执行的所有者；它只定义采集窗口和导出边界。

### ProcessEvidenceSession

某个被 LSPosed 注入的进程在一个 `DiagnosticSession` 窗口内建立的本地
证据采集上下文。它至少包含：

- session id
- package name
- process name
- pid
- module build/runtime marker
- route plan
- hook installation state
- counters
- latency histograms
- slow-call samples
- transport health

一个 DiagnosticSession 可以包含多个 ProcessEvidenceSession，例如抖音主进程
和 `com.google.android.webview` 进程。

### RouteEvidence

某个 runtime route 在目标进程或 `system_server` 中实际执行的结构化证据。
RouteEvidence 不等同于日志文本。

证据阶段固定为：

```text
hook_installed
callback_entered
decision
mutation
```

其中 `hook_installed` 只能证明安装；`callback_entered` 才能证明回调进入；
`decision` 说明允许修改或跳过的原因；`mutation` 才能证明运行对象实际被
改变。

### PerformanceMeasurement

某个 RouteEvidence 的耗时和执行上下文：

- durationNs
- thread name / tid
- 是否主线程
- route
- operation
- decision
- mutation result
- 异常状态

高频调用不逐条持久化 PerformanceMeasurement，而是在目标进程本地聚合。
慢调用和异常可以作为少量样本导出。

### CorrelationResult

将 DPIS 运行证据与 Perfetto 的调度、帧和输入时间线进行关联后的结论。
结论等级为：

- `observed`：观察到目标 route 执行或 mutation。
- `correlated`：DPIS 执行时间与长帧或主线程压力存在时间重叠。
- `likely_contributor`：DPIS 执行耗时在相关帧预算中占有可解释比例。
- `not_supported`：证据不足，不能支持归因。

`likely_contributor` 不是“已证明唯一原因”；其它应用线程、Binder、I/O、
CPU 频率和系统调度因素仍需保留。

## 执行证据链

```text
hook install
  -> callback entered
  -> route decision
  -> skip or mutation
  -> local counter/histogram
  -> periodic aggregate snapshot
  -> cross-process transport
  -> DPIS export
  -> Perfetto correlation
```

每条链路必须能区分：

- 没有安装；
- 已安装但没有回调；
- 有回调但被跳过；
- 允许修改但实际没有改变；
- 实际修改成功；
- 目标进程产生了统计但传输失败；
- 目标进程没有建立证据会话。

空统计不能只显示 `entries: 0`。

## 目标进程采集模型

实际 hook 执行进程拥有 `ProcessEvidenceSession`。高频路径只执行低成本
操作：

- 原子计数；
- `System.nanoTime()`；
- 低成本延迟直方图；
- 跳过原因计数；
- 慢调用阈值判断；
- 少量抽样样本。

高频路径不得执行：

- 每次调用写文件；
- 每次调用构造完整 JSON；
- 每次调用获取完整 Java 堆栈；
- 每次调用进行 Binder/UI 进程通信；
- 每次调用启动 root shell。

目标进程通过周期性快照传输聚合结果。建议快照按 500ms～1s 发送，
慢调用和异常使用单独的小容量样本通道。

## RouteEvidence 的最小字段

```text
sessionId
packageName
processName
pid
route
operation
stage
decision
reason
mutation
inputSummary
outputSummary
durationNs
threadName
tid
isMainThread
timestampElapsedNanos
```

`inputSummary` 和 `outputSummary` 只允许记录诊断所需的数值、类型和状态；
不记录用户文本内容。

## 性能聚合字段

每个 route/operation 至少聚合：

- calls
- applied
- skipped
- skip reasons
- measured calls
- p50 / p95 / p99 / max duration
- main-thread calls
- main-thread accumulated duration
- slow-call count
- exception count
- first and last monotonic timestamp

聚合必须按进程分组，不能把 WebView 或其它子进程混入主进程统计。

## Perfetto 生命周期

Perfetto 由 DiagnosticSession 控制：

1. 诊断会话准备完成后启动。
2. 目标应用重启并进入前台。
3. 用户在目标应用内复现。
4. 用户返回 DPIS。
5. DPIS 立即停止 Perfetto 和 runtime evidence capture。
6. DPIS 读取并打包 trace 与聚合证据。

采集预算按约 60 秒设计，但返回 DPIS 是硬停止边界。超过预算时应采取
明确的安全策略，而不是要求用户再次操作；首期优先保证会话可以自动结束，
并在导出中标记 trace 是否因预算截断。

Perfetto 至少需要覆盖：

- scheduler / thread state
- frame timeline / gfx
- input
- cpu frequency / idle
- binder（用于等待链分析）
- 目标进程线程
- DPIS 自定义 counter 或慢调用 trace slice

DPIS 自定义 trace 不应为每个高频 callback 创建完整 slice。建议所有调用
进入聚合 counter，仅对超过阈值的慢调用创建 slice，并保留 mutation counter。

## 后台与前台语义

当前不假设 DPIS 进入后台后仍能准确完成所有采集工作。需要单独验证：

- DPIS UI 进程进入后台后，root shell/读取流程是否仍可靠；
- 目标进程通过 marker 是否仍能发现会话；
- `/data/local/tmp` 文件追加是否受 SELinux、进程权限或进程冻结影响；
- 返回 DPIS 时是否能稳定读取最后一个聚合快照；
- Perfetto stop 是否必须由 DPIS 前台进程触发；
- MIUI/HyperOS 是否对后台 DPIS 或目标进程执行冻结、延迟或省电限制。

因此导出必须包含独立的 transport health 和 capture completeness 字段，
不能把后台期间没有收到快照解释成“目标 route 没有命中”。

## 导出文件角色

`diagnostic.txt`：

- DiagnosticSession manifest
- ProcessEvidenceSession 列表
- route 安装/回调/decision/mutation 摘要
- 每进程性能聚合
- transport health
- Perfetto 文件引用
- CorrelationResult
- 未覆盖边界和证据不足说明

`dpis-log.txt`：

- DPIS UI 进程应用日志；
- 不作为目标进程实际 hook 命中的唯一来源。

`lsposed-log.txt`：

- 原始 LSPosed 证据；
- 用于交叉验证模块加载、hook 安装和桥接日志。

Perfetto 原始文件：

- 作为独立 ZIP entry；
- 不能只保留解析后的摘要；
- 如果采集失败，仍需导出失败原因和其它运行证据。

## 当前实现与目标模型的差距

当前分支已增加目标进程本地的
`FeedbackDiagnosticProcessPerformance`，并通过 runtime transport 发布按
进程聚合的 calls/applied/skipped 与延迟分位数。导出端优先消费
`source=target-process-transport` 的数据；旧的
`FeedbackDiagnosticPerformanceSnapshot` 仍是迁移期间的 UI 进程 fallback，
不能作为目标 App 进程实际执行结果。

当前 `FeedbackDiagnosticRuntimeTransport` 已经提供跨进程 marker 和文件通道，
并已开始承载目标进程周期性聚合快照。后续仍需补充明确的 transport health、
session completeness、结束时 flush 语义，以及少量慢调用样本的独立传输协议。

## 未决问题

- Perfetto 预算超出约 60 秒时，是截断并继续等待返回，还是提前停止并标记
  `trace_budget_exhausted`。
- 系统是否允许目标进程自定义 trace section 直接写入 Perfetto。
- 慢调用阈值是否按 route 配置，还是先统一使用一个宽松阈值。
- `system_server` 的证据是否与 app-process 使用同一个 session id 和传输文件。
- 后台 DPIS/HyperOS 省电策略对 transport 和 Perfetto stop 的实际影响。
