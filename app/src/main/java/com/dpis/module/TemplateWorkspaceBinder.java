package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

final class TemplateWorkspaceBinder {
    interface GlobalPrefillActions {
        void edit();
    }

    interface QuickTemplateActions {
        void apply(String templateId);

        void edit(String templateId);

        void select(String templateId);

        void create();

        void sort(List<QuickTemplateStore.QuickTemplate> templates);
    }

    private final Context context;
    private final SharedPreferences preferences;
    private final TemplateConfigSummaryFormatter formatter;
    private final TemplateSummaryChipBinder summaryChipBinder;
    private final QuickTemplateListAdapter quickTemplateListAdapter;
    private final GlobalPrefillActions globalPrefillActions;
    private final QuickTemplateActions quickTemplateActions;
    private static final float DISABLED_ACTION_ALPHA = 0.45f;

    TemplateWorkspaceBinder(Context context,
            GlobalPrefillActions globalPrefillActions,
            QuickTemplateActions quickTemplateActions) {
        this.context = context;
        this.globalPrefillActions = globalPrefillActions;
        this.quickTemplateActions = quickTemplateActions;
        this.preferences = context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE);
        this.formatter = new TemplateConfigSummaryFormatter(
                new ResourceSummaryText(context),
                new TemplateTypefaceResolver(() -> ConfigStoreFactory.createLocalUiFontLibraryStore(
                        context, DpisApplication.getXposedService())));
        this.summaryChipBinder = new TemplateSummaryChipBinder(context);
        this.quickTemplateListAdapter = new QuickTemplateListAdapter(
                context,
                formatter,
                summaryChipBinder,
                this::onEditTemplate,
                this::onApplyTemplate,
                this::onSelectTemplate);
    }

    void bind(View workspaceView, String query) {
        if (workspaceView == null) {
            return;
        }
        bindGlobalPrefill(workspaceView);
        LinearLayout listContainer = workspaceView.findViewById(R.id.quick_template_list_container);
        MaterialTextView emptyState = workspaceView.findViewById(R.id.quick_template_empty_state);
        List<QuickTemplateStore.QuickTemplate> templates = new QuickTemplateStore(preferences).readAll();
        bindHeaderActions(workspaceView, templates);
        String normalizedQuery = normalizeQuery(query);
        boolean searching = !normalizedQuery.isEmpty();
        setVisible(workspaceView.findViewById(R.id.global_prefill_card), !searching);
        setVisible(workspaceView.findViewById(R.id.quick_template_section_header), !searching);
        quickTemplateListAdapter.bind(
                listContainer,
                emptyState,
                searching ? filterTemplates(templates, normalizedQuery) : templates,
                searching ? context.getString(R.string.quick_template_search_empty)
                        : context.getString(R.string.template_workspace_quick_templates_empty));
    }

    private void bindGlobalPrefill(View workspaceView) {
        TemplateConfigValue value = new GlobalPrefillStore(preferences).read();
        TemplateConfigSummaryFormatter.Result result = formatter.format(value);
        ChipGroup summaryChips = workspaceView.findViewById(R.id.global_prefill_summary_chips);
        MaterialTextView emptySummaryView = workspaceView.findViewById(R.id.global_prefill_empty_summary);
        View editButton = workspaceView.findViewById(R.id.global_prefill_edit_button);
        summaryChipBinder.bind(summaryChips, emptySummaryView, result);
        TouchFeedbackBinder.bindPressHaptic(editButton);
        editButton.setOnClickListener(v -> {
            if (globalPrefillActions != null) {
                globalPrefillActions.edit();
            }
        });
    }

    private void bindHeaderActions(View workspaceView, List<QuickTemplateStore.QuickTemplate> templates) {
        View sortButton = workspaceView.findViewById(R.id.quick_template_sort_button);
        if (sortButton != null) {
            boolean sortEnabled = templates != null && !templates.isEmpty();
            sortButton.setEnabled(sortEnabled);
            sortButton.setAlpha(sortEnabled ? 1f : DISABLED_ACTION_ALPHA);
            TouchFeedbackBinder.bindPressHaptic(sortButton);
            sortButton.setOnClickListener(v -> {
                if (quickTemplateActions != null) {
                    quickTemplateActions.sort(new QuickTemplateStore(preferences).readAll());
                }
            });
        }
        View createButton = workspaceView.findViewById(R.id.quick_template_create_button);
        if (createButton == null) {
            return;
        }
        TouchFeedbackBinder.bindPressHaptic(createButton);
        createButton.setOnClickListener(v -> {
            if (quickTemplateActions != null) {
                quickTemplateActions.create();
            }
        });
    }

    private void onEditTemplate(String templateId) {
        if (quickTemplateActions != null) {
            quickTemplateActions.edit(templateId);
        }
    }

    private void onApplyTemplate(String templateId) {
        if (quickTemplateActions != null) {
            quickTemplateActions.apply(templateId);
        }
    }

    private void onSelectTemplate(String templateId) {
        if (quickTemplateActions != null) {
            quickTemplateActions.select(templateId);
        }
    }

    private void showNotWiredToast() {
        Toast.makeText(context, R.string.template_workspace_not_wired, Toast.LENGTH_SHORT).show();
    }

    private static List<QuickTemplateStore.QuickTemplate> filterTemplates(
            List<QuickTemplateStore.QuickTemplate> templates,
            String normalizedQuery) {
        ArrayList<QuickTemplateStore.QuickTemplate> filtered = new ArrayList<>();
        if (templates == null) {
            return filtered;
        }
        for (QuickTemplateStore.QuickTemplate template : templates) {
            if (template.name.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                filtered.add(template);
            }
        }
        return filtered;
    }

    private static String normalizeQuery(String query) {
        return query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private static final class ResourceSummaryText implements TemplateConfigSummaryFormatter.Text {
        private final Context context;

        ResourceSummaryText(Context context) {
            this.context = context;
        }

        @Override
        public String emptySummary() {
            return context.getString(R.string.template_workspace_summary_empty);
        }

        @Override
        public String viewportSummary(String detail) {
            return context.getString(R.string.template_workspace_summary_viewport, detail);
        }

        @Override
        public String viewportTargetTypeScale() {
            return context.getString(R.string.dialog_viewport_mode_system);
        }

        @Override
        public String viewportTargetTypeWidth() {
            return context.getString(R.string.dialog_viewport_mode_compat);
        }

        @Override
        public String fontSummary(String detail) {
            return context.getString(R.string.template_workspace_summary_font, detail);
        }

        @Override
        public String noValue() {
            return context.getString(R.string.app_status_no_value);
        }

        @Override
        public String typeface(String displayName) {
            return context.getString(R.string.template_workspace_summary_typeface, displayName);
        }

        @Override
        public String hookDomains() {
            return context.getString(R.string.template_workspace_summary_hook_domains);
        }

        @Override
        public String modeAuto() {
            return context.getString(R.string.template_workspace_mode_auto);
        }

        @Override
        public String modeSystem() {
            return context.getString(R.string.template_workspace_mode_system);
        }

        @Override
        public String modeCompat() {
            return context.getString(R.string.template_workspace_mode_compat);
        }
    }
}
