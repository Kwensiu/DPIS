# 只读状态页 实施计划

> **给代理执行者：** 必选子技能：使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 将 `MainActivity` 变成一个原生只读状态页，展示目标包信息、Hook 列表、作用域说明与真机验证提示，并写一份真机验证文档。

**架构：** `MainActivity` 通过新布局 + 字符串资源绑定几组 TextView，程序只能读出配置信息；支持性文档在 `docs/superpowers/specs` 中补充验证提示和 logcat 关键字。

**技术栈：** Android SDK（Java、布局、资源）、logcat 诊断、用于验证指引的 Markdown 文档。

---

### 任务 1： 创建状态页布局

**文件：**
- 新增： `app/src/main/res/layout/activity_status.xml`

- [ ] **步骤 1： 编写布局 XML。**

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

- [ ] **步骤 2：运行 `./gradlew :app:assembleDebug`** ，验证布局可编译进资源包（失败通常表示属性缺失或拼写错误）。

- [ ] **步骤 3： 在设备/模拟器上启动该 Activity。**
  - 命令： `adb shell am start -n com.dpis.module/.MainActivity`
  - 预期： ScrollView 渲染出各个 TextView 区块；没有输入框或交互控件。

- [ ] **步骤 4： 提交布局改动.**

```bash
git add app/src/main/res/layout/activity_status.xml
git commit -m "feat: add read-only status layout"
```

### 任务 2： 为各分区添加专用字符串

**文件：**
- 修改： `app/src/main/res/values/strings.xml`

- [ ] **步骤 1： 插入所需字符串。**

```xml
    <string name="status_header_text">DPIS 只读状态页</string>
    <string name="status_target_label">目标信息</string>
    <string name="status_hooks_label">已安装 Hook</string>
    <string name="status_scope_text">只对 `DpiConfig.TARGET_PACKAGE` 生效；Hook 修改 `Configuration.densityDpi` 和 `DisplayMetrics`，不影响其他包。</string>
    <string name="status_validation_text">真机验证：看 `DPIS` logcat 条目（安装、override densityDpi）并确认目标包以 560dpi 渲染，其他 app 保持默认 DPI。</string>
```

- [ ] **步骤 2：运行 `./gradlew :app:assembleDebug`** ，确保新增字符串资源合并无冲突。

- [ ] **步骤 3：运行 `./gradlew :app:lintDebug`** ，捕获资源告警（预期通过）。

- [ ] **步骤 4： 提交字符串改动.**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "chore: add strings for status page"
```

### 任务 3： 更新 `MainActivity` 以驱动布局

**文件：**
- 修改： `app/src/main/java/com/dpis/module/MainActivity.java`

- [ ] **步骤 1： 用新的绑定代码替换 Activity 内容。**

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

- [ ] **步骤 2： 运行 `./gradlew :app:assembleDebug`。**

- [ ] **步骤 3： 通过 `adb shell am start -n com.dpis.module/.MainActivity` 启动，并确认 TextView 展示了预期字符串、目标包、DPI、Hook 名称和验证提示。**

- [ ] **步骤 4： 启动时观察 logcat（如 `adb logcat -s DPIS`）。目标包加载时应出现 `installed ResourcesManager and ResourcesImpl hooks` 与 `override densityDpi ...`，以确认页面内容与日志一致。**

- [ ] **步骤 5： 提交 Activity 改动。**

```bash
git add app/src/main/java/com/dpis/module/MainActivity.java
git commit -m "feat: show status page info in MainActivity"
```

### 任务 4： 文档化真机验证步骤

**文件：**
- 新增： `docs/superpowers/specs/2026-04-14-real-device-validation.md`

- [ ] **步骤 1： 编写验证文档，说明 logcat 关键字、命令和观察检查项。**

```markdown
# 真机验证指南

1. 启动模块（重启或 `adb push` + `app_process` 加载），然后在 logcat 中筛选 `DPIS` TAG：`adb logcat -s DPIS`. 关键字包括 `installed ResourcesManager and ResourcesImpl hooks` （确认每个目标包成功插桩）和 `override densityDpi in ResourcesManager/ResourcesImpl`（确认 density 被替换）。
2. 启动目标包 `com.android.settings` 的 Activity 并观察 DPI：检查 `Settings` 的画面是否更细腻（等效 560dpi）或者通过 `adb shell dumpsys display` 查看 `densityDpi`，确保 `buildDensity=560` 而不是系统默认值。
3. 启动一个非目标包（如 launcher），确认没有 `DPIS` logcat 输出、density 仍然默认，这说明 Hook 只在 `DpiConfig` 匹配时生效。
```

- [ ] **步骤 2： 复核文档与规格章节一致，确保验证提示保持统一。**

- [ ] **步骤 3： 提交文档.**

```bash
git add docs/superpowers/specs/2026-04-14-real-device-validation.md
git commit -m "docs: add real-device validation guide"
```



