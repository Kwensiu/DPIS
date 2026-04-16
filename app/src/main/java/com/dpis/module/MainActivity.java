package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements DpisApplication.ServiceStateListener {
    private final List<AppItem> allApps = new ArrayList<>();
    private final List<AppItem> filteredApps = new ArrayList<>();
    private final Object listLock = new Object();

    private AppListAdapter adapter;
    private String currentQuery = "";
    private AppListFilter.Tab currentTab = AppListFilter.Tab.USER_APPS;
    private TextInputEditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        RecyclerView recyclerView = findViewById(R.id.app_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter();
        recyclerView.setAdapter(adapter);
        applyInsets();

        TabLayout tabLayout = findViewById(R.id.filter_tabs);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_user_apps));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_configured_apps));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_system_apps));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = mapTabPosition(tab.getPosition());
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                searchInput.setHint("");
                return;
            }
            CharSequence current = searchInput.getText();
            if (current == null || current.length() == 0) {
                searchInput.setHint(getString(R.string.search_hint));
            }
        });

        View systemSettingsButton = findViewById(R.id.system_settings_button);
        systemSettingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, SystemServerSettingsActivity.class)));

        loadAppsAsync();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && searchInput != null && searchInput.hasFocus()) {
            Rect outRect = new Rect();
            searchInput.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                clearSearchFocus();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(this::loadAppsAsync);
    }

    private void loadAppsAsync() {
        new Thread(() -> {
            List<AppItem> loaded = loadInstalledApps();
            synchronized (listLock) {
                allApps.clear();
                allApps.addAll(loaded);
            }
            runOnUiThread(this::applyFilter);
        }, "dpis-load-apps").start();
    }

    private void applyInsets() {
        View topContainer = findViewById(R.id.top_container);
        final int baseTopPadding = topContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(topContainer, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(topContainer);
    }

    private void clearSearchFocus() {
        if (searchInput == null) {
            return;
        }
        searchInput.clearFocus();
        Editable current = searchInput.getText();
        if (current == null || current.length() == 0) {
            searchInput.setHint(getString(R.string.search_hint));
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    private List<AppItem> loadInstalledApps() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installedApps;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            installedApps = packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0));
        } else {
            installedApps = packageManager.getInstalledApplications(0);
        }

        Set<String> scopePackages = new HashSet<>();
        XposedService service = DpisApplication.getXposedService();
        if (service != null) {
            scopePackages.addAll(service.getScope());
        }
        DpiConfigStore store = DpisApplication.getConfigStore();

        List<AppItem> result = new ArrayList<>();
        for (ApplicationInfo applicationInfo : installedApps) {
            if (applicationInfo.packageName.equals(getPackageName())) {
                continue;
            }
            boolean systemApp = isSystemApp(applicationInfo);
            String label = packageManager.getApplicationLabel(applicationInfo).toString();
            Drawable icon = applicationInfo.loadIcon(packageManager);
            Integer viewportWidth = store != null
                    ? store.getTargetViewportWidthDp(applicationInfo.packageName)
                    : null;
            result.add(new AppItem(label, applicationInfo.packageName,
                    scopePackages.contains(applicationInfo.packageName), viewportWidth, systemApp,
                    icon));
        }
        result.sort(Comparator.comparing((AppItem item) -> item.label.toLowerCase(Locale.ROOT))
                .thenComparing(item -> item.packageName));
        return result;
    }

    private static boolean isSystemApp(ApplicationInfo applicationInfo) {
        int flags = applicationInfo.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
    }

    private void applyFilter() {
        String query = currentQuery.trim();
        synchronized (listLock) {
            filteredApps.clear();
            for (AppItem item : allApps) {
                if (AppListFilter.matches(query, currentTab, item.label, item.packageName,
                        item.systemApp, item.inScope, item.viewportWidthDp)) {
                    filteredApps.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private static AppListFilter.Tab mapTabPosition(int position) {
        if (position == 1) {
            return AppListFilter.Tab.CONFIGURED_APPS;
        }
        if (position == 2) {
            return AppListFilter.Tab.SYSTEM_APPS;
        }
        return AppListFilter.Tab.USER_APPS;
    }

    private void toggleScope(AppItem item) {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (item.inScope) {
            service.removeScope(Collections.singletonList(item.packageName));
            Toast.makeText(this, getString(R.string.scope_remove_success, item.packageName),
                    Toast.LENGTH_SHORT).show();
            loadAppsAsync();
            return;
        }
        service.requestScope(Collections.singletonList(item.packageName),
                new XposedService.OnScopeEventListener() {
                    @Override
                    public void onScopeRequestApproved(List<String> approved) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.scope_add_success, item.packageName),
                                    Toast.LENGTH_SHORT).show();
                            loadAppsAsync();
                        });
                    }

                    @Override
                    public void onScopeRequestFailed(String message) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                getString(R.string.scope_add_failed, message),
                                Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void saveViewportWidth(AppItem item, TextInputEditText inputView) {
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            boolean cleared = store.clearTargetViewportWidthDp(item.packageName);
            Toast.makeText(this,
                    cleared
                            ? getString(R.string.status_save_disabled, item.packageName)
                            : getString(R.string.status_save_requires_init),
                    Toast.LENGTH_SHORT).show();
            if (cleared) {
                loadAppsAsync();
            }
            return;
        }
        try {
            int widthDp = Integer.parseInt(raw);
            if (widthDp <= 0) {
                throw new NumberFormatException("width must be positive");
            }
            boolean saved = store.setTargetViewportWidthDp(item.packageName, widthDp);
            Toast.makeText(this,
                    saved
                            ? getString(R.string.status_save_success, item.packageName)
                            : getString(R.string.status_save_requires_init),
                    Toast.LENGTH_SHORT).show();
            if (saved) {
                loadAppsAsync();
            }
        } catch (NumberFormatException exception) {
            Toast.makeText(this, getString(R.string.status_save_invalid), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void disableViewport(AppItem item) {
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        boolean cleared = store.clearTargetViewportWidthDp(item.packageName);
        Toast.makeText(this,
                cleared
                        ? getString(R.string.status_save_disabled, item.packageName)
                        : getString(R.string.status_save_requires_init),
                Toast.LENGTH_SHORT).show();
        if (cleared) {
            loadAppsAsync();
        }
    }

    private void showEditDialog(AppItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_config, null, false);
        android.widget.ImageView iconView = dialogView.findViewById(R.id.dialog_app_icon);
        MaterialTextView titleView = dialogView.findViewById(R.id.dialog_title);
        MaterialTextView packageView = dialogView.findViewById(R.id.dialog_package);
        MaterialTextView statusView = dialogView.findViewById(R.id.dialog_status);
        TextInputEditText inputView = dialogView.findViewById(R.id.dialog_viewport_input);
        MaterialButton scopeButton = dialogView.findViewById(R.id.dialog_scope_button);
        MaterialButton disableButton = dialogView.findViewById(R.id.dialog_disable_button);
        MaterialButton saveButton = dialogView.findViewById(R.id.dialog_save_button);

        iconView.setImageDrawable(item.icon);
        titleView.setText(item.label);
        packageView.setText(item.packageName);
        statusView.setText(AppStatusFormatter.format(item.inScope, item.viewportWidthDp));
        inputView.setText(item.viewportWidthDp != null ? String.valueOf(item.viewportWidthDp) : "");
        scopeButton.setText(item.inScope
                ? getString(R.string.scope_remove_button)
                : getString(R.string.scope_add_button));

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        scopeButton.setOnClickListener(v -> {
            toggleScope(item);
            dialog.dismiss();
        });
        disableButton.setOnClickListener(v -> {
            disableViewport(item);
            dialog.dismiss();
        });
        saveButton.setOnClickListener(v -> {
            saveViewportWidth(item, inputView);
            dialog.dismiss();
        });
        dialog.show();
    }

    private final class AppListAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AppItem item = filteredApps.get(position);

            holder.label.setText(item.label);
            holder.packageName.setText(item.packageName);
            holder.icon.setImageDrawable(item.icon);
            holder.expandIndicator.setText("›");
            holder.status.setText(AppStatusFormatter.format(item.inScope, item.viewportWidthDp));
            holder.headerClickTarget.setOnClickListener(v -> showEditDialog(item));
        }

        @Override
        public int getItemCount() {
            return filteredApps.size();
        }
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        final View headerClickTarget;
        final android.widget.ImageView icon;
        final MaterialTextView label;
        final MaterialTextView packageName;
        final MaterialTextView status;
        final MaterialTextView expandIndicator;

        private ViewHolder(View root) {
            super(root);
            headerClickTarget = root.findViewById(R.id.header_click_target);
            icon = root.findViewById(R.id.app_icon);
            label = root.findViewById(R.id.app_label);
            packageName = root.findViewById(R.id.app_package);
            status = root.findViewById(R.id.app_status);
            expandIndicator = root.findViewById(R.id.expand_indicator);
        }
    }

    private static final class AppItem {
        final String label;
        final String packageName;
        final boolean inScope;
        final Integer viewportWidthDp;
        final boolean systemApp;
        final Drawable icon;

        private AppItem(String label,
                        String packageName,
                        boolean inScope,
                        Integer viewportWidthDp,
                        boolean systemApp,
                        Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.inScope = inScope;
            this.viewportWidthDp = viewportWidthDp;
            this.systemApp = systemApp;
            this.icon = icon;
        }
    }
}
