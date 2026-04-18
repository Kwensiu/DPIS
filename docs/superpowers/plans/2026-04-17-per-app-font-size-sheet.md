# Per-App Font Size Sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在应用配置 Sheet 中支持“按应用单独设置字体大小（font scale）”，并在 Hook 链路中生效，同时不破坏现有 viewport 宽度功能。

**Architecture:** 复用现有 per-app 配置架构（`DpiConfigStore -> PerAppDisplayConfigSource -> ResourcesImplHookInstaller`），新增 `font scale` 字段并贯通 UI、存储和运行时应用。字体设置与 viewport 设置解耦：可单独生效，也可同时生效。空值代表“跟随系统”。

**Tech Stack:** Java (Android), SharedPreferences, Material3 BottomSheet, libxposed hook, JUnit4

---

## File Structure / Responsibility
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`（新增 per-app 字体缩放存取）
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java`（新增 `fontScale` 字段）
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`（返回组合配置）
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`（Sheet 输入、保存/禁用逻辑）
- Modify: `app/src/main/java/com/dpis/module/AppListFilter.java`（仅字体配置也算“已配置”）
- Modify: `app/src/main/java/com/dpis/module/AppStatusFormatter.java`（状态文案包含字体）
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`（应用 per-app fontScale）
- Modify: `app/src/main/res/layout/dialog_app_config.xml`（新增字体输入框）
- Modify: `app/src/main/res/values/strings.xml`（新增字体文案）
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- Test: `app/src/test/java/com/dpis/module/AppListFilterTest.java`
- Test: `app/src/test/java/com/dpis/module/AppStatusFormatterTest.java`
- Test: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

### Task 1: 扩展配置存储与模型

**Files:**
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfig.java`
- Modify: `app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: 先写失败测试（font scale 存取/清除）**
```java
@Test
public void updatesFontScaleForConfiguredPackage() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);

    assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 115));
    assertEquals(Integer.valueOf(115), store.getTargetFontScalePercent("com.max.xiaoheihe"));
}

@Test
public void clearsFontScaleWhenDisabled() {
    FakePrefs prefs = new FakePrefs();
    DpiConfigStore store = new DpiConfigStore(prefs);
    store.setTargetFontScalePercent("com.max.xiaoheihe", 115);

    assertTrue(store.clearTargetFontScalePercent("com.max.xiaoheihe"));
    assertNull(store.getTargetFontScalePercent("com.max.xiaoheihe"));
}
```

- [ ] **Step 2: 运行单测确认失败**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: FAIL（方法不存在或断言失败）

- [ ] **Step 3: 最小实现（store + model + source）**
```java
// DpiConfigStore
Integer getTargetFontScalePercent(String packageName)
boolean setTargetFontScalePercent(String packageName, int percent)
boolean clearTargetFontScalePercent(String packageName)
private static String keyForFontScale(String packageName) {
    return "font." + packageName + ".scale_percent";
}

// PerAppDisplayConfig
final Integer targetFontScalePercent;

// PerAppDisplayConfigSource#get
return new PerAppDisplayConfig(packageName, targetViewportWidthDp, targetFontScalePercent);
```

- [ ] **Step 4: 运行单测确认通过**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: PASS

- [ ] **Step 5: 提交**
```bash
git add app/src/main/java/com/dpis/module/DpiConfigStore.java app/src/main/java/com/dpis/module/PerAppDisplayConfig.java app/src/main/java/com/dpis/module/PerAppDisplayConfigSource.java app/src/test/java/com/dpis/module/DpiConfigStoreTest.java
git commit -m "feat(config): add per-app font scale storage"
```

### Task 2: 列表状态与筛选支持“仅字体配置”

**Files:**
- Modify: `app/src/main/java/com/dpis/module/AppListFilter.java`
- Modify: `app/src/main/java/com/dpis/module/AppStatusFormatter.java`
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`
- Test: `app/src/test/java/com/dpis/module/AppListFilterTest.java`
- Test: `app/src/test/java/com/dpis/module/AppStatusFormatterTest.java`

- [ ] **Step 1: 先写失败测试**
```java
assertTrue(AppListFilter.matches("", Tab.CONFIGURED_APPS, "x", "pkg", false, false, null, 115));
assertEquals("未注入 · 未启用 · 字体115%", AppStatusFormatter.format(false, null, 115));
```

- [ ] **Step 2: 运行测试确认失败**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppStatusFormatterTest`
Expected: FAIL（签名不匹配）

- [ ] **Step 3: 最小实现（新增 font 参数）**
```java
// AppListFilter.matches(..., Integer viewportWidthDp, Integer fontScalePercent)
case CONFIGURED_APPS -> inScope || viewportWidthDp != null || fontScalePercent != null;

// AppStatusFormatter.format(boolean inScope, Integer viewportWidthDp, Integer fontScalePercent)
```

- [ ] **Step 4: 运行测试确认通过**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppStatusFormatterTest`
Expected: PASS

- [ ] **Step 5: 提交**
```bash
git add app/src/main/java/com/dpis/module/AppListFilter.java app/src/main/java/com/dpis/module/AppStatusFormatter.java app/src/main/java/com/dpis/module/MainActivity.java app/src/test/java/com/dpis/module/AppListFilterTest.java app/src/test/java/com/dpis/module/AppStatusFormatterTest.java
git commit -m "feat(ui): treat font-only apps as configured"
```

### Task 3: Sheet 新增字体输入与保存逻辑

**Files:**
- Modify: `app/src/main/res/layout/dialog_app_config.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`

- [ ] **Step 1: 先补输入校验测试（建议新增 MainActivity 提取的纯函数）**
```java
// 纯函数示例：parseFontScalePercent(raw) -> Integer/null
// "" => null, "115" => 115, "49" => invalid, "abc" => invalid
```

- [ ] **Step 2: 运行测试确认失败**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.*Main*`
Expected: FAIL（若尚无测试则先创建最小测试类）

- [ ] **Step 3: 实现 UI 和保存行为**
```xml
<!-- dialog_app_config.xml 新增一个 TextInputLayout + dialog_font_scale_input -->
```
```java
// MainActivity
// 保存时：
// 1) viewport 为空 => 清除 viewport
// 2) font 为空 => 清除 font
// 3) 两者都空 => 等价“禁用参数”
// 4) font 取值范围：50~200（百分比）
```

- [ ] **Step 4: 手工验证保存路径**
Run:
1. 安装 debug 包
2. 打开某应用 Sheet，仅填字体 `115` 保存
3. 重新打开确认回填 `115`
Expected: 不崩溃，配置可持久化

- [ ] **Step 5: 提交**
```bash
git add app/src/main/res/layout/dialog_app_config.xml app/src/main/res/values/strings.xml app/src/main/java/com/dpis/module/MainActivity.java
git commit -m "feat(sheet): add per-app font scale input"
```

### Task 4: Hook 链路应用 per-app fontScale

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java`

- [ ] **Step 1: 先写失败测试（仅字体配置也要改 scaledDensity）**
```java
@Test
public void appliesFontScaleWhenViewportMissing() {
    Configuration config = new Configuration();
    config.densityDpi = 480;
    config.fontScale = 1.0f;
    DisplayMetrics metrics = new DisplayMetrics();
    metrics.densityDpi = 480;
    metrics.density = 3.0f;
    metrics.scaledDensity = 3.0f;

    FakePrefs prefs = new FakePrefs();
    prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit();

    ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, new DpiConfigStore(prefs));

    assertEquals(1.15f, config.fontScale, 0.0001f);
    assertEquals(DensityOverride.scaledDensityFrom(480, 1.15f), metrics.scaledDensity, 0.0001f);
}
```

- [ ] **Step 2: 运行测试确认失败**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
Expected: FAIL

- [ ] **Step 3: 最小实现（先应用字体，再决定是否做 viewport override）**
```java
Integer fontPercent = store != null ? store.getTargetFontScalePercent(packageName) : null;
float effectiveFontScale = fontPercent != null ? (fontPercent / 100f) : config.fontScale;
config.fontScale = effectiveFontScale;

// 当 viewport 无变化但 metrics 存在时，也要更新 scaledDensity
metrics.scaledDensity = DensityOverride.scaledDensityFrom(metrics.densityDpi, effectiveFontScale);
```

- [ ] **Step 4: 运行目标单测**
Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ResourcesImplHookInstallerTest`
Expected: PASS

- [ ] **Step 5: 提交**
```bash
git add app/src/main/java/com/dpis/module/ResourcesImplHookInstaller.java app/src/test/java/com/dpis/module/ResourcesImplHookInstallerTest.java
git commit -m "feat(hook): apply per-app font scale in resources override"
```

### Task 5: 全量回归与发布前验证

**Files:**
- Modify (if needed): `docs/README.md`（补充“字体大小配置”使用说明）

- [ ] **Step 1: 跑全量单测**
Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: 构建调试包并安装验证**
Run:
1. `./gradlew :app:assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: 安装成功

- [ ] **Step 3: 手工场景验证**
Run:
1. 仅设置 viewport（字体空）
2. 仅设置字体（viewport 空）
3. 两者同时设置
4. 点击“禁用参数”
Expected: 4 条路径均符合预期，无明显新增闪屏/卡顿

- [ ] **Step 4: 提交（若文档有更新）**
```bash
git add docs/README.md
git commit -m "docs: add per-app font scale usage"
```

## Self-Review
- **Spec coverage:** 已覆盖 Sheet 输入、配置持久化、Hook 生效、筛选/状态展示、测试与回归验证。
- **Placeholder scan:** 无 TBD/TODO；每个任务给出文件、命令、预期和示例代码。
- **Type consistency:** 统一使用 `targetFontScalePercent`（Integer，可空）命名，避免 float 持久化精度问题。
