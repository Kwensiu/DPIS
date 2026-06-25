# 单应用字体稳定性借鉴清单 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不增加用户配置心智的前提下，提升“单应用字体替换”的覆盖率与稳定性。

**Architecture:** 保持“每应用仅配置字体倍率”不变，内部采用分层策略：`Resources` 一致性层 + `TextView` 通用层 + 渲染类型适配层。借鉴 `perdpi.apk` 的覆盖思路，但避免其全局粗暴拦截导致的重复放大。

**Tech Stack:** LSPosed/Xposed API, Android `Resources/Configuration/DisplayMetrics`, `TextView/Spanned`。

---

### Task 1: 建立借鉴项决策矩阵（可纳入/需改造/不建议）

**Files:**
- Modify: `docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md`
- Reference: `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- Reference: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Reference: `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`

- [ ] **Step 1: 记录可直接借鉴项（低风险）**

```text
1) Resources 多入口一致性：updateConfiguration/getConfiguration/getDisplayMetrics 一致覆盖
2) 进程早期注入：onPackageReady 后尽早固定目标 fontScale
3) 幂等去抖：同一对象重复进入路径时不重复乘倍率
```

- [ ] **Step 2: 记录需改造借鉴项（中风险）**

```text
1) TextView.setTextSize 参数拦截：保留，但必须以“基准字号”计算
2) Spanned 内 Absolute/Relative span 缩放：保留，但需打 marker 防二次缩放
3) 自定义控件适配：按“渲染类型”而非“页面”扩展
```

- [ ] **Step 3: 记录不建议借鉴项（高风险）**

```text
1) 全局 Paint.setTextSize/TextPaint.setTextSize 直接乘倍率
2) 同时对同一文本路径做多层无去重乘法
3) 对非目标包默认启用重型字体 hook
```

- [ ] **Step 4: 验收标准**

Run: `rg -n "可直接借鉴|需改造|不建议|风险" docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md`
Expected: 至少命中 4 行决策矩阵内容

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md
git commit -m "docs: add font hook borrowing checklist and tradeoff matrix"
```

### Task 2: 借鉴项落地优先级（不破坏心智）

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Modify: `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: 优先做 Resources 一致性层收敛**

```java
// Ensure config.fontScale and metrics.scaledDensity are always coherent.
FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, config.fontScale);
FontScaleOverride.applyToConfiguration(config, fontScale);
FontScaleOverride.applyScaledDensity(metrics, config);
```

- [ ] **Step 2: 通用 TextView 层保持幂等基准**

```java
// Avoid multiplicative drift across repeated setText/setTextSize calls.
float desiredPx = basePx * factor;
if (Math.abs(desiredPx - currentPx) >= SIZE_EPSILON_PX) {
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, desiredPx);
}
```

- [ ] **Step 3: 渲染类型适配层仅针对命中类型扩展**

```java
if (source instanceof Spanned spanned) {
    CharSequence patched = scaleSpansWithMarker(spanned, factor);
    // only patch when marker absent and spans changed
}
```

- [ ] **Step 4: 跑回归测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java \
        app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java \
        app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java \
        app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
git commit -m "fix: harden per-app font override with idempotent layering"
```

### Task 3: 验证策略（覆盖率 vs 稳定性）

**Files:**
- Modify: `docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md`
- Optional: `docs/README.md`

- [ ] **Step 1: 定义三类页面验证清单**

```text
A. 普通 TextView 页面
B. 富文本/表情 span 页面（正文/评论）
C. 自绘文本页面（若存在）
```

- [ ] **Step 2: 定义通过标准**

```text
1) 同倍率下重复进出页面，字号不漂移
2) 重启应用后配置不丢失
3) 无新增闪屏/明显卡顿
```

- [ ] **Step 3: 定义失败回滚标准**

```text
出现双倍放大、严重卡顿、跨应用串扰任一项，即回滚最近字体 hook 变更
```

- [ ] **Step 4: 验证命令**

Run: `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest`
Expected: 两条命令均成功

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-04-17-font-stability-borrowing-checklist.md
git commit -m "docs: add font override verification and rollback criteria"
```

## 借鉴清单（最终决策）

- 可纳入：Resources 多入口一致性、TextView 幂等基线、Spanned marker 防二次缩放。
- 需改造：自定义富文本控件（按渲染类型扩展，不按页面逐个 patch）。
- 不建议：全局 Paint/TextPaint 强拦截（仅在目标包专用调试模式短期开启，默认关闭）。
- 主要取舍：覆盖率提升会增加 hook 复杂度，必须以“幂等 + 包级隔离 + 回滚阈值”兜底。
- 主要问题点：重复放大、风格变化导致基准失真、滚动场景卡顿。
