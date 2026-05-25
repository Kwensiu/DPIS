package com.dpis.module;

import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class FontHookDomainDialog {
    interface Host {
        boolean saveCustom(String packageName,
                           Set<String> selectedKnownDomains,
                           Set<String> automaticKnownDomains,
                           Set<String> unknownDomains);

        boolean restoreRecommended(String packageName);

        boolean saveViewportApplyMode(String packageName, String mode);
    }

    private FontHookDomainDialog() {
    }

    static void show(Activity activity,
                     Host host,
                     String packageName,
                     Set<String> automaticKnownDomains,
                     HookDomainOverride currentOverride,
                     String currentViewportApplyMode,
                     Runnable onStateChanged) {
        View view = LayoutInflater.from(activity).inflate(
                R.layout.dialog_font_hook_domains, null, false);
        TabLayout tabs = view.findViewById(R.id.font_hook_domains_tabs);
        View interfacePage = view.findViewById(R.id.font_hook_domains_interface_page);
        View fontPage = view.findViewById(R.id.font_hook_domains_font_page);
        LinearLayout knownContainer = view.findViewById(R.id.font_hook_domains_known_container);
        LinearLayout viewportApplyContainer =
                view.findViewById(R.id.font_hook_domains_viewport_apply_container);
        MaterialTextView unknownTitle = view.findViewById(R.id.font_hook_domains_unknown_title);
        LinearLayout unknownContainer = view.findViewById(R.id.font_hook_domains_unknown_container);
        View restoreButton = view.findViewById(R.id.font_hook_domains_restore_button);

        LinkedHashSet<String> knownIds = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableDisplayIdsList());
        LinkedHashSet<String> automaticKnown = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableDisplaySubset(automaticKnownDomains));
        LinkedHashSet<String> selectedKnown = new LinkedHashSet<>(
                currentOverride != null && currentOverride.customPathEnabled
                        ? FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                                currentOverride.enabledKnownDomains)
                        : automaticKnown);
        LinkedHashSet<String> unknown = new LinkedHashSet<>(
                currentOverride != null ? currentOverride.unknownDomains : Set.of());
        String[] viewportApplyMode = new String[] {
                normalizeViewportApplyModeForDisplay(currentViewportApplyMode)
        };

        bindTabs(tabs, interfacePage, fontPage);
        bindViewportApplyRows(activity, viewportApplyContainer, host, packageName,
                viewportApplyMode);
        Map<String, MaterialSwitch> switches = new LinkedHashMap<>();
        Map<String, LinearLayout> groupContainers = createKnownGroups(activity, knownContainer);
        boolean[] binding = new boolean[] { false };
        for (String id : knownIds) {
            View row = createDomainRow(activity, id, true);
            MaterialSwitch switchView = row.findViewById(R.id.font_hook_domain_switch);
            switchView.setChecked(selectedKnown.contains(id));
            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (binding[0]) {
                    return;
                }
                updateSelectedKnown(selectedKnown, id, isChecked);
                if (host.saveCustom(packageName, selectedKnown, automaticKnown, unknown)
                        && onStateChanged != null) {
                    onStateChanged.run();
                }
            });
            row.setOnClickListener(v -> switchView.toggle());
            switches.put(id, switchView);
            LinearLayout groupContainer = groupContainers.get(FontHookDomainRegistry.groupFor(id));
            if (groupContainer != null) {
                groupContainer.addView(row);
            }
        }

        bindUnknownRows(activity, unknownTitle, unknownContainer, unknown);

        restoreButton.setOnClickListener(v -> {
            if (!host.restoreRecommended(packageName)) {
                return;
            }
            selectedKnown.clear();
            selectedKnown.addAll(automaticKnown);
            unknown.clear();
            binding[0] = true;
            for (String id : knownIds) {
                MaterialSwitch switchView = switches.get(id);
                if (switchView != null) {
                    switchView.setChecked(selectedKnown.contains(id));
                }
            }
            binding[0] = false;
            bindUnknownRows(activity, unknownTitle, unknownContainer, unknown);
            if (onStateChanged != null) {
                onStateChanged.run();
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_font_hook_domains_dialog_title)
                .setView(view)
                .create();
        dialog.show();
    }

    private static void bindTabs(TabLayout tabs, View interfacePage, View fontPage) {
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
    }

    private static void bindViewportApplyRows(Activity activity,
                                              LinearLayout container,
                                              Host host,
                                              String packageName,
                                              String[] selectedModeRef) {
        if (container == null) {
            return;
        }
        container.removeAllViews();
        String normalizedSelected = ViewportApplyMode.normalize(selectedModeRef[0]);
        addViewportApplyRow(activity, container, host, packageName,
                ViewportApplyMode.AUTO,
                R.string.dialog_viewport_apply_auto,
                R.string.dialog_viewport_apply_auto_subtitle,
                selectedModeRef);
        addViewportApplyRow(activity, container, host, packageName,
                ViewportApplyMode.SYSTEM,
                R.string.dialog_viewport_apply_system,
                R.string.dialog_viewport_apply_system_subtitle,
                selectedModeRef);
        addViewportApplyRow(activity, container, host, packageName,
                ViewportApplyMode.COMPAT,
                R.string.dialog_viewport_apply_compat,
                R.string.dialog_viewport_apply_compat_subtitle,
                selectedModeRef);
    }

    private static void addViewportApplyRow(Activity activity,
                                            LinearLayout container,
                                            Host host,
                                            String packageName,
                                            String mode,
                                            int titleRes,
                                            int subtitleRes,
                                            String[] selectedModeRef) {
        String selectedMode = ViewportApplyMode.normalize(selectedModeRef[0]);
        View row = LayoutInflater.from(activity).inflate(
                R.layout.item_viewport_apply_mode, container, false);
        MaterialTextView title = row.findViewById(R.id.viewport_apply_mode_title);
        MaterialTextView subtitle = row.findViewById(R.id.viewport_apply_mode_subtitle);
        MaterialRadioButton radioButton = row.findViewById(R.id.viewport_apply_mode_radio);
        title.setText(titleRes);
        subtitle.setText(subtitleRes);
        radioButton.setChecked(mode.equals(selectedMode));
        View.OnClickListener listener = v -> {
            if (mode.equals(ViewportApplyMode.normalize(selectedModeRef[0]))) {
                return;
            }
            if (host.saveViewportApplyMode(packageName, mode)) {
                selectedModeRef[0] = mode;
                bindViewportApplyRows(activity, container, host, packageName, selectedModeRef);
            }
        };
        row.setOnClickListener(listener);
        radioButton.setOnClickListener(listener);
        container.addView(row);
    }

    private static String normalizeViewportApplyModeForDisplay(String mode) {
        String normalized = ViewportApplyMode.normalize(mode);
        return ViewportApplyMode.isEnabled(normalized)
                ? normalized
                : ViewportApplyMode.AUTO;
    }

    private static void updateSelectedKnown(Set<String> selectedKnown, String id, boolean checked) {
        if (checked) {
            selectedKnown.add(id);
            return;
        }
        selectedKnown.remove(id);
    }

    private static Map<String, LinearLayout> createKnownGroups(Activity activity,
                                                               LinearLayout knownContainer) {
        Map<String, LinearLayout> containers = new LinkedHashMap<>();
        List<String> groups = FontHookDomainRegistry.orderedGroups();
        for (int index = 0; index < groups.size(); index++) {
            String group = groups.get(index);
            MaterialTextView title = createGroupTitle(activity, group);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = index == 0 ? 0 : dp(activity, 16);
            knownContainer.addView(title, titleParams);

            LinearLayout groupRows = new LinearLayout(activity);
            groupRows.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowsParams.topMargin = dp(activity, 4);
            knownContainer.addView(groupRows, rowsParams);
            containers.put(group, groupRows);
        }
        return containers;
    }

    private static MaterialTextView createGroupTitle(Activity activity, String group) {
        MaterialTextView title = new MaterialTextView(activity);
        title.setText(resolveGroupTitleRes(group));
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        title.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                title, com.google.android.material.R.attr.colorOnSurfaceVariant));
        title.setIncludeFontPadding(false);
        return title;
    }

    private static void bindUnknownRows(Activity activity,
                                        MaterialTextView unknownTitle,
                                        LinearLayout unknownContainer,
                                        Set<String> unknown) {
        unknownContainer.removeAllViews();
        if (unknown == null || unknown.isEmpty()) {
            unknownTitle.setVisibility(View.GONE);
            unknownContainer.setVisibility(View.GONE);
            return;
        }
        unknownTitle.setVisibility(View.VISIBLE);
        unknownContainer.setVisibility(View.VISIBLE);
        for (String id : unknown) {
            View row = createDomainRow(activity, id, false);
            row.setEnabled(false);
            row.setAlpha(0.58f);
            MaterialSwitch switchView = row.findViewById(R.id.font_hook_domain_switch);
            switchView.setChecked(true);
            switchView.setEnabled(false);
            unknownContainer.addView(row);
        }
    }

    private static View createDomainRow(Activity activity, String domainId, boolean known) {
        View row = LayoutInflater.from(activity).inflate(
                R.layout.item_font_hook_domain, null, false);
        MaterialTextView title = row.findViewById(R.id.font_hook_domain_title);
        MaterialTextView subtitle = row.findViewById(R.id.font_hook_domain_subtitle);
        if (known) {
            title.setText(resolveDomainTitleRes(domainId));
        } else {
            title.setText(domainId);
        }
        subtitle.setText(known
                ? createSubtitleText(activity, domainId)
                : domainId);
        return row;
    }

    private static CharSequence createSubtitleText(Activity activity, String domainId) {
        int colorRes = resolveRiskDotColorRes(domainId);
        if (colorRes == 0) {
            return domainId;
        }
        SpannableString subtitle = new SpannableString("\u25CF " + domainId);
        subtitle.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(activity, colorRes)),
                0,
                1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        subtitle.setSpan(
                new RelativeSizeSpan(0.72f),
                0,
                1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return subtitle;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static int resolveGroupTitleRes(String group) {
        return switch (group) {
            case FontHookDomainRegistry.GROUP_RESOURCES ->
                    R.string.dialog_font_hook_group_resources;
            case FontHookDomainRegistry.GROUP_TEXT_VIEW_FALLBACK ->
                    R.string.dialog_font_hook_group_text_view_fallback;
            case FontHookDomainRegistry.GROUP_WEB ->
                    R.string.dialog_font_hook_group_web;
            case FontHookDomainRegistry.GROUP_CROSS_RUNTIME ->
                    R.string.dialog_font_hook_group_cross_runtime;
            default -> throw new IllegalArgumentException("Unknown domain group: " + group);
        };
    }

    private static int resolveDomainTitleRes(String domainId) {
        return switch (domainId) {
            case FontHookDomainRegistry.ID_RESOURCES_FONT ->
                    R.string.dialog_font_hook_domain_resources_font;
            case FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT ->
                    R.string.dialog_font_hook_domain_activity_thread_font;
            case FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE ->
                    R.string.dialog_font_hook_domain_textview_sp_rewrite;
            case FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE ->
                    R.string.dialog_font_hook_domain_textview_absolute_rewrite;
            case FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK ->
                    R.string.dialog_font_hook_domain_textview_current_px_fallback;
            case FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK ->
                    R.string.dialog_font_hook_domain_paint_text_size_fallback;
            case FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM ->
                    R.string.dialog_font_hook_domain_webview_text_zoom;
            case FontHookDomainRegistry.ID_FLUTTER_SETTINGS ->
                    R.string.dialog_font_hook_domain_flutter_settings;
            case FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER ->
                    R.string.dialog_font_hook_domain_hyperos_native_flutter;
            default -> throw new IllegalArgumentException("Unknown domain id: " + domainId);
        };
    }

    private static int resolveRiskDotColorRes(String domainId) {
        return switch (domainId) {
            case FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE,
                    FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE ->
                    R.color.font_hook_domain_risk_low;
            case FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK ->
                    R.color.font_hook_domain_risk_medium;
            case FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK ->
                    R.color.font_hook_domain_risk_high;
            default -> 0;
        };
    }
}
