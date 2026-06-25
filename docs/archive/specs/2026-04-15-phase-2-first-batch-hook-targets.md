# 第二阶段第一批验证 Hook 点清单

## 目标

用两个对照样本快速定位第二阶段真正的缺口：

- 明显生效：`com.coolapk.market`
- 整体未重排：`com.max.xiaoheihe`

第一批不追求“直接修好小黑盒”，而是回答下面三个问题：

1. 小黑盒的根布局最终吃到的宽高是否仍是真实窗口宽度？
2. 小黑盒主要依赖的是哪条窗口尺寸入口，而不是 `Resources` 链路？
3. 酷安和小黑盒到底从哪一层开始分叉？

## 范围控制

第一批只验证三组入口：

1. `ViewRootImpl` 根测量入口
2. `WindowManager / WindowMetrics` 窗口尺寸入口
3. `Resources / Display / Configuration` 对照入口

不在第一批内处理：

1. WebView 专项验证
2. Compose 专项验证
3. 大范围补 Hook
4. UI 或配置页改动

## 样本与预期

### 样本 1：酷安

- 包名：`com.coolapk.market`
- 现象：整体内容变大，已经接近“最小宽度”伪装效果
- 作用：作为“正常样本”，提供一条已经相对正确的运行链路

### 样本 2：小黑盒

- 包名：`com.max.xiaoheihe`
- 现象：只有局部元素变化，整体布局没有明显重排
- 作用：作为“异常样本”，定位哪条主窗口链路没有跟上

## 第一组：ViewRootImpl 根测量入口

### 目标

确认根布局进入 测量/布局 时使用的真实宽高是什么，以及酷安和小黑盒是否在这里出现分叉。

### 优先类与方法

#### 1. `android.view.ViewRootImpl#performTraversals()`

原因：

- 当前已经有 探针 经验
- 这是根布局遍历与测量的关键入口
- 如果这里看到的小黑盒宽高仍是真实值，而酷安已经接近目标值，就说明分叉已经发生

验证内容：

1. 进入 `performTraversals()` 时记录本轮参与布局的宽高输入
2. 区分：
   - 冷启动首屏
   - 首次绘制后
   - 旋转/重建后

预期日志字段：

- `packageName`
- `processName`
- `width`
- `height`
- `virtualWidthDp`
- 当前推导出的目标像素宽高

判定：

- 如果小黑盒在这里一直拿到真实宽高，而酷安拿到的是目标宽高或近似值，优先继续追窗口尺寸链路
- 如果两者在这里都已经是目标宽高，但小黑盒仍未整体重排，则说明问题更偏向技术栈或后续布局逻辑

#### 2. `ViewRootImpl` 内部进入 measure 前的中间变量

原因：

- `performTraversals()` 只是总入口
- 需要确认当前记录的宽高是不是最终送进测量的那一份

验证重点：

1. 记录 `performTraversals()` 中最终进入 `measureHierarchy/performMeasure` 前使用的宽高
2. 确认当前 覆盖 的时机是否已经太晚

判定：

- 如果 探针 到的值和实际用于 measure 的值不同，第二阶段实现要下沉到更接近最终测量输入的位置

## 第二组：WindowManager / WindowMetrics 入口

### 目标

确认小黑盒是否更依赖窗口 边界，而不是 `Resources`。

### 优先类与方法

#### 1. `android.view.WindowManager#getCurrentWindowMetrics()`

原因：

- 这是现代 Android 应用获取当前窗口大小的主入口之一
- 大量响应式布局、折叠屏/大屏适配逻辑、断点逻辑都会走这里

验证内容：

1. 记录当前窗口 边界
2. 对比：
   - 酷安是否拿到伪装后的 边界
   - 小黑盒是否仍拿到真实 边界

预期日志字段：

- `bounds.width`
- `bounds.height`
- `insets`
- 目标像素宽高

判定：

- 如果酷安与目标一致，小黑盒仍是原始值，则优先补这个入口及其上游返回链
- 如果两者都一致，但小黑盒仍不重排，则继续追 `ViewRootImpl` 或应用自身缓存逻辑

#### 2. `android.view.WindowManager#getMaximumWindowMetrics()`

原因：

- 某些布局会基于最大窗口而非当前窗口做断点判断
- 特别是自适应布局、抽屉、双栏判断

验证内容：

1. 记录返回的最大窗口 边界
2. 比较酷安与小黑盒是否使用不同策略

判定：

- 若小黑盒主要读取最大窗口并且该值未被伪装，解释力很强

#### 3. 当前 `WindowMetrics` Hook 与 `Display` Hook 的一致性

原因：

- 当前已经有 `WindowMetrics` 与 `Display` 伪装
- 第一批需要判断是不是返回结果已分裂

验证内容：

1. `WindowMetrics` 返回宽高
2. `Display#getMetrics/getRealMetrics/getSize/getRealSize`
3. 这几组值是否能共同推导出相同的虚拟宽度

判定：

- 如果三者不一致，先解决一致性，再谈更上层布局问题

## 第三组：Resources / Display / Configuration 对照入口

### 目标

证明“资源链路已经生效，但窗口主链路不一致”，避免继续在资源层盲目加 Hook。

### 优先类与方法

#### 1. `android.content.res.Resources#getConfiguration()`

原因：

- 可以直接验证应用看到的 `screenWidthDp/smallestScreenWidthDp/densityDpi`

验证内容：

1. 酷安与小黑盒是否都已拿到目标 `screenWidthDp`
2. 冷启动与 Activity 重建后是否仍一致

判定：

- 如果两者都拿到目标值，而小黑盒仍不重排，说明资源链路不是主问题

#### 2. `android.content.res.Resources#getDisplayMetrics()`

原因：

- 验证 `densityDpi/density/widthPixels/heightPixels` 是否都已对齐

判定：

- 若酷安、小黑盒在这里都一致，但视觉结果不同，说明差异在窗口或布局消费端

#### 3. 当前 `Display` Hook 返回

原因：

- 这是资源链路与窗口链路之间的桥接对照项

判定：

- 若 `Display` 已变、`Resources` 已变，但 `ViewRootImpl` 仍不变，优先怀疑窗口主链路或测量时机

## 第一批推荐日志策略

为了避免再次出现刷屏日志，第一批日志建议遵守：

1. 只对两个样本包名打印
2. 每个关键入口只在值变化时打印
3. 每条日志固定包含：
   - 包名
   - 方法名
   - 本次宽高
   - 目标宽高
   - 是否与目标一致

推荐标签前缀：

- `Probe/ViewRoot`
- `Probe/WindowMetrics`
- `Probe/Resources`

## 第一批成功判定

第一批不以“修复完成”为目标，而以下列结果为成功：

1. 能明确说出酷安和小黑盒第一处分叉发生在哪一层
2. 能明确判断小黑盒更依赖：
   - `ViewRootImpl` 根测量
   - 或 `WindowManager/WindowMetrics`
   - 或应用自身缓存逻辑
3. 能把第二批实现范围收缩到 1-2 个实际有效的入口，而不是继续大面积试探

## 第一批之后的分支策略

### 分支 A：小黑盒在 `WindowMetrics` 就已经分叉

后续优先：

1. 深入 `WindowManager` 返回链
2. 统一 当前/最大 metrics 与 Insets

### 分支 B：小黑盒在 `ViewRootImpl` 测量输入才分叉

后续优先：

1. 进一步下探 `performTraversals` 内部实际 measure 输入
2. 评估当前 覆盖 时机是否过晚

### 分支 C：小黑盒在窗口与资源都一致，但仍未整体重排

后续优先：

1. 判断其技术栈特征
2. 排查自有尺寸缓存、Compose、自绘或 WebView-heavy 逻辑

## 当前建议

第一批实现时，最值得先做的不是“增加更多 Hook”，而是：

1. 让现有 探针 围绕两个样本更加精准
2. 用对照日志把分叉点找出来
3. 再决定第二批到底是补 `WindowMetrics`，还是补 `ViewRootImpl`

这样推进最快，也最不容易把第二阶段做成无边界试错。

