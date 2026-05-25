# Hook Chain Dialog UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename and restructure the per-app Hook chain dialog into `自定义 Hook 链路` with `界面` and `字体` tabs.

**Architecture:** Keep the existing `FontHookDomainDialog` behavior and host API. Add a Material `TabLayout` to the dialog layout, split the current viewport apply rows and font hook rows into two tab containers, and toggle visibility in the dialog binder.

**Tech Stack:** Android Java 17, Material Components, JUnit4 source smoke tests.

---

### Task 1: Hook Chain Dialog Tabs

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/layout/dialog_font_hook_domains.xml`
- Modify: `app/src/main/java/com/dpis/module/FontHookDomainDialog.java`
- Modify: `app/src/test/java/com/dpis/module/AppConfigDialogBinderSourceSmokeTest.java`

- [ ] **Step 1: Add/rename strings**

In `app/src/main/res/values-zh-rCN/strings.xml`, set:

```xml
<string name="dialog_font_hook_domains_dialog_title">自定义 Hook 链路</string>
<string name="dialog_hook_chain_tab_interface">界面</string>
<string name="dialog_hook_chain_tab_font">字体</string>
<string name="dialog_viewport_apply_strategy_title">界面比例应用策略</string>
```

In `app/src/main/res/values/strings.xml`, set:

```xml
<string name="dialog_font_hook_domains_dialog_title">Custom Hook Chain</string>
<string name="dialog_hook_chain_tab_interface">Interface</string>
<string name="dialog_hook_chain_tab_font">Font</string>
<string name="dialog_viewport_apply_strategy_title">Interface scale apply strategy</string>
```

- [ ] **Step 2: Add tabs and content containers**

In `app/src/main/res/layout/dialog_font_hook_domains.xml`, add a `com.google.android.material.tabs.TabLayout` near the top of the dialog content:

```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/font_hook_domains_tabs"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:tabMode="fixed"
    app:tabGravity="fill" />
```

Wrap the existing viewport apply title/container in:

```xml
<LinearLayout
    android:id="@+id/font_hook_domains_interface_page"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    ...
</LinearLayout>
```

Wrap the existing font domain known/unknown/restore controls in:

```xml
<LinearLayout
    android:id="@+id/font_hook_domains_font_page"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone">
    ...
</LinearLayout>
```

- [ ] **Step 3: Bind tab visibility**

In `FontHookDomainDialog.show`, read the new views:

```java
TabLayout tabs = view.findViewById(R.id.font_hook_domains_tabs);
View interfacePage = view.findViewById(R.id.font_hook_domains_interface_page);
View fontPage = view.findViewById(R.id.font_hook_domains_font_page);
```

Import `com.google.android.material.tabs.TabLayout`.

Add tabs and toggle pages:

```java
tabs.addTab(tabs.newTab().setText(R.string.dialog_hook_chain_tab_interface));
tabs.addTab(tabs.newTab().setText(R.string.dialog_hook_chain_tab_font));
tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        boolean interfaceSelected = tab.getPosition() == 0;
        interfacePage.setVisibility(interfaceSelected ? View.VISIBLE : View.GONE);
        fontPage.setVisibility(interfaceSelected ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }
});
```

- [ ] **Step 4: Add a source smoke assertion**

In `AppConfigDialogBinderSourceSmokeTest`, add a test that reads `FontHookDomainDialog.java` and asserts it contains:

```java
assertTrue(source.contains("dialog_hook_chain_tab_interface"));
assertTrue(source.contains("dialog_hook_chain_tab_font"));
assertTrue(source.contains("font_hook_domains_interface_page"));
assertTrue(source.contains("font_hook_domains_font_page"));
```

- [ ] **Step 5: Run targeted tests**

Run:

```powershell
./gradlew :app:testModern101DebugUnitTest --tests com.dpis.module.AppConfigDialogBinderSourceSmokeTest
```

Expected: build succeeds.

- [ ] **Step 6: Run full verification**

Run:

```powershell
./gradlew :app:testAllDebugUnitTests
./gradlew :app:assembleModern101Debug :app:assembleCompat100Debug
git diff --check
```

Expected: all commands succeed.

- [ ] **Step 7: Commit**

Commit only the spec, plan, strings, layout, Java dialog, and related test files:

```powershell
git add docs/superpowers/specs/2026-05-25-hook-chain-dialog-ui-design.md docs/superpowers/plans/2026-05-25-hook-chain-dialog-ui.md app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/res/layout/dialog_font_hook_domains.xml app/src/main/java/com/dpis/module/FontHookDomainDialog.java app/src/test/java/com/dpis/module/AppConfigDialogBinderSourceSmokeTest.java
git commit -m "feat: add hook chain dialog tabs"
```
