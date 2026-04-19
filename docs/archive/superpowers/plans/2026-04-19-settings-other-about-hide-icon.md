# 设置页「其他 / 关于 / 隐藏桌面图标」Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设置页新增“其他”分组，落地“关于”页面与“隐藏桌面图标”开关，并确保 LSPosed 模块设置入口不受影响。

**Architecture:** 采用 `SystemServerSettingsActivity` 扩展 + 独立 `AboutActivity`。通过 Manifest `activity-alias` 承载 `LAUNCHER`，开关仅控制 alias 组件启停；`MainActivity` 保留 `MODULE_SETTINGS` 入口。状态持久化放入 `DpiConfigStore` 并纳入迁移逻辑。

**Tech Stack:** Java 17、Android XML、Material3、JUnit4（现有测试体系）。

---

## 实施前文件映射（先读）

- `app/src/main/AndroidManifest.xml`
  - 责任：入口声明、`activity-alias`、AboutActivity 注册。
- `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java`
  - 责任：设置页 UI 绑定、关于页跳转、隐藏图标开关行为。
- `app/src/main/res/layout/activity_system_server_settings.xml`
  - 责任：新增“其他”分组和两条设置项。
- `app/src/main/java/com/dpis/module/AboutActivity.java`（新建）
  - 责任：关于页内容展示与外链打开。
- `app/src/main/res/layout/activity_about.xml`（新建）
  - 责任：关于页布局。
- `app/src/main/java/com/dpis/module/DpiConfigStore.java`
  - 责任：隐藏图标开关持久化读写。
- `app/src/main/java/com/dpis/module/DpisApplication.java`
  - 责任：迁移时同步隐藏图标配置到 remote store。
- `app/src/main/res/values/strings.xml`
  - 责任：新增“其他/关于/隐藏图标/外链 URL”等文案配置。
- `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
  - 责任：新增隐藏图标配置读写测试。
- `app/src/test/java/com/dpis/module/DpisApplicationMigrationTest.java`
  - 责任：新增隐藏图标迁移测试。
- `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`
  - 责任：确认 manifest 仍保留 `MODULE_SETTINGS` 元数据约束（必要时增补 alias 断言）。

## 关于页配置项（MVP 固化）

- `about_source_url`：`https://github.com/Kwensiu/DPIS`
- `about_releases_url`：`https://github.com/Kwensiu/DPIS/releases`
- `about_issues_url`：`https://github.com/Kwensiu/DPIS/issues`
- `about_version_format`：`版本：%1$s (%2$d)`
- `about_link_source_title` / `about_link_update_title` / `about_link_feedback_title`
- `about_link_source_desc` / `about_link_update_desc` / `about_link_feedback_desc`
- `settings_section_other`、`settings_about_label`、`settings_about_hint`
- `settings_hide_launcher_icon_label`、`settings_hide_launcher_icon_hint`
- `settings_hide_launcher_icon_confirm_title`、`settings_hide_launcher_icon_confirm_message`

### Task 1: Manifest 入口拆分（LAUNCHER Alias）+ AboutActivity 注册

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java`

- [ ] **Step 1: 写失败测试（或先补断言）**

```java
assertTrue(manifest.contains("android:name=\".MainActivityLauncher\""));
assertTrue(manifest.contains("android.intent.category.LAUNCHER"));
assertTrue(manifest.contains("de.robv.android.xposed.category.MODULE_SETTINGS"));
```

- [ ] **Step 2: 运行目标测试确认基线**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.LegacyModuleManifestMetadataTest`  
Expected: PASS（新增断言前可能 PASS；新增断言后若未改 manifest 应 FAIL）

- [ ] **Step 3: 最小实现修改 Manifest**

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="de.robv.android.xposed.category.MODULE_SETTINGS" />
    </intent-filter>
</activity>

<activity-alias
    android:name=".MainActivityLauncher"
    android:targetActivity=".MainActivity"
    android:exported="true"
    android:label="@string/app_name">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity-alias>

<activity android:name=".AboutActivity" android:exported="false" />
```

- [ ] **Step 4: 复跑目标测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.LegacyModuleManifestMetadataTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/test/java/com/dpis/module/LegacyModuleManifestMetadataTest.java
git commit -m "feat: split launcher entry into manifest alias for icon visibility toggle"
```

### Task 2: 配置层新增“隐藏桌面图标”键 + 迁移

**Files:**
- Modify: `app/src/main/java/com/dpis/module/DpiConfigStore.java`
- Modify: `app/src/main/java/com/dpis/module/DpisApplication.java`
- Test: `app/src/test/java/com/dpis/module/DpiConfigStoreTest.java`
- Test: `app/src/test/java/com/dpis/module/DpisApplicationMigrationTest.java`

- [ ] **Step 1: 写失败测试（Store 默认值/更新）**

```java
assertFalse(store.isLauncherIconHidden());
assertTrue(store.setLauncherIconHidden(true));
assertTrue(store.isLauncherIconHidden());
```

- [ ] **Step 2: 写失败测试（迁移）**

```java
assertTrue(local.setLauncherIconHidden(true));
invokeMigrate(local, remote);
assertTrue(remote.isLauncherIconHidden());
```

- [ ] **Step 3: 最小实现（Store + migrate）**

```java
static final String KEY_HIDE_LAUNCHER_ICON = "ui.hide_launcher_icon";

boolean isLauncherIconHidden() {
    return getBoolean(KEY_HIDE_LAUNCHER_ICON, false);
}

boolean hasLauncherIconHidden() {
    return containsInPrimary(KEY_HIDE_LAUNCHER_ICON);
}

boolean setLauncherIconHidden(boolean hidden) {
    return commitBoth(editor -> editor.putBoolean(KEY_HIDE_LAUNCHER_ICON, hidden));
}
```

```java
if (from.hasLauncherIconHidden() && !to.hasLauncherIconHidden()) {
    to.setLauncherIconHidden(from.isLauncherIconHidden());
}
```

- [ ] **Step 4: 运行目标测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.DpiConfigStoreTest --tests com.dpis.module.DpisApplicationMigrationTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/DpiConfigStore.java app/src/main/java/com/dpis/module/DpisApplication.java app/src/test/java/com/dpis/module/DpiConfigStoreTest.java app/src/test/java/com/dpis/module/DpisApplicationMigrationTest.java
git commit -m "feat: persist and migrate launcher icon visibility setting"
```

### Task 3: 设置页布局与文案新增“其他”分组

**Files:**
- Modify: `app/src/main/res/layout/activity_system_server_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 新增布局断言测试（如需）**

```java
assertTrue(layout.contains("android:id=\"@+id/row_about\""));
assertTrue(layout.contains("android:id=\"@+id/row_hide_launcher_icon\""));
```

- [ ] **Step 2: 运行目标测试（若已新增）**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest`  
Expected: PASS（此步主要保证无现有回归）

- [ ] **Step 3: 最小实现布局与字符串**

```xml
<com.google.android.material.textview.MaterialTextView
    android:text="@string/settings_section_other" ... />

<include
    android:id="@+id/row_about"
    layout="@layout/item_settings_entry" />

<include
    android:id="@+id/row_hide_launcher_icon"
    layout="@layout/item_settings_switch" />
```

```xml
<string name="settings_section_other">其他</string>
<string name="settings_about_label">关于</string>
<string name="settings_about_hint">查看版本、源码与反馈渠道。</string>
<string name="settings_hide_launcher_icon_label">隐藏桌面图标</string>
<string name="settings_hide_launcher_icon_hint">隐藏后可从 LSPosed 模块设置入口进入。</string>
```

- [ ] **Step 4: 运行资源相关测试**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.FilterSheetLayoutSmokeTest`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_system_server_settings.xml app/src/main/res/values/strings.xml
git commit -m "feat: add other section with about entry and launcher icon switch row"
```

### Task 4: 设置页控制器接线（关于跳转 + 隐藏图标开关）

**Files:**
- Modify: `app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java`

- [ ] **Step 1: 写失败测试（可选源码烟测）**

```java
assertTrue(source.contains("R.id.row_about"));
assertTrue(source.contains("R.id.row_hide_launcher_icon"));
assertTrue(source.contains("new Intent(this, AboutActivity.class)"));
assertTrue(source.contains("setComponentEnabledSetting"));
```

- [ ] **Step 2: 运行目标测试（若新增）**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivitySourceSmokeTest`  
Expected: PASS（确保未影响 MainActivity 现有逻辑）

- [ ] **Step 3: 最小实现控制逻辑**

```java
aboutEntryRow = bindEntryRow(
        R.id.row_about,
        R.drawable.ic_info_outline_24,
        R.string.settings_about_label,
        R.string.settings_about_hint,
        v -> startActivity(new Intent(this, AboutActivity.class)));

hideLauncherIconSwitch = bindSwitchRow(
        R.id.row_hide_launcher_icon,
        R.drawable.ic_block_24,
        R.string.settings_hide_launcher_icon_label,
        R.string.settings_hide_launcher_icon_hint);
hideLauncherIconSwitch.setChecked(store.isLauncherIconHidden());
hideLauncherIconSwitch.setOnCheckedChangeListener(this::onHideLauncherIconChanged);
```

```java
ComponentName alias = new ComponentName(this, getPackageName() + ".MainActivityLauncher");
int state = hidden
        ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
getPackageManager().setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP);
```

- [ ] **Step 4: 手动功能验证**

Run:  
`./gradlew :app:assembleDebug`  
`adb install -r "app/build/outputs/apk/debug/app-debug.apk"`  
Expected:
- 设置页出现“其他”分组；
- 点击“关于”可进入新页面；
- 开关可切换且失败时回滚。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/SystemServerSettingsActivity.java
git commit -m "feat: wire about navigation and launcher icon visibility toggle in settings"
```

### Task 5: 新建 AboutActivity 与页面布局（参考 InstallerX 内容组织）

**Files:**
- Create: `app/src/main/java/com/dpis/module/AboutActivity.java`
- Create: `app/src/main/res/layout/activity_about.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 写失败测试（布局/源码烟测）**

```java
assertTrue(layout.contains("@+id/about_back_button"));
assertTrue(layout.contains("@+id/row_about_source"));
assertTrue(layout.contains("@+id/row_about_update"));
assertTrue(layout.contains("@+id/row_about_feedback"));
```

- [ ] **Step 2: 运行目标测试（若新增）**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest`  
Expected: PASS（现有布局不回归）

- [ ] **Step 3: 最小实现 About 页面**

```java
titleView.setText(getString(R.string.app_name));
versionView.setText(getString(R.string.about_version_format,
        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
descView.setText(R.string.module_description);
bindEntryRow(R.id.row_about_source, R.drawable.ic_info_outline_24,
        R.string.about_link_source_title, R.string.about_link_source_desc,
        v -> openUrl(getString(R.string.about_source_url)));
```

```xml
<string name="about_title">关于</string>
<string name="about_version_format">版本：%1$s (%2$d)</string>
<string name="about_source_url">https://github.com/Kwensiu/DPIS</string>
<string name="about_releases_url">https://github.com/Kwensiu/DPIS/releases</string>
<string name="about_issues_url">https://github.com/Kwensiu/DPIS/issues</string>
```

- [ ] **Step 4: 运行构建与单测**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dpis/module/AboutActivity.java app/src/main/res/layout/activity_about.xml app/src/main/res/values/strings.xml
git commit -m "feat: add about page with source update and feedback links"
```

### Task 6: 联调验收与回归

**Files:**
- Verify only (no mandatory code changes)

- [ ] **Step 1: 功能联调**

Run: `adb shell cmd package resolve-activity --brief com.dpis.module`  
Expected: 可解析 launcher alias（隐藏前）；隐藏后 launcher 不可见。

- [ ] **Step 2: 模块入口回归**

Run: 在 LSPosed 中打开模块设置入口  
Expected: 即使隐藏桌面图标，模块设置入口仍可打开。

- [ ] **Step 3: 配置持久化回归**

Run:  
1. 打开隐藏开关  
2. 强杀应用并重进设置页  
Expected: 开关状态保持，组件状态一致。

- [ ] **Step 4: 全量测试**

Run: `./gradlew :app:testDebugUnitTest`  
Expected: PASS

- [ ] **Step 5: 发布前检查**

Run: `./gradlew :app:assembleDebug`  
Expected: BUILD SUCCESSFUL，APK 可安装。

---

## 风险与边界

- 若 alias 名称与 Manifest 声明不一致，开关会失效；必须统一使用同一常量字符串。
- 某些桌面启动器图标刷新有延迟，属于系统行为，不应误判为失败。
- 本计划仅实现“打开外链式更新入口”，不包含“在线版本检查/应用内更新下载”链路。

## 执行顺序（强约束）
1. 先做 Task 1（入口拆分），再做隐藏图标逻辑；否则会误伤主入口。
2. Task 2（Store + migrate）必须早于 Task 4（控制器接线），避免运行时访问不存在 API。
3. Task 5 可与 Task 4 并行开发，但合并前必须一起通过 Task 6。
