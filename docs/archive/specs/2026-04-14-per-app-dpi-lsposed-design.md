# Android 单应用视口伪装模块设计草案

## 目标

构建一个基于 `libxposed/api` 的 Android Hook 模块，让特定应用在进程内看到一个 **更小的 dp 视口**，从而使 UI 内容（控件、图标、文本、布局）整体呈现更大的视觉效果。用户真实目标在于**把内容放大而非单纯调整图标密度或边距**，因此首要从 `Configuration.screenWidthDp` / `screenHeightDp` / `smallestScreenWidthDp` 等 视口 数据切入，在保证内容尺度一致性的前提下，再以 `densityDpi` 作为辅助手段。

## 当前已确认约束

- 主要 API 依赖使用 `libxposed/api`，不再以旧版 XposedBridge 资源 Hook 为核心设计对象。
- 目标宿主环境是 LSPosed 当前维护线所兼容的现代 Xposed 生态。
- 第一版不追求修改系统级显示参数，也不追求统一影响 `SurfaceView`、游戏原生渲染链、WebView 内核独立缩放策略。
- 第一版优先覆盖典型 Android 应用中依赖 `Resources`、`DisplayMetrics`、`dp/sp` 计算的 UI 内容缩放。

## 已知事实

### 1. 现代 Xposed API 的限制

`LSPosed` / `libxposed` 现代 API 文档明确说明，旧式 resource hooks 已移除，因此不能把设计建立在“直接替换资源实现”上。

这意味着模块需要从应用进程内的配置流和度量流入手，修改目标应用看到的 `Configuration.densityDpi`、`DisplayMetrics.densityDpi` 以及相关派生值。

参考：
- `https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API`
- `https://github.com/libxposed/api`

### 2. Android 16 上与 DPI 相关的框架链路仍然存在

当前查到的 AOSP Android 16 资料表明，资源配置和显示度量仍通过以下链路传递：

- `ActivityThread` 在应用绑定和配置更新过程中处理 `Configuration`
- `ResourcesManager` 负责按配置创建和更新资源对象
- `ResourcesImpl` 在更新配置时同步 `Configuration.densityDpi` 到 `DisplayMetrics`
- `Context.createConfigurationContext(Configuration)` 仍然是官方支持的 覆盖配置 入口

这说明 Android 16 上仍然存在“按应用进程改写内容 DPI”的技术路径。

参考：
- `https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/android/app/ActivityThread.java`
- `https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ResourcesManager.java`
- `https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/android/content/res/ResourcesImpl.java`
- `https://developer.android.com/reference/android/content/Context.html#createConfigurationContext(android.content.res.Configuration)`

### 3. Android 16 上 LSPosed 生态并非不可用

当前维护线 `Vector` 仓库的说明显示，兼容范围覆盖 `Android 8.1 through Android 17 Beta`。这至少说明：

- Android 16 不是天然不支持的目标
- 模块开发应优先按当前维护线的装载与 API 约束验证
- 真机验证仍然必要，尤其要确认不同 ROM 下 `Resources` 链路是否有厂商定制偏差

参考：
- `https://github.com/JingMatrix/Vector`

## 第一版设计方向：按应用 更小 dp 视口伪装

### 目标效果

对单个目标应用注入一个“更小”的虚拟 dp 视口，让应用认为可用的 `screenWidthDp` / `screenHeightDp` / `smallestScreenWidthDp` 都减少，实质上扩大了控件、字体、图标和布局的视觉尺寸，以实现“内容变大”的真实感受。`densityDpi` 仍将被同步调整以维护度量一致性，但它是辅助手段，**典型内容放大效果由 视口 数据驱动**。

### 预期覆盖范围

优先覆盖以下场景：
1. 普通 `View` / `AppCompat` / `Material` 界面
2. 依赖 `Resources.getDisplayMetrics()`、`Resources.getConfiguration()` 以及 `screenWidthDp` 等字段决定布局和资源选择逻辑
3. 基于 `dp` / `sp` 的 XML 布局
4. 基于 density 分桶 的位图/资源加载
5. 期望通过缩小 视口 而不是单独调整图标、内边距 或 状态列表

### 明确不保证的场景

第一版不承诺完全覆盖：
1. 直接读取真实 `Display` / `WindowMetrics` 并自行计算布局的应用
2. `SurfaceView` / OpenGL / Vulkan / 游戏引擎自管分辨率链路
3. WebView 内核内部缩放行为
4. 厂商 ROM 对资源管线的大幅改造

## 第一版候选 Hook 思路

### Hook 策略与优先级（视口优先）

首要目标是让目标应用看到的 `screenWidthDp`/`screenHeightDp`/`smallestScreenWidthDp` 发生变化，从而让布局系统重新计算尺寸，真实感知“内容变大”。要做到这一点，必须按入口→落地→Activity 级的顺序依次控制 `Configuration` 与 `DisplayMetrics`。建议的优先 Hook 顺序如下：

1. `ResourcesManager.applyConfigurationToResources(Configuration, CompatibilityInfo)` —— 系统将启动过程中生成的配置送入资源系统的统一入口。在这里拦截并把 视口 相关字段（`screenWidthDp`、`screenHeightDp`、`smallestScreenWidthDp`）改为模块目标值（比如 按应用 虚拟宽度），`densityDpi` 仅作辅助同步，避免影响 `locale`、`fontScale` 等字段。
2. `ResourcesImpl.updateConfiguration(Configuration, DisplayMetrics, CompatibilityInfo)` —— `Resources` 内部的配置落地点，负责把 `Configuration` 同步到 `DisplayMetrics`。在这里再保证 `DisplayMetrics.densityDpi`/`density`/`scaledDensity` 跟随 视口 的变化，防止只是改 `Configuration` 造成度量分裂。
3. `ResourcesManager.updateResourcesForActivity(IBinder, Configuration, int)` —— Activity 级 覆盖配置 的入口，防止某些 Activity 自己在 `overrideConfig` 里把密度或 视口 恢复到原值。必要时在进入 `chain.proceed()` 前对 `overrideConfig` 修正 视口 并保持 `densityDpi` 一致。

这个顺序的核心逻辑是：先管控 `Configuration`（视口优先），再同步 `DisplayMetrics`（density-second），最后锁定 Activity 的 覆盖 上下文。其他 Hook 点（如 `ActivityThread.handleBindApplication(...)`）可在验证冷启动首屏被漏掉时作为补充。

### 为什么 视口优先 更贴近真实需求

单独改 `densityDpi` 只会让像素换算变快，但 `screenWidthDp`/`screenHeightDp` 等 视口 数据仍旧反映真实屏幕尺寸，布局本身不会重新计算，结果是控件尺度不变但文字/图标被压缩，体验欠佳。把 视口 先缩小，应用才会重新计算布局——真正放大页面内容。`densityDpi` 的调整则是为了让 `DisplayMetrics` 的 `density`/`scaledDensity` 与新的 视口 保持一致，避免资源选择或缓存落差。这个顺序也意味着最后一跳的 `ResourcesManager.updateResourcesForActivity` 只是防止还在 Activity 级别的 覆盖配置 把 视口 “抹平”回原值。

### 第一阶段最小配置项建议：按应用 虚拟宽度 DP

当前阶段无需提供复杂的多维度配置，只需暴露一个 按应用 虚拟宽度 dp，就能驱动 视口 伪装：

1. 以 `targetWidthDp` 为输入，在 `ResourcesManager.applyConfigurationToResources(...)` 以及 `ResourcesImpl.updateConfiguration(...)` 中，将 `Configuration.screenWidthDp`、`smallestScreenWidthDp` 以及 `screenHeightDp`（可根据显示比例推算）写入新值。
2. 同步驱动 `DisplayMetrics.densityDpi`/`density`/`scaledDensity` 让每个 dp 单位拥有一致的像素结果，保持系统放大感。
3. 在 `ResourcesManager.updateResourcesForActivity` 里复写 Activity 的 覆盖配置，避免因 Activity 自带 覆盖 而丢失 `targetWidthDp`。

这个最小集让我们在第一阶段验证 视口优先 能力，同时为后续引入更多 按应用 虚拟高度、最大 dp 区段等扩展打基础。

## Android 16 第一版优先 Hook 点

这一节用于把“候选观察点”收敛为第一版实际优先尝试的技术路径。

### 模块生命周期与安装时机

基于 `libxposed/api` 当前生命周期，第一版建议分三层处理：

- `onModuleLoaded()`
  - 只做轻量初始化
  - 记录框架版本、能力位、模块内部配置读取能力是否可用
  - 不在这里安装具体 Hook
- `onPackageLoaded()`
  - 可选
  - 只用于非常早期的类/方法预热、反射缓存、必要时的 反优化 准备
  - 不作为第一版主 Hook 安装点
- `onPackageReady()`
  - 第一版主安装点
  - 原因是此时应用 类加载器 已稳定，适合用反射查找框架类并安装 Hook
  - 第一版所有实际 Hook 以这个阶段为准

结论：
- 第一版主策略是在 `onPackageReady()` 中安装对 Android framework 类的 Hook
- `onModuleLoaded()` 只做模块级准备
- `onPackageLoaded()` 仅在后续验证发现“第一次资源构建已经错过”时才提前介入

### 主 Hook 点 1：`android.app.ResourcesManager.applyConfigurationToResources(...)`

优先级：最高  
建议角色：全局主入口

目标方法：
- `android.app.ResourcesManager.applyConfigurationToResources(Configuration, CompatibilityInfo)`

选择理由：
- `ActivityThread.handleBindApplication(...)` 在应用绑定时会调用它，把 `data.config` 推进资源系统
- 应用运行中的全局配置变化也会走它
- 这是“配置进入 ResourcesManager”的统一入口之一，适合做第一层 `densityDpi` 改写

建议时机：
- 在 `onPackageReady()` 中安装
- 在调用 `chain.proceed()` 之前修改入参 `Configuration`

建议行为：
- 若当前进程属于命中的目标应用，则复制或直接修改传入的 `Configuration`
- 将 `config.densityDpi` 改为模块配置的目标值
- 保留其他配置字段，避免误伤 locale、night mode、fontScale 等

预期收益：
- 覆盖冷启动早期系统配置进入资源系统的路径
- 覆盖多数全局配置更新事件

主要风险：
- 某些 ROM 可能在进入这里之前或之后附加额外 覆盖配置
- 仅改这一层，Activity 级 覆盖配置 仍可能把密度再次覆盖

### 主 Hook 点 2：`android.content.res.ResourcesImpl.updateConfiguration(...)`

优先级：最高  
建议角色：全局一致性修正层

目标方法：
- `android.content.res.ResourcesImpl.updateConfiguration(Configuration, DisplayMetrics, CompatibilityInfo)`

选择理由：
- AOSP Android 16 中，这里明确把 `mConfiguration.densityDpi` 同步到 `mMetrics.densityDpi`
- 同时会据此更新 `mMetrics.density` 与 `mMetrics.scaledDensity`
- 这是“配置值真正落入资源度量”的关键位置

建议时机：
- 在 `onPackageReady()` 中安装
- 优先在 `chain.proceed()` 之前修正 `Configuration`
- 如传入的 `DisplayMetrics` 非空，也同步修正对应的 `densityDpi`
- 在 `chain.proceed()` 之后只做观测或断言，不再二次硬改内部字段作为主逻辑

建议行为：
- 若 `config != null`，将 `config.densityDpi` 设为目标值
- 若 `metrics != null`，同步修正：
  - `metrics.densityDpi`
  - `metrics.density`
  - `metrics.scaledDensity`
- 让方法自身继续完成后续 `CompatibilityInfo` 和 `AssetManager` 链路更新

预期收益：
- 避免“改了 Configuration，没改 metrics”造成的状态分裂
- 即使上游某处漏改，这里仍能作为最关键的落点兜底

主要风险：
- 这是高频方法，日志和额外分配必须克制
- 如果某些对象复用传入参数，需要谨慎避免引入重复修改副作用

### 补充 Hook 点 1：`android.app.ResourcesManager.updateResourcesForActivity(...)`

优先级：高  
建议角色：Activity 级 覆盖配置 补丁层

目标方法：
- `android.app.ResourcesManager.updateResourcesForActivity(IBinder, Configuration, int)`

选择理由：
- AOSP Android 16 中，Activity 覆盖配置 更新时会经过这里
- 如果应用或系统在 Activity 粒度重新设置 覆盖配置，单靠全局配置入口可能不够

建议时机：
- 与主 Hook 一样，在 `onPackageReady()` 中安装
- 在 `chain.proceed()` 之前检查并修正 `overrideConfig`

建议行为：
- 当 `overrideConfig != null` 时，写入目标 `densityDpi`
- 不改 `displayId`
- 不主动创建新的 覆盖配置，除非后续验证发现传 `null` 时会丢失目标效果

预期收益：
- 提升 Activity 重建、旋转、分屏、窗口变化后的稳定性
- 防止 Activity 级 覆盖 把全局密度覆盖回去

### 补充 Hook 点 2：`android.app.ActivityThread.handleBindApplication(...)`

优先级：中  
建议角色：冷启动超早期引导点

目标方法：
- `android.app.ActivityThread.handleBindApplication(AppBindData)`

选择理由：
- Android 16 中应用启动早期，`data.config` 会先在这里进入 `mResourcesManager.applyConfigurationToResources(...)`
- 这里能看到最早的应用进程初始 `Configuration`

建议时机：
- 只在验证发现主 Hook 点仍然错过首帧资源初始化时启用
- 在 `chain.proceed()` 之前修改 `data.config.densityDpi`

建议行为：
- 只修正 `AppBindData.config.densityDpi`
- 不在这里扩展更多资源逻辑

定位说明：
- 这个点不是第一优先，因为它属于更早、更重、更容易受 Android 内部结构变化影响的私有实现点
- 它的价值主要在“冷启动第一屏已经创建完成之前”的窗口

### 观察点 / 可选兜底点：`android.app.ActivityThread.applyConfigurationToResources(...)`

优先级：中低  
建议角色：链路观察与调试辅助

目标方法：
- `android.app.ActivityThread.applyConfigurationToResources(Configuration)`

选择理由：
- 这是 `ActivityThread` 对 `ResourcesManager.applyConfigurationToResources(...)` 的薄封装
- 价值主要在于帮助确认调用链和时机

建议用途：
- 调试阶段打印进入次数、配置变化来源
- 不建议把它作为唯一主 Hook 点

### 暂不作为第一版主路线的点

- `Context.createConfigurationContext(...)`
  - 先保留为第二层补充路线
  - 第一版不优先从这里切入，因为它更偏 上下文 层传播，覆盖面不如资源主链统一
- `Resources.getDisplayMetrics()` / `Resources.getConfiguration()`
  - 不作为主 Hook 点
  - 只适合作为调试输出或极端兼容性兜底

## 第一版推荐 Hook 顺序

第一版 PoC 建议按下面顺序递进，不要一开始就全开：

1. 只 Hook `ResourcesManager.applyConfigurationToResources(...)`
2. 再叠加 `ResourcesImpl.updateConfiguration(...)`
3. 若 Activity 重建后丢失，再补 `ResourcesManager.updateResourcesForActivity(...)`
4. 若冷启动首屏仍错过，再补 `ActivityThread.handleBindApplication(...)`

原因：
- 这样最容易定位“到底是哪一层把密度带进去，哪一层把它覆盖掉”
- 每次只增加一个变量，符合第一版 PoC 的排障原则

## 第一版方法级技术方案

### 反射与隐藏 API 策略

由于目标类多为 框架隐藏/内部 类，第一版不直接在源码层静态引用这些类，而采用反射查找：

- 通过字符串加载类与方法
- 使用 `libxposed/api` 的 `hook(Executable)` 直接安装
- 如需要更稳定的签名匹配与缓存，可以引入 `libxposed/helper`

第一版建议：
- 先用标准反射 + `hook(Executable)` 完成最小可行链路
- 在 PoC 稳定后，再决定是否切到 `libxposed/helper` 的匹配 DSL

### 对 `Configuration` 的修改原则

- 只修改 `densityDpi`
- 不覆盖已有 `fontScale`、locale、uiMode、screenLayout 等字段
- 对同一个配置对象允许幂等重写，不依赖“只改一次”

### 对 `DisplayMetrics` 的修改原则

若当前 Hook 点拿得到 `DisplayMetrics`，第一版同步维护：
- `densityDpi`
- `density`
- `scaledDensity`

不主动修改：
- `widthPixels`
- `heightPixels`
- `xdpi`
- `ydpi`

这是为了严格保持第一版“仅内容缩放”的边界，不提前进入“显示信息伪装”。

## 第一版时机判断

当前推荐的时机判断如下：

- 模块启动：`onModuleLoaded()` 读取配置、判断是否命中当前目标进程
- 包加载完成：`onPackageReady()` 安装 Hook
- 配置更新前：在 Hook 的 `chain.proceed()` 之前写入目标 `densityDpi`
- 配置更新后：只做日志和一致性检查，不把“后改内部字段”作为常规主逻辑

当前判断依据：
- `onPackageReady()` 是现代 API 中对应用 类加载器 最稳定的公开时机
- Android 16 的资源链在 `ResourcesManager` 和 `ResourcesImpl` 上仍保持集中
- 从前置参数入手比事后篡改内部字段更符合系统原生流向

## 当前推荐路线

第一版推荐主路线：
- 以方案 A 为主
- 方案 B 作为补充
- 方案 C 仅作为调试和兼容性兜底手段

推荐理由：
- 第一版目标是“内容缩放”，而不是伪装真实显示设备参数
- 直接围绕 `Configuration` 和 `ResourcesImpl` 处理，最符合 Android 资源系统的因果链
- 如果只修 `DisplayMetrics`，容易出现布局、资源选择、缓存状态不一致的问题

## 第二版设计方向：窗口/显示信息伪装

仅当第一版验证后发现以下问题明显存在时再做：
- 某些应用直接依赖 `Display` / `WindowMetrics` / `WindowManager` 结果进行布局
- 第一版只能改资源缩放，但应用仍按真实窗口尺寸重新布局，导致显示效果不符合预期

第二版可能涉及的范围：
- `Display` 相关接口
- `WindowMetrics` / `WindowManager` 相关接口
- 某些 ROM 上的兼容缩放或显示适配链路

这部分复杂度显著高于第一版，且更容易引入行为副作用，因此不作为首要目标。

## 分阶段推进与三层统一

### 第一阶段结论

- 当前实测只靠改 `Configuration` 及其伴随的 `DisplayMetrics.densityDpi`（即 Configuration/density 的伪装）已经能够命中目标应用并让资源链重新评估 `dp` 视口，但很多应用仍能从其他 API（如 `WindowMetrics`、`Display`）读到真实窗口尺寸，导致整体尺寸感知与真实设备尺寸一致、控件并未“变大”。也就是说，阶段 1 已经命中，但对目标应用的整体尺寸感知仍然不足。

### 第二阶段主线

- 第二阶段仍以 `virtualWidthDp` 单一配置项为中心，保持第一阶段已验证的 `Configuration` + `density` 伪装链，同时新增对 `WindowMetrics` 与 `Display` 系列接口的伪装。新的策略是：在维持 `virtualWidthDp` 驱动 视口 的前提下，Hook `WindowMetrics.getBounds()`/`WindowManager.getMaximumWindowMetrics()` 等入口，和 `Display.getRealMetrics()` 这类既有度量，确保应用/框架层读取到的窗口尺寸、物理度量都与 `virtualWidthDp` 同步。

### 三层统一推导（配置 → 窗口 → 显示）

- Configuration：入口配置，负责把 `screenWidthDp`/`screenHeightDp`/`smallestScreenWidthDp` 伪装为 `virtualWidthDp` 及其比率。所有资源/布局/模板决策首先读这里。
- WindowMetrics：应用层窗口接口，许多现代 UI 直接调用来判定当前窗口尺寸，必须与 Configuration 保持一致，避免 window-aware 布局落回真实尺寸。
- Display：底层度量，很多 API（`Display.getMetrics()` 等）在任何 resource 之外仍会暴露 DPI 与屏幕大小。三层按顺序对应配置、窗口、展示，形成统一的体感尺寸传播链路。

### 成功标准与每层必要性

- Configuration 成功标准：主资源链 `ResourcesManager`/`ResourcesImpl` 看到的 `screenWidthDp` 和 `densityDpi` 已按 `virtualWidthDp` 设定，常规 `View`/`dp` 布局能感知放大效果。必要性：这是内容缩放链的第一层，任何 UI 读不到这层就不会重新测量/布局。
- WindowMetrics 成功标准：应用通过 `WindowMetrics`/`WindowManager` 获取的 `Bounds` 与 Configuration 视觉尺寸匹配，旋转、分屏、窗口变化场景下不会意外读取真实尺寸。必要性：许多现代组件跳过 `Resources` 直接读 `WindowMetrics`，这层要同步否则即便资源链被改写也会还原布局。
- Display 成功标准：`Display.getMetrics()`/`Display.getRealMetrics()` 等接口上报的 `densityDpi`、`widthPixels`/`heightPixels` 理论上与 视口 重合或受控，避免与底层度量不一致的缓存和状态。必要性：度量层收尾，关系到 `DisplayMetrics` 缓存、API 兼容性以及 `WindowInsets` 之类的衍生计算。

## 分阶段推进与三层统一

### 第一阶段结论

+ 当前实测只靠改 `Configuration` 及其伴随的 `DisplayMetrics.densityDpi`（即 Configuration/density 的伪装）已经能够命中目标应用并让资源链重新评估 `dp` 视口，但很多应用仍能从其他 API（如 `WindowMetrics`、`Display`）读到真实窗口尺寸，导致整体尺寸感知与真实设备尺寸一致、控件并未“变大”。也就是说，阶段 1 已经命中，但对目标应用的整体尺寸感知仍然不足。

### 第二阶段主线

+ 第二阶段仍以 `virtualWidthDp` 单一配置项为中心，保持第一阶段已验证的 `Configuration` + `density` 伪装链，同时新增对 `WindowMetrics` 与 `Display` 系列接口的伪装。新的策略是：在维持 `virtualWidthDp` 驱动 视口 的前提下，Hook `WindowMetrics.getBounds()`/`WindowManager.getMaximumWindowMetrics()` 等入口，和 `Display.getRealMetrics()` 这类既有度量，确保应用/框架层读取到的窗口尺寸、物理度量都与 `virtualWidthDp` 同步。

### 三层统一推导（配置 → 窗口 → 显示）

+ Configuration：入口配置，负责把 `screenWidthDp`/`screenHeightDp`/`smallestScreenWidthDp` 伪装为 `virtualWidthDp` 及其比率。所有资源/布局/模板决策首先读这里。
+ WindowMetrics：应用层窗口接口，许多现代 UI 直接调用来判定当前窗口尺寸，必须与 Configuration 保持一致，避免 window-aware 布局落回真实尺寸。
+ Display：底层度量，很多 API（`Display.getMetrics()` 等）在任何 resource 之外仍会暴露 DPI 与屏幕大小。三层按顺序对应配置、窗口、展示，形成统一的体感尺寸传播链路。

### 成功标准与每层必要性

+ Configuration 成功标准：主资源链 `ResourcesManager`/`ResourcesImpl` 看到的 `screenWidthDp` 和 `densityDpi` 已按 `virtualWidthDp` 设定，常规 `View`/`dp` 布局能感知放大效果。必要性：这是内容缩放链的第一层，任何 UI 读不到这层就不会重新测量/布局。
+ WindowMetrics 成功标准：应用通过 `WindowMetrics`/`WindowManager` 获取的 `Bounds` 与 Configuration 视觉尺寸匹配，旋转、分屏、窗口变化场景下不会意外读取真实尺寸。必要性：许多现代组件跳过 `Resources` 直接读 `WindowMetrics`，这层要同步否则即便资源链被改写也会还原布局。
+ Display 成功标准：`Display.getMetrics()`/`Display.getRealMetrics()` 等接口上报的 `densityDpi`、`widthPixels`/`heightPixels` 理论上与 视口 重合或受控，避免与底层度量不一致的缓存和状态。必要性：度量层收尾，关系到 `DisplayMetrics` 缓存、API 兼容性以及 `WindowInsets` 之类的衍生计算。

## 第二阶段实验结论

真机实验已经证明：

1. `Configuration` 伪装生效：
   - `screenWidthDp` / `screenHeightDp` / `smallestScreenWidthDp` 已按 `virtualWidthDp` 改写。
2. `Display` 伪装生效：
   - `Display.getSize()` / `getRealSize()` / `getRealMetrics()` 已返回缩小后的像素宽高与新的 `densityDpi`。
3. `WindowManager.getCurrentWindowMetrics()` / `getMaximumWindowMetrics()` 未命中：
   - 入口探针无日志，说明目标 应用 当前主路径并不依赖这两个 Java 层入口。
4. `ViewRootImpl.performTraversals()` 始终看到真实根尺寸：
   - 根遍历宽高仍保持真实窗口尺寸（例如 `1080x2376`），即便资源链和显示链都已命中。

由此可得：对目标 应用 来说，整体布局主导因素不是单纯 `Configuration`，也不是当前已覆盖的 `WindowManager` 入口，而是更接近 `ViewRootImpl` 根尺寸或其上游测量链路。第二阶段因此不能视为“失败”，而是完成了排除：它证明了三层链路里，真正缺失的是根布局输入。

## 第四阶段主线：ViewRootImpl 根尺寸伪装

### 目标

在继续保留前面三层结果一致性的前提下，尝试把 `ViewRootImpl.performTraversals()` 看到的根宽高对齐到虚拟窗口像素尺寸，让根布局测量真正建立在“更小屏幕”之上。

### 最小实现边界

第四阶段先不继续扩展新的外部 API，而是只在 `ViewRootImpl` 现有探针基础上升级：

1. 读取共享的 `VirtualDisplayOverride.Result`
2. 在 `performTraversals()` 前观察当前 `mWidth` / `mHeight`
3. 尝试将其对齐为虚拟窗口像素尺寸
4. 记录 before/after 日志，判断根尺寸是否真正被消费

### 成功标准

1. `ViewRoot probe(performTraversals)` 的宽高不再始终等于真实窗口尺寸
2. 目标 应用 的整体布局尺寸开始明显向“小屏设备显示更大”的方向变化
3. 同时保持：
   - `Configuration` 仍为虚拟视口
   - `Display` 仍为虚拟像素尺寸
   - 三层日志不互相矛盾

### 风险

1. `ViewRootImpl` 是高频关键路径，探针与改写都必须极其克制
2. 仅改 `mWidth/mHeight` 可能仍不足以覆盖所有 measure spec 来源
3. 若第四阶段最小实现仍无法改变整体布局，则需要继续向 `measure spec` / `Insets` / 更早期窗口尺寸链路追踪

## 需要准备的开发环境

### 基础开发环境

- Android Studio 最新稳定版
- Android SDK
- JDK（与 Android Studio / AGP 兼容版本）
- Gradle
- Kotlin

### 调试与验证环境

- 一台可反复测试的 Root Android 16 设备
- `Magisk` 或 `KernelSU`
- `Zygisk`
- 当前维护线的 LSPosed/Vector 框架
- `adb`
- `logcat`

### 可选环境

仅在需要观察更底层行为时再装：
- Android NDK
- CMake
- 反编译/静态分析工具，如 `jadx`

## 需要准备的资料

### 核心 API / 框架资料

- `libxposed/api` 仓库与示例
- LSPosed 现代 API 开发文档
- AOSP Android 16 的以下类：
  - `android.app.ActivityThread`
  - `android.app.ResourcesManager`
  - `android.content.res.ResourcesImpl`
  - `android.content.res.Configuration`
  - `android.util.DisplayMetrics`

### 官方 Android 文档

- `Context.createConfigurationContext(...)`
- 配置变更与资源更新机制
- DisplayMetrics / density 相关文档

### 参考方向

可以优先找以下类型项目，而不是拘泥于“名字就叫每应用 DPI”：
- 单应用 locale 覆盖
- 单应用资源配置覆盖
- 与 `Resources` / `Configuration` 改写相关的 Xposed 模块
- 与兼容缩放/显示适配相关的 AOSP 或 ROM 补丁

## Android 16 上需要重点确认的问题

这是后续进入实现前必须验证的关键问题：

1. `libxposed/api` 在目标宿主上的推荐入口和示例模块结构是什么
2. Android 16 上哪些类和方法在应用进程内最稳定、最早、最适合注入 DPI 覆盖
3. `ResourcesImpl.updateConfiguration(...)` 及相关链路在 Android 16 上的签名、可见性、调用时机是否足够稳定
4. 应用发生以下事件时，覆盖值是否会丢失：
   - 进程冷启动
   - Activity 重建
   - 横竖屏切换
   - 深色模式切换
   - 系统字体或显示大小变化
5. 修改 `densityDpi` 后，是否还需要同步修正：
   - `DisplayMetrics.density`
   - `DisplayMetrics.scaledDensity`
   - 其他由 density 派生的像素换算值
6. 不同 ROM 是否会绕过标准 `ResourcesManager` 路径

## 当前最小可行验证思路

后续 PoC 应只验证一条最窄链路：

- 选一个简单目标应用
- 在应用进程启动早期注入固定 `densityDpi`
- 打日志确认注入前后：
  - `Resources.getConfiguration().densityDpi`
  - `Resources.getDisplayMetrics().densityDpi`
  - `Resources.getDisplayMetrics().density`
- 打开一个明显依赖 `dp` 的界面，确认布局和字号发生变化
- 旋转屏幕或重建 Activity，确认覆盖未丢失

只要这条链路稳定复现，就说明第一版方向成立。

## 当前结论

- 这个项目的第一版在技术上是可行的。
- Android 16 不是天然阻断条件，重点在于选对应用进程内的资源配置 Hook 点。
- 第一版不应该从“显示信息伪装”入手，而应该先验证 `Configuration` / `Resources` / `DisplayMetrics` 这条内容缩放链。
- `libxposed/api` 应作为主要开发对象，LSPosed/Vector 作为宿主兼容层来验证。

## 参考链接

- `https://github.com/libxposed/api`
- `https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API`
- `https://github.com/JingMatrix/Vector`
- `https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/android/app/ActivityThread.java`
- `https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ResourcesManager.java`
- `https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/android/content/res/ResourcesImpl.java`
- `https://developer.android.com/reference/android/content/Context.html#createConfigurationContext(android.content.res.Configuration)`

