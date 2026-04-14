# 只读状态页 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `MainActivity` 变成一个原生只读状态页，展示目标包信息、Hook 列表、作用域说明与真机验证提示，并写一份真机验证文档。

**Architecture:** `MainActivity` 通过新 layout + string resources 绑定几组 TextView，程序只能读出配置信息；支持性文档在 `docs/superpowers/specs` 中补充验证提示和 logcat 关键字。

**Tech Stack:** Android SDK (Java, layouts, resources), logcat diagnostics, Markdown docs for verification guidance.

---

### Task 1: Create the status layout

**Files:**
- Create: `app/src/main/res/layout/activity_status.xml`

- [ ] **Step 1: Write the layout XML.**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="24dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <TextView
            android:id="@+id/status_header"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:textAppearanceLarge"
            android:paddingBottom="12dp" />

        <TextView
            android:id="@+id/status_target_info"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:textAppearanceMedium"
            android:paddingTop="8dp"
            android:paddingBottom="12dp" />

        <TextView
            android:id="@+id/status_hooks"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:textAppearanceMedium"
            android:paddingTop="8dp"
            android:paddingBottom="12dp" />

        <TextView
            android:id="@+id/status_scope"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:textAppearanceMedium"
            android:paddingTop="8dp"
            android:paddingBottom="12dp" />

        <TextView
            android:id="@+id/status_validation"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?android:textAppearanceMedium"
            android:paddingTop="8dp"
            android:paddingBottom="8dp" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Run `./gradlew :app:assembleDebug`** to verify the layout compiles into the resource package (failure indicates missing attributes or typos).

- [ ] **Step 3: Launch the activity on a device/emulator.**
  - Command: `adb shell am start -n com.dpis.module/.MainActivity`
  - Expectation: the ScrollView renders each TextView section; there are no inputs or interactive elements.

- [ ] **Step 4: Commit the layout.**

```bash
git add app/src/main/res/layout/activity_status.xml
git commit -m "feat: add read-only status layout"
```

### Task 2: Add dedicated strings for each section

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Insert the required strings.**

```xml
    <string name="status_header_text">DPIS 只读状态页</string>
    <string name="status_target_label">目标信息</string>
    <string name="status_hooks_label">已安装 Hook</string>
    <string name="status_scope_text">只对 `DpiConfig.TARGET_PACKAGE` 生效；Hook 修改 `Configuration.densityDpi` 和 `DisplayMetrics`，不影响其他包。</string>
    <string name="status_validation_text">真机验证：看 `DPIS` logcat 条目（安装、override densityDpi）并确认目标包以 560dpi 渲染，其他 app 保持默认 DPI。</string>
```

- [ ] **Step 2: Run `./gradlew :app:assembleDebug`** to ensure the new string resources merge without conflict.

- [ ] **Step 3: Run `./gradlew :app:lintDebug`** to catch resource warnings (expect PASS).

- [ ] **Step 4: Commit the string changes.**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "chore: add strings for status page"
```

### Task 3: Update `MainActivity` to drive the layout

**Files:**
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`

- [ ] **Step 1: Replace the activity contents with the new binding code.**

```java
package com.dpis.module;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String[] INSTALLED_HOOKS = {
            "ResourcesManager.applyConfigurationToResources",
            "ResourcesImpl.updateConfiguration"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        ((TextView) findViewById(R.id.status_header))
                .setText(getString(R.string.status_header_text));

        ((TextView) findViewById(R.id.status_target_info))
                .setText(getString(R.string.status_target_label) + ":\n"
                        + DpiConfig.TARGET_PACKAGE + "\n"
                        + getString(R.string.module_description) + " (" + DpiConfig.TARGET_DENSITY_DPI + "dpi)");

        ((TextView) findViewById(R.id.status_hooks))
                .setText(getString(R.string.status_hooks_label) + ":\n"
                        + TextUtils.join("\n", INSTALLED_HOOKS));

        ((TextView) findViewById(R.id.status_scope))
                .setText(getString(R.string.status_scope_text));

        ((TextView) findViewById(R.id.status_validation))
                .setText(getString(R.string.status_validation_text));
    }
}
```

- [ ] **Step 2: Run `./gradlew :app:assembleDebug`.**

- [ ] **Step 3: Launch via `adb shell am start -n com.dpis.module/.MainActivity` and confirm the TextViews show the expected strings, target package, DPI, hook names, and validation hint.**

- [ ] **Step 4: Watch logcat while launching (e.g., `adb logcat -s DPIS`). Expect entries `installed ResourcesManager and ResourcesImpl hooks` and `override densityDpi ...` when the target package loads; this confirms the page matches the logs.**

- [ ] **Step 5: Commit the activity change.**

```bash
git add app/src/main/java/com/dpis/module/MainActivity.java
git commit -m "feat: show status page info in MainActivity"
```

### Task 4: Document real-device verification steps

**Files:**
- Create: `docs/superpowers/specs/2026-04-14-real-device-validation.md`

- [ ] **Step 1: Write the validation doc describing logcat keywords, commands, and observational checks.**

```markdown
# 真机验证指南

1. 启动模块（重启或 `adb push` + `app_process` 加载），然后在 logcat 中筛选 `DPIS` TAG：`adb logcat -s DPIS`. 关键字包括 `installed ResourcesManager and ResourcesImpl hooks` （确认每个目标包成功插桩）和 `override densityDpi in ResourcesManager/ResourcesImpl`（确认 density 被替换）。
2. 启动目标包 `com.android.settings` 的 Activity 并观察 DPI：检查 `Settings` 的画面是否更细腻（等效 560dpi）或者通过 `adb shell dumpsys display` 查看 `densityDpi`，确保 `buildDensity=560` 而不是系统默认值。
3. 启动一个非目标包（如 launcher），确认没有 `DPIS` logcat 输出、density 仍然默认，这说明 Hook 只在 `DpiConfig` 匹配时生效。
```

- [ ] **Step 2: Double-check the doc matches the spec section so the validation hints stay consistent.**

- [ ] **Step 3: Commit the doc.**

```bash
git add docs/superpowers/specs/2026-04-14-real-device-validation.md
git commit -m "docs: add real-device validation guide"
```
