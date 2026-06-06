package com.dpis.module;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class QuickTemplateTargetSelectionActivity extends LocalizedActivity {
    static final String EXTRA_TEMPLATE_ID = "quick_template_targets.template_id";

    private QuickTemplateStore quickTemplateStore;
    private DpiConfigStore configStore;
    private QuickTemplateStore.QuickTemplate template;
    private QuickTemplateTargetAdapter adapter;
    private MaterialTextView subtitleView;
    private MaterialTextView emptyView;
    private TextInputEditText searchInput;
    private MaterialButton saveButton;
    private View toolbar;

    private final ArrayList<TargetAppItem> allItems = new ArrayList<>();
    private final LinkedHashSet<String> selectedPackages = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_template_targets);
        SharedPreferences preferences = getSharedPreferences(DpiConfigStore.GROUP, MODE_PRIVATE);
        quickTemplateStore = new QuickTemplateStore(preferences);
        configStore = getHookConfigStore();
        String templateId = getIntent() != null
                ? getIntent().getStringExtra(EXTRA_TEMPLATE_ID)
                : null;
        template = quickTemplateStore.read(templateId);
        if (template == null) {
            showToast(R.string.quick_template_target_missing);
            finish();
            return;
        }
        selectedPackages.addAll(template.selectedPackages);
        bindViews();
        bindToolbar();
        applyInsets();
        bindList();
        loadApps();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.quick_template_targets_toolbar);
        subtitleView = findViewById(R.id.quick_template_targets_subtitle);
        emptyView = findViewById(R.id.quick_template_targets_empty);
        searchInput = findViewById(R.id.quick_template_targets_search_input);
        saveButton = findViewById(R.id.quick_template_targets_save_button);
        MaterialTextView titleView = findViewById(R.id.quick_template_targets_title);
        titleView.setText(getString(R.string.quick_template_targets_title, template.name));
        subtitleView.setText(getString(
                R.string.quick_template_targets_selected_count,
                selectedPackages.size()));
        TouchFeedbackBinder.bindPressHaptic(saveButton);
        saveButton.setOnClickListener(v -> saveSelection());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.quick_template_targets_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void applyInsets() {
        final int baseTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        RecyclerView list = findViewById(R.id.quick_template_targets_list);
        final int baseBottomPadding = list.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(list, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottomPadding + navigationBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
        ViewCompat.requestApplyInsets(list);
    }

    private void bindList() {
        RecyclerView list = findViewById(R.id.quick_template_targets_list);
        adapter = new QuickTemplateTargetAdapter(selectedPackages, this::onSelectionChanged);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    private void loadApps() {
        ArrayList<TargetAppItem> loaded = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        for (ApplicationInfo applicationInfo : getInstalledApplications(packageManager)) {
            if (getPackageName().equals(applicationInfo.packageName)) {
                continue;
            }
            String label = packageManager.getApplicationLabel(applicationInfo).toString();
            loaded.add(new TargetAppItem(
                    label,
                    applicationInfo.packageName,
                    configStore.hasRealPackageConfig(applicationInfo.packageName)));
        }
        loaded.sort(Comparator
                .comparing((TargetAppItem item) -> item.label.toLowerCase(Locale.ROOT))
                .thenComparing(item -> item.packageName));
        allItems.clear();
        allItems.addAll(loaded);
        pruneSelectedPackagesToInstalledApps(selectedPackages, allItems);
        refreshSelectedCount();
        filterApps();
    }

    private List<ApplicationInfo> getInstalledApplications(PackageManager packageManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA));
        }
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    private void filterApps() {
        String query = textOf(searchInput).trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            adapter.submit(allItems);
            emptyView.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        ArrayList<TargetAppItem> filtered = new ArrayList<>();
        for (TargetAppItem item : allItems) {
            if (item.label.toLowerCase(Locale.ROOT).contains(query)
                    || item.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(item);
            }
        }
        adapter.submit(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onSelectionChanged(String packageName, boolean selected) {
        if (selected) {
            selectedPackages.add(packageName);
        } else {
            selectedPackages.remove(packageName);
        }
        refreshSelectedCount();
    }

    private void refreshSelectedCount() {
        subtitleView.setText(getString(
                R.string.quick_template_targets_selected_count,
                selectedPackages.size()));
    }

    private void saveSelection() {
        if (quickTemplateStore.setSelectedPackages(template.id, selectedPackages)) {
            showToast(R.string.quick_template_targets_save_success);
            finish();
            return;
        }
        showToast(R.string.quick_template_targets_save_failed);
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private DpiConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
    }

    private static String textOf(TextInputEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    static LinkedHashSet<String> pruneSelectedPackagesToInstalledApps(
            Set<String> selectedPackages,
            List<TargetAppItem> installedItems) {
        LinkedHashSet<String> installedPackages = new LinkedHashSet<>();
        if (installedItems != null) {
            for (TargetAppItem item : installedItems) {
                if (item != null && item.packageName != null && !item.packageName.isBlank()) {
                    installedPackages.add(item.packageName.trim());
                }
            }
        }
        LinkedHashSet<String> pruned = new LinkedHashSet<>();
        if (selectedPackages != null) {
            for (String packageName : selectedPackages) {
                if (packageName == null) {
                    continue;
                }
                String trimmed = packageName.trim();
                if (installedPackages.contains(trimmed)) {
                    pruned.add(trimmed);
                }
            }
            selectedPackages.clear();
            selectedPackages.addAll(pruned);
        }
        return pruned;
    }

    static final class TargetAppItem {
        final String label;
        final String packageName;
        final boolean configured;

        TargetAppItem(String label, String packageName, boolean configured) {
            this.label = label != null ? label : packageName;
            this.packageName = packageName;
            this.configured = configured;
        }
    }
}
