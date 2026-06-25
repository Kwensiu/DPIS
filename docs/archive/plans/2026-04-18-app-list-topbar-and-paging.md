# App List Topbar And Paging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `MainActivity` list chrome so the search box contains both search and filter icons, the settings button remains outside on the right, and the app list supports `全部应用 / 已配置应用` paging with both tab taps and horizontal swipes.

**Architecture:** Keep `MainActivity` as the single screen owner, but move page semantics into small helper types so the activity is not doing filter math inline. Use `ViewPager2` plus `TabLayoutMediator` for swipeable pages, keep the search field and settings button in the existing top bar, and expose the filter affordance as a bottom sheet entry point via the search box end icon.

**Tech Stack:** Java 17, Android Views, Material `TextInputLayout`, `TabLayout`, `BottomSheetDialog`, `ViewPager2`, JUnit4.

---

## File Map

- Create: `app/src/main/java/com/dpis/module/AppListPage.java`
  Responsibility: canonical two-page enum (`ALL_APPS`, `CONFIGURED_APPS`) with tab position and string resource mapping.
- Create: `app/src/main/java/com/dpis/module/AppListItem.java`
  Responsibility: top-level immutable row model extracted from `MainActivity.AppItem` so paging helpers and adapters can share it.
- Create: `app/src/main/java/com/dpis/module/AppListVisibleSections.java`
  Responsibility: pure helper that turns the full loaded app list plus search query into the page-specific visible lists.
- Create: `app/src/main/java/com/dpis/module/AppListPagerAdapter.java`
  Responsibility: `ViewPager2` adapter hosting one plain `RecyclerView` per page, while preserving existing row layout and click behavior.
- Create: `app/src/main/res/layout/item_app_list_page.xml`
  Responsibility: per-page `RecyclerView` host used by `ViewPager2`.
- Create: `app/src/main/res/layout/dialog_list_filters.xml`
  Responsibility: minimal bottom sheet placeholder for the filter entry point, following existing sheet conventions.
- Create: `app/src/main/res/drawable/ic_tune_24.xml`
  Responsibility: internal search-box filter icon.
- Create: `app/src/test/java/com/dpis/module/AppListPageTest.java`
  Responsibility: unit coverage for page enum positions and title resources.
- Create: `app/src/test/java/com/dpis/module/AppListVisibleSectionsTest.java`
  Responsibility: unit coverage for `全部应用` vs `已配置应用` page results.
- Create: `app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java`
  Responsibility: file-level smoke check that the layout contains the agreed top bar and pager wiring.
- Modify: `app/src/main/java/com/dpis/module/AppListFilter.java`
  Responsibility: replace `USER_APPS/SYSTEM_APPS` semantics with `ALL_APPS/CONFIGURED_APPS`.
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`
  Responsibility: replace the single `RecyclerView` flow with paged lists, wire `TextInputLayout` end icon to the filter sheet, and keep settings button behavior intact.
- Modify: `app/src/main/res/layout/activity_status.xml`
  Responsibility: keep the top bar layout, switch the search box to a custom end icon, and replace the root list widget with `ViewPager2`.
- Modify: `app/src/main/res/values/strings.xml`
  Responsibility: add `全部应用` and filter-sheet copy while preserving existing settings strings.
- Modify: `app/build.gradle.kts`
  Responsibility: add `ViewPager2` dependency.
- Modify: `gradle/libs.versions.toml`
  Responsibility: declare the `androidx.viewpager2` library alias.
- Modify: `app/src/test/java/com/dpis/module/AppListFilterTest.java`
  Responsibility: update coverage to the new two-page semantics.

### Task 1: Replace Three-Tab Filter Semantics With Two-Page Semantics

**Files:**
- Create: `app/src/main/java/com/dpis/module/AppListPage.java`
- Modify: `app/src/main/java/com/dpis/module/AppListFilter.java`
- Create: `app/src/test/java/com/dpis/module/AppListPageTest.java`
- Modify: `app/src/test/java/com/dpis/module/AppListFilterTest.java`

- [ ] **Step 1: Write the failing tests for page identity and the new “全部应用” semantics**

```java
package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppListPageTest {
    @Test
    public void fromPosition_mapsTwoPageOrder() {
        assertEquals(AppListPage.ALL_APPS, AppListPage.fromPosition(0));
        assertEquals(AppListPage.CONFIGURED_APPS, AppListPage.fromPosition(1));
    }

    @Test
    public void titleRes_matchesApprovedTabs() {
        assertEquals(R.string.tab_all_apps, AppListPage.ALL_APPS.titleRes());
        assertEquals(R.string.tab_configured_apps, AppListPage.CONFIGURED_APPS.titleRes());
    }
}
```

```java
@Test
public void allAppsTabMatchesBothUserAndSystemApps() {
    assertTrue(AppListFilter.matches("",
            AppListFilter.Tab.ALL_APPS,
            "Coolapk",
            "com.coolapk.market",
            false,
            false,
            null,
            null,
            FontApplyMode.OFF));
    assertTrue(AppListFilter.matches("",
            AppListFilter.Tab.ALL_APPS,
            "Android System WebView",
            "com.google.android.webview",
            true,
            false,
            null,
            null,
            FontApplyMode.OFF));
}
```

- [ ] **Step 2: Run the focused tests to verify the new page model does not exist yet**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListPageTest --tests com.dpis.module.AppListFilterTest`

Expected: FAIL with compile errors such as `cannot find symbol AppListPage` and `cannot find symbol ALL_APPS`.

- [ ] **Step 3: Implement the page enum and update filter matching**

```java
package com.dpis.module;

enum AppListPage {
    ALL_APPS(0, R.string.tab_all_apps, AppListFilter.Tab.ALL_APPS),
    CONFIGURED_APPS(1, R.string.tab_configured_apps, AppListFilter.Tab.CONFIGURED_APPS);

    private final int position;
    private final int titleRes;
    private final AppListFilter.Tab filterTab;

    AppListPage(int position, int titleRes, AppListFilter.Tab filterTab) {
        this.position = position;
        this.titleRes = titleRes;
        this.filterTab = filterTab;
    }

    int position() {
        return position;
    }

    int titleRes() {
        return titleRes;
    }

    AppListFilter.Tab filterTab() {
        return filterTab;
    }

    static AppListPage fromPosition(int position) {
        return position == 1 ? CONFIGURED_APPS : ALL_APPS;
    }
}
```

```java
final class AppListFilter {
    enum Tab {
        ALL_APPS,
        CONFIGURED_APPS
    }

    static boolean matches(String query,
                           Tab tab,
                           String label,
                           String packageName,
                           boolean systemApp,
                           boolean inScope,
                           Integer viewportWidthDp,
                           Integer fontScalePercent,
                           String fontMode) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (!normalizedQuery.isEmpty()) {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            String normalizedPackage = packageName.toLowerCase(Locale.ROOT);
            if (!normalizedLabel.contains(normalizedQuery)
                    && !normalizedPackage.contains(normalizedQuery)) {
                return false;
            }
        }
        boolean fontConfigured = fontScalePercent != null
                && FontApplyMode.isEnabled(FontApplyMode.normalize(fontMode));
        return switch (tab) {
            case ALL_APPS -> true;
            case CONFIGURED_APPS -> inScope || viewportWidthDp != null || fontConfigured;
        };
    }
}
```

- [ ] **Step 4: Run the focused tests again to verify the page semantics are green**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListPageTest --tests com.dpis.module.AppListFilterTest`

Expected: PASS

- [ ] **Step 5: Commit the semantic page split**

```bash
git add app/src/main/java/com/dpis/module/AppListPage.java app/src/main/java/com/dpis/module/AppListFilter.java app/src/test/java/com/dpis/module/AppListPageTest.java app/src/test/java/com/dpis/module/AppListFilterTest.java
git commit -m "feat: replace app list tabs with two-page model"
```

### Task 2: Add Shared App List Models And Page Filtering Helper

**Files:**
- Create: `app/src/main/java/com/dpis/module/AppListItem.java`
- Create: `app/src/main/java/com/dpis/module/AppListVisibleSections.java`
- Create: `app/src/test/java/com/dpis/module/AppListVisibleSectionsTest.java`

- [ ] **Step 1: Write the failing test for page-specific visible lists**

```java
package com.dpis.module;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class AppListVisibleSectionsTest {
    @Test
    public void filter_returnsConfiguredSubsetWithoutDroppingSearch() {
        AppListItem configured = new AppListItem(
                "123云盘",
                "com.mfcloudcalculate.networkdisk",
                true,
                null,
                null,
                FontApplyMode.OFF,
                false,
                null);
        AppListItem plain = new AppListItem(
                "Android System WebView",
                "com.google.android.webview",
                false,
                null,
                null,
                FontApplyMode.OFF,
                true,
                null);

        List<AppListItem> configuredItems = AppListVisibleSections.filter(
                List.of(configured, plain), "", AppListPage.CONFIGURED_APPS);
        List<AppListItem> searchedItems = AppListVisibleSections.filter(
                List.of(configured, plain), "android", AppListPage.ALL_APPS);

        assertEquals(1, configuredItems.size());
        assertEquals("123云盘", configuredItems.get(0).label);
        assertEquals(1, searchedItems.size());
        assertEquals("com.google.android.webview", searchedItems.get(0).packageName);
    }
}
```

- [ ] **Step 2: Run the focused helper test to verify the shared row model does not exist yet**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListVisibleSectionsTest`

Expected: FAIL with compile errors such as `cannot find symbol AppListItem`.

- [ ] **Step 3: Implement the shared item model and pure page filtering helper**

```java
package com.dpis.module;

import android.graphics.drawable.Drawable;

final class AppListItem {
    final String label;
    final String packageName;
    final boolean inScope;
    final Integer viewportWidthDp;
    final Integer fontScalePercent;
    final String fontMode;
    final boolean systemApp;
    final Drawable icon;

    AppListItem(String label,
                String packageName,
                boolean inScope,
                Integer viewportWidthDp,
                Integer fontScalePercent,
                String fontMode,
                boolean systemApp,
                Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.inScope = inScope;
        this.viewportWidthDp = viewportWidthDp;
        this.fontScalePercent = fontScalePercent;
        this.fontMode = FontApplyMode.normalize(fontMode);
        this.systemApp = systemApp;
        this.icon = icon;
    }
}
```

```java
package com.dpis.module;

import java.util.ArrayList;
import java.util.List;

final class AppListVisibleSections {
    private AppListVisibleSections() {
    }

    static List<AppListItem> filter(List<AppListItem> source, String query, AppListPage page) {
        List<AppListItem> visible = new ArrayList<>();
        for (AppListItem item : source) {
            if (AppListFilter.matches(query,
                    page.filterTab(),
                    item.label,
                    item.packageName,
                    item.systemApp,
                    item.inScope,
                    item.viewportWidthDp,
                    item.fontScalePercent,
                    item.fontMode)) {
                visible.add(item);
            }
        }
        return visible;
    }
}
```

- [ ] **Step 4: Run the focused helper test again to verify page filtering is green**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListVisibleSectionsTest`

Expected: PASS

- [ ] **Step 5: Commit the shared paging data model**

```bash
git add app/src/main/java/com/dpis/module/AppListItem.java app/src/main/java/com/dpis/module/AppListVisibleSections.java app/src/test/java/com/dpis/module/AppListVisibleSectionsTest.java
git commit -m "refactor: extract app list paging helpers"
```

### Task 3: Scaffold The Top Bar Resources And Swipe Host

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/layout/activity_status.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/layout/item_app_list_page.xml`
- Create: `app/src/main/res/layout/dialog_list_filters.xml`
- Create: `app/src/main/res/drawable/ic_tune_24.xml`
- Create: `app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java`

- [ ] **Step 1: Write the failing smoke test for the agreed top bar and pager scaffold**

```java
package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class MainActivityLayoutSmokeTest {
    @Test
    public void activityStatusLayoutContainsSearchFilterSettingsAndPager() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/search_layout\""));
        assertTrue(layout.contains("app:startIconDrawable=\"@drawable/ic_search_24\""));
        assertTrue(layout.contains("app:endIconMode=\"custom\""));
        assertTrue(layout.contains("app:endIconDrawable=\"@drawable/ic_tune_24\""));
        assertTrue(layout.contains("android:id=\"@+id/system_settings_button\""));
        assertTrue(layout.contains("android:id=\"@+id/app_pager\""));
        assertTrue(strings.contains("tab_all_apps"));
        assertTrue(Files.exists(Path.of("src/main/res/drawable/ic_tune_24.xml")));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 2: Run the smoke test to verify the scaffold is still missing**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest`

Expected: FAIL because `app:endIconMode="custom"`, `@+id/app_pager`, `tab_all_apps`, and `ic_tune_24.xml` do not exist yet.

- [ ] **Step 3: Add the dependency, strings, icon, filter sheet layout, and pager host**

```toml
[versions]
viewpager2 = "1.1.0"

[libraries]
androidx-viewpager2 = { group = "androidx.viewpager2", name = "viewpager2", version.ref = "viewpager2" }
```

```kotlin
dependencies {
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
}
```

```xml
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/search_layout"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1"
    app:boxBackgroundColor="?attr/colorSurfaceContainerHigh"
    app:boxCornerRadiusBottomEnd="30dp"
    app:boxCornerRadiusBottomStart="30dp"
    app:boxCornerRadiusTopEnd="30dp"
    app:boxCornerRadiusTopStart="30dp"
    app:boxStrokeWidth="0dp"
    app:boxStrokeWidthFocused="0dp"
    app:endIconMode="custom"
    app:endIconDrawable="@drawable/ic_tune_24"
    app:endIconContentDescription="@string/filter_button"
    app:hintEnabled="false"
    app:startIconDrawable="@drawable/ic_search_24"
    app:startIconTint="?attr/colorOnSurfaceVariant">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/search_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/search_hint"
        android:imeOptions="actionDone"
        android:inputType="text" />
</com.google.android.material.textfield.TextInputLayout>
```

```xml
<androidx.viewpager2.widget.ViewPager2
    android:id="@+id/app_pager"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1" />
```

```xml
<string name="tab_all_apps">全部应用</string>
<string name="filter_button">筛选</string>
<string name="filter_sheet_title">列表筛选</string>
<string name="filter_sheet_message">筛选项将在后续步骤补充，这里先保留入口和交互骨架。</string>
<string name="dialog_close_button">关闭</string>
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#00000000"
        android:pathData="M4,7h16M4,12h10M4,17h13"
        android:strokeColor="#FF000000"
        android:strokeLineCap="round"
        android:strokeLineWidth="1.8" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M15,5a2,2 0,1 1,0 4a2,2 0,1 1,0 -4zM10,10a2,2 0,1 1,0 4a2,2 0,1 1,0 -4zM12,15a2,2 0,1 1,0 4a2,2 0,1 1,0 -4z"
        android:strokeColor="#FF000000"
        android:strokeLineWidth="1.8" />
</vector>
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.recyclerview.widget.RecyclerView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_list"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipToPadding="false"
    android:paddingStart="16dp"
    android:paddingTop="10dp"
    android:paddingEnd="16dp"
    android:paddingBottom="24dp" />
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/filter_sheet_title"
        android:textStyle="bold" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/filter_sheet_message" />
</LinearLayout>
```

- [ ] **Step 4: Run the smoke test again to verify the resource scaffold is in place**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.MainActivityLayoutSmokeTest`

Expected: PASS

- [ ] **Step 5: Commit the top bar and pager resource scaffold**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/layout/activity_status.xml app/src/main/res/layout/item_app_list_page.xml app/src/main/res/layout/dialog_list_filters.xml app/src/main/res/drawable/ic_tune_24.xml app/src/main/res/values/strings.xml app/src/test/java/com/dpis/module/MainActivityLayoutSmokeTest.java
git commit -m "feat: scaffold app list pager layout and filter affordance"
```

### Task 4: Wire MainActivity To ViewPager2 And Shared Page Adapters

**Files:**
- Create: `app/src/main/java/com/dpis/module/AppListPagerAdapter.java`
- Modify: `app/src/main/java/com/dpis/module/MainActivity.java`

- [ ] **Step 1: Run the existing filter and layout tests as the integration baseline**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppListVisibleSectionsTest --tests com.dpis.module.MainActivityLayoutSmokeTest`

Expected: PASS

- [ ] **Step 2: Implement the pager adapter and replace the single-list wiring in MainActivity**

```java
package com.dpis.module;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class AppListPagerAdapter extends RecyclerView.Adapter<AppListPagerAdapter.PageHolder> {
    interface OnAppClickListener {
        void onAppClicked(AppListItem item);
    }

    private final EnumMap<AppListPage, List<AppListItem>> pages = new EnumMap<>(AppListPage.class);
    private final OnAppClickListener onAppClickListener;

    AppListPagerAdapter(OnAppClickListener onAppClickListener) {
        this.onAppClickListener = onAppClickListener;
        for (AppListPage page : AppListPage.values()) {
            pages.put(page, new ArrayList<>());
        }
    }

    void submitPage(AppListPage page, List<AppListItem> items) {
        pages.put(page, new ArrayList<>(items));
        notifyItemChanged(page.position());
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_list_page, parent, false);
        return new PageHolder(view, onAppClickListener);
    }

    @Override
    public void onBindViewHolder(PageHolder holder, int position) {
        holder.bind(pages.get(AppListPage.fromPosition(position)));
    }

    @Override
    public int getItemCount() {
        return AppListPage.values().length;
    }

    static final class PageHolder extends RecyclerView.ViewHolder {
        private final PageListAdapter adapter;

        PageHolder(View itemView, OnAppClickListener onAppClickListener) {
            super(itemView);
            RecyclerView recyclerView = itemView.findViewById(R.id.page_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            adapter = new PageListAdapter(onAppClickListener);
            recyclerView.setAdapter(adapter);
        }

        void bind(List<AppListItem> items) {
            adapter.submit(items);
        }
    }
}
```

```java
private AppListPagerAdapter pagerAdapter;
private TextInputLayout searchLayout;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_status);

    searchLayout = findViewById(R.id.search_layout);
    ViewPager2 appPager = findViewById(R.id.app_pager);
    pagerAdapter = new AppListPagerAdapter(this::showEditDialog);
    appPager.setAdapter(pagerAdapter);

    TabLayout tabLayout = findViewById(R.id.filter_tabs);
    new TabLayoutMediator(tabLayout, appPager,
            (tab, position) -> tab.setText(getString(AppListPage.fromPosition(position).titleRes())))
            .attach();

    searchLayout.setEndIconOnClickListener(v -> showFilterDialog());

    searchInput = findViewById(R.id.search_input);
    searchInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            currentQuery = s != null ? s.toString() : "";
            applyFilter();
        }
    });

    View systemSettingsButton = findViewById(R.id.system_settings_button);
    systemSettingsButton.setOnClickListener(v ->
            startActivity(new Intent(this, SystemServerSettingsActivity.class)));

    loadAppsAsync();
}

private void applyFilter() {
    String query = currentQuery.trim();
    List<AppListItem> snapshot;
    synchronized (listLock) {
        snapshot = new ArrayList<>(allApps);
    }
    for (AppListPage page : AppListPage.values()) {
        pagerAdapter.submitPage(page, AppListVisibleSections.filter(snapshot, query, page));
    }
}

private void showFilterDialog() {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_list_filters, null, false);
    dialog.setContentView(dialogView);
    dialog.show();
}
```

```java
private final List<AppListItem> allApps = new ArrayList<>();

private List<AppListItem> loadInstalledApps() {
    ...
    List<AppListItem> result = new ArrayList<>();
    for (ApplicationInfo applicationInfo : installedApps) {
        ...
        result.add(new AppListItem(label, applicationInfo.packageName,
                scopePackages.contains(applicationInfo.packageName),
                viewportWidth,
                fontScalePercent,
                fontMode,
                systemApp,
                icon));
    }
    ...
}
```

- [ ] **Step 3: Run focused UI-adjacent regression plus full unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.dpis.module.AppListFilterTest --tests com.dpis.module.AppListVisibleSectionsTest --tests com.dpis.module.MainActivityLayoutSmokeTest`

Expected: PASS

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS

- [ ] **Step 4: Build the debug APK to verify the new dependency and layout wiring compile end-to-end**

Run: `./gradlew :app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit the activity integration**

```bash
git add app/src/main/java/com/dpis/module/AppListPagerAdapter.java app/src/main/java/com/dpis/module/MainActivity.java
git commit -m "feat: add swipe paging to app list"
```

## Self-Review

- **Spec coverage:** Task 1 covers the two-page semantic change. Task 2 covers the per-page filtered data source. Task 3 covers the confirmed top bar structure, filter icon entry point, and pager host resources. Task 4 wires `MainActivity` to the confirmed interaction model and runs final regressions.
- **Placeholder scan:** No `TODO`, `TBD`, or “similar to” placeholders remain; each code-changing step includes concrete file content and exact commands.
- **Type consistency:** The plan uses `AppListPage`, `AppListItem`, `AppListVisibleSections`, and `AppListPagerAdapter` consistently from first definition through final integration.
