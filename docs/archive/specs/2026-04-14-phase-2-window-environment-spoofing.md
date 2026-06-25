# Android 单应用“最小宽度”伪装第二阶段方案

## 背景

第一阶段已经完成一个可运行的 LSPosed/libxposed 模块：

- 支持 按应用 配置 `virtualWidthDp`
- 已覆盖 `ResourcesManager`、`ResourcesImpl`、`Display`、`WindowMetrics`、`ViewRootImpl` 等若干链路
- 在部分应用上，已经能看到明显的内容放大效果

但当前验证结果也很明确：**并不是所有应用都能达到“像开发者选项里的最小宽度那样整体重排”的效果**。有些应用只出现了局部尺寸变化，例如图标、边框、部分 框架 或 侧边栏 变化，而根布局并没有按目标宽度完整重排。

这说明第一阶段主要实现的是“资源/度量层面的部分伪装”，而用户真实目标是：

> 像系统全局修改“最小宽度”一样，只对指定 应用 生效，并让整个应用按新的窗口环境重新布局。

第二阶段因此不再以“调 DPI”为主线，而是转为：

> **按应用 window environment spoofing**

也就是对指定应用伪装更小的“有效窗口宽度 / 最小宽度环境”。

## 第一阶段结论

### 已经验证有效的部分

第一阶段已经证明下面几件事是成立的：

1. 在 Android 16 上，应用进程内仍存在可 Hook 的 `Configuration` / `Resources` / `DisplayMetrics` 链路。
2. 通过 modern Xposed API + LSPosed，可以稳定安装进目标应用进程，并读取远程配置。
3. 通过 按应用 `virtualWidthDp` 推导目标 `Configuration` 与 `DisplayMetrics`，可以让部分 应用 明显放大内容。
4. 原生配置页、动态作用域、按应用 参数存储链路已经闭环，足够支撑后续底层实验。

### 已经确认的限制

第一阶段同样证明了下面这些限制：

1. 只改 `densityDpi` 不等于“最小宽度”伪装。
2. 即使同时改了 `screenWidthDp`/`DisplayMetrics`，也可能只影响资源换算，而不能决定根布局宽度。
3. 部分应用的整体布局主要受 `WindowMetrics`、`WindowManager`、`Insets`、`ViewRootImpl` 真实测量尺寸控制。
4. 不同技术栈对显示环境的消费入口不同，单一 Hook 点无法覆盖所有 应用。

## 根因判断

当前“局部缩放、整体不重排”的根因可以归纳为一句话：

> **应用真正决定布局的主窗口链路还没有被完整、一致地伪装。**

更具体地说：

1. `Resources`/`Configuration` 链路已经改到，所以依赖 `dp` 和资源桶的控件会变化。
2. 但很多应用决定页面结构时，不只依赖 `Resources`，还依赖：
   - `WindowManager`
   - `WindowMetrics`
   - `Insets`
   - `ViewRootImpl` 进入 测量/布局 的根尺寸
   - 自身缓存的屏幕尺寸
3. 只要这些链路里仍有一条返回真实值，就会出现“控件尺寸变了，但整体布局断点没变”的情况。

因此第二阶段的重点不是再补更多资源 Hook，而是：

> 找到“根布局真正吃到的窗口宽高”并让整条窗口环境链路保持一致。

## 第二阶段目标

### 目标定义

对指定应用伪装一个更小的可用窗口宽度，让目标应用尽可能像系统修改“最小宽度”后那样：

1. 页面整体重新排版，而不是只缩放局部元素
2. 布局断点（单栏/双栏、侧边栏展开与否、列表列数）随目标宽度变化
3. 旋转、重建、分屏后仍尽量维持一致的伪装结果

### 非目标

第二阶段仍不追求：

1. 完全复刻系统级 显示策略
2. 覆盖游戏/引擎/SurfaceView 的所有渲染链路
3. 解决 WebView/Chromium 内核内部自己的页面布局逻辑

## 第二阶段优先排查与 Hook 清单

以下按优先级排序。

### 1. `ViewRootImpl` 实际测量入口

优先级：最高

目标：

- 确认根布局最终使用的宽高参数到底来自哪里
- 确认当前只在 探针 阶段观察到的尺寸，是否已经太晚，或是否没有改到真正参与 measure 的值

建议排查：

1. `performTraversals(...)` 的输入宽高
2. 宽高进入 `measureHierarchy(...)` / `performMeasure(...)` 之前的中间变量
3. `mWidth` / `mHeight` 与 `WindowManager` 返回值的对应关系

验证标准：

- 修改后应能看到根 view 的 measure 输入尺寸发生变化
- 而不是只看到 `Resources` 和 `Display` 变化

### 2. `WindowManager` / `WindowMetrics` 一致性

优先级：最高

目标：

- 让目标应用在查询窗口 边界 时，拿到与 `virtualWidthDp` 一致的结果

建议排查：

1. `WindowManager#getCurrentWindowMetrics()`
2. `WindowManager#getMaximumWindowMetrics()`
3. 当前 应用 实际是直接调用 public API，还是通过 framework 内部包装层取值

验证标准：

- 当前窗口 边界、最大窗口 边界、`DisplayMetrics` 返回值三者能相互对应
- 宽高变化能影响布局断点，而不只是图标尺寸

### 3. Insets / 可用区域链路

优先级：高

目标：

- 让“去掉状态栏、导航栏后的可用内容区域”也与伪装宽度一致

原因：

- 很多 应用 真正用于布局的是“content 边界”，不是裸 显示尺寸

建议排查：

1. `WindowInsets` 参与布局计算的入口
2. `WindowMetrics#getBounds()` 与 Insets 扣减后的结果
3. 是否存在厂商 ROM 特定的窗口包装层

验证标准：

- 伪装前后，页面内容区宽度判断有变化
- 不出现 display 变了、但可用内容区还是原始值的分裂

### 4. Activity/窗口重建时机

优先级：高

目标：

- 确保旋转、重建、切前后台后，伪装仍然保持

建议排查：

1. Activity 重建时哪些 `Configuration`/`WindowMetrics` 入口会重新走
2. 当前 Hook 是否只在冷启动首帧生效
3. 是否有 Activity 覆盖配置 覆盖回真实值

验证标准：

- 冷启动、热启动、旋转后的布局都保持同一目标宽度

### 5. 技术栈专项验证

优先级：中

目标：

- 分类判断“哪些 应用 因为技术栈不同而仍不生效”

建议分组：

1. 传统 View / AppCompat / Material
2. Jetpack Compose
3. WebView-heavy 应用
4. 自绘/游戏/跨平台引擎

验证意义：

- 先区分“链路没打通”还是“技术栈天然不同”
- 避免把所有不生效都归因于同一个 Hook 缺口

## 推荐实施顺序

第二阶段建议按以下顺序推进：

1. **先定位根布局真实宽高来源**
   - 以 `ViewRootImpl` 为中心
   - 找到真正参与测量的输入值
2. **再统一 `WindowManager + WindowMetrics + DisplayMetrics`**
   - 让窗口环境的几个主要公开入口一致
3. **补 Insets / content 边界**
   - 解决“显示尺寸变了但内容区域没变”的分裂
4. **最后做技术栈专项验证**
   - 把 Compose/WebView/自绘应用分组处理

## 验证标准

第二阶段不要再以“icon 有没有变化”为主判断标准，而要改成下面四类验证：

1. **整体布局是否重排**
   - 页面密度是否整体变化
2. **断点是否变化**
   - 单栏/双栏、卡片列数、侧边栏展开状态是否变化
3. **窗口链路是否一致**
   - `Configuration`、`DisplayMetrics`、`WindowMetrics`、根 measure 输入是否互相对应
4. **重建场景是否稳定**
   - 旋转、前后台切换、重启进程后是否仍生效

## 当前建议

进入第二阶段时，优先不要再扩 UI 功能，也不要继续引入更多资源层面的花式 Hook。

最值得先做的是：

1. 把 `ViewRootImpl` 根测量输入追准
2. 找出当前还没覆盖到的 `WindowManager` / 边界 入口
3. 用少量代表性 应用 建立验证矩阵，而不是只看单个目标应用

这会直接决定 DPIS 能不能从“部分应用局部生效”进入“多数常规应用整体生效”的阶段。

