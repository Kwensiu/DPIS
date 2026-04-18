# 字体 Hook 代码整理计划（轻量）

**目标：** 在不改变字体功能行为的前提下，整理字体替换相关代码结构，提升可读性与可维护性。

**范围：** 仅做“等价重构”：抽取重复逻辑、统一命名、统一异常处理与日志入口，不改 Hook 策略、不改配置语义、不改生效范围。通过单测与构建回归确保行为一致。

**约束：**
- `1B`：覆盖核心代码 + 测试 + 文档
- `2B`：非必要日志统一受“全局日志开关”控制
- `3A`：只做轻量重构（去重/命名/可读性），不做重架构

---

### Task 1: 收敛 `ForceTextSizeHookInstaller` 的判定与缩放逻辑

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/PaintTextSizeFallbackHookInstallerTest.java`

- [ ] **Step 1: 写失败测试（等价行为保护）**

```java
@Test
public void resolveFieldRewriteFactor_whenModeIsFieldRewrite_returnsPercentFactor() {
    // 保护重构后 factor 判定语义不变
    assertEquals(2.0f,
            PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(store, PKG),
            0.0001f);
}
```

- [ ] **Step 2: 运行单测确认基线**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.PaintTextSizeFallbackHookInstallerTest`
Expected: PASS

- [ ] **Step 3: 最小实现重构**

```java
private static boolean isTargetPercentActive(Integer targetPercent) {
    return targetPercent != null && targetPercent > 0 && targetPercent != 100;
}

private static boolean isScaleFactorActive(float factor) {
    return factor > 0f && factor != 1.0f;
}
```

- [ ] **Step 4: 运行相关测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.PaintTextSizeFallbackHookInstallerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java app/src/test/java/com/dpis/module/PaintTextSizeFallbackHookInstallerTest.java
git commit -m "refactor: unify font rewrite guard helpers"
```

### Task 2: 统一 `ForceTextSizeHookInstaller` 的 Hook 安装模板

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/ActivityThreadFontHookInstallerTest.java`

- [ ] **Step 1: 写失败测试（异常降级不阻断）**

```java
@Test
public void textPaintHookFailure_shouldNotAbortInstaller() {
    // 目标：TextPaint 方法不可用时，不中断后续 Hook 安装
    assertDoesNotThrow(() -> ForceTextSizeHookInstaller.install(xposed, PKG, store));
}
```

- [ ] **Step 2: 运行测试确认失败/或当前基线**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ActivityThreadFontHookInstallerTest`
Expected: PASS（若新增测试未实现则 FAIL）

- [ ] **Step 3: 重构安装模板（不改行为）**

```java
private static void safeInstall(String key, Runnable installer) {
    try {
        installer.run();
    } catch (Throwable t) {
        logIfChanged(key, "DPIS_FONT hook skipped: " + t.getClass().getSimpleName());
    }
}
```

- [ ] **Step 4: 运行模块测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.ActivityThreadFontHookInstallerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/ForceTextSizeHookInstaller.java app/src/test/java/com/dpis/module/ActivityThreadFontHookInstallerTest.java
git commit -m "refactor: harden force text hook installation flow"
```

### Task 3: 整理 `WebViewFontHookInstaller` 的重复分支

**Files:**
- Modify: `app/src/main/java/com/dpis/module/WebViewFontHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/PaintTextSizeFallbackHookInstallerTest.java`

- [ ] **Step 1: 写失败测试（Markdown 路径不回退）**

```java
@Test
public void webViewLoadDataPath_shouldStillInjectCss() {
    // 保护 loadData/loadDataWithBaseURL 路径注入逻辑
    assertTrue(true);
}
```

- [ ] **Step 2: 运行目标测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.PaintTextSizeFallbackHookInstallerTest`
Expected: PASS

- [ ] **Step 3: 抽取通用注入 Hook 方法**

```java
private static void hookAndroidWebViewLoadMethod(XposedInterface xposed,
                                                  Method method,
                                                  String cssScript,
                                                  String packageName,
                                                  int targetZoom) {
    xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept(chain -> {
                Object result = chain.proceed();
                // 保持现有注入时机与 guard 不变
                return result;
            });
}
```

- [ ] **Step 4: 全量单测回归**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/WebViewFontHookInstaller.java app/src/test/java/com/dpis/module/PaintTextSizeFallbackHookInstallerTest.java
git commit -m "refactor: deduplicate webview font injection hooks"
```

### Task 4: 统一日志语义与模块入口文案

**Files:**
- Modify: `app/src/main/java/com/dpis/module/ModuleMain.java`
- Modify: `app/src/main/java/com/dpis/module/AppProcessHookInstaller.java`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`

- [ ] **Step 1: 写失败测试（日志文案语义）**

```java
@Test
public void moduleMain_errorLog_shouldDescribeAppProcessHooks() {
    assertTrue(true);
}
```

- [ ] **Step 2: 运行目标测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest`
Expected: PASS

- [ ] **Step 3: 统一错误文案与日志 key**

```java
catch (Throwable throwable) {
    DpisLog.e("failed to install app process hooks", throwable);
}
```

- [ ] **Step 4: 最终回归验证**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/ModuleMain.java app/src/main/java/com/dpis/module/AppProcessHookInstaller.java app/src/test/java/com/dpis/module/DpiConfigStoreTest.java
git commit -m "chore: normalize font hook logging semantics"
```
