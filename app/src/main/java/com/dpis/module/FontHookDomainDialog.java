package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
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
    }

    private FontHookDomainDialog() {
    }

    static void show(Activity activity,
                     Host host,
                     String packageName,
                     Set<String> automaticKnownDomains,
                     HookDomainOverride currentOverride,
                     Runnable onStateChanged) {
        View view = LayoutInflater.from(activity).inflate(
                R.layout.dialog_font_hook_domains, null, false);
        LinearLayout knownContainer = view.findViewById(R.id.font_hook_domains_known_container);
        MaterialTextView unknownTitle = view.findViewById(R.id.font_hook_domains_unknown_title);
        LinearLayout unknownContainer = view.findViewById(R.id.font_hook_domains_unknown_container);
        View restoreButton = view.findViewById(R.id.font_hook_domains_restore_button);

        LinkedHashSet<String> knownIds = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableIdsList());
        LinkedHashSet<String> automaticKnown = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableSubset(automaticKnownDomains));
        LinkedHashSet<String> selectedKnown = new LinkedHashSet<>(
                currentOverride != null && currentOverride.customPathEnabled
                        ? FontHookDomainRegistry.orderedCustomizableSubset(
                                currentOverride.enabledKnownDomains)
                        : automaticKnown);
        LinkedHashSet<String> unknown = new LinkedHashSet<>(
                currentOverride != null ? currentOverride.unknownDomains : Set.of());

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
        subtitle.setText(domainId);
        return row;
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
}
