package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

final class TemplateWorkspaceBinder {
    interface GlobalPrefillActions {
        void edit();

        void reset();
    }

    interface QuickTemplateActions {
        void edit(String templateId);

        void select(String templateId);

        void create();
    }

    private final Context context;
    private final SharedPreferences preferences;
    private final TemplateConfigSummaryFormatter formatter;
    private final QuickTemplateListAdapter quickTemplateListAdapter;
    private final GlobalPrefillActions globalPrefillActions;
    private final QuickTemplateActions quickTemplateActions;

    TemplateWorkspaceBinder(Context context,
            GlobalPrefillActions globalPrefillActions,
            QuickTemplateActions quickTemplateActions) {
        this.context = context;
        this.globalPrefillActions = globalPrefillActions;
        this.quickTemplateActions = quickTemplateActions;
        this.preferences = context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
        this.formatter = new TemplateConfigSummaryFormatter(
                new ResourceSummaryText(context),
                new TemplateTypefaceResolver(() -> ConfigStoreFactory.createFontLibraryForModuleApp(
                        context, DpisApplication.getXposedService())));
        this.quickTemplateListAdapter = new QuickTemplateListAdapter(
                context,
                formatter,
                this::formatUpdatedTime,
                this::onEditTemplate,
                this::onApplyTemplate,
                this::onSelectTemplate);
    }

    void bind(View workspaceView) {
        if (workspaceView == null) {
            return;
        }
        bindGlobalPrefill(workspaceView);
        bindCreateTemplateButton(workspaceView);
        LinearLayout listContainer = workspaceView.findViewById(R.id.quick_template_list_container);
        MaterialTextView emptyState = workspaceView.findViewById(R.id.quick_template_empty_state);
        List<QuickTemplateStore.QuickTemplate> templates =
                new QuickTemplateStore(preferences).readAll();
        quickTemplateListAdapter.bind(listContainer, emptyState, templates);
    }

    private void bindGlobalPrefill(View workspaceView) {
        TemplateConfigValue value = new GlobalPrefillStore(preferences).read();
        TemplateConfigSummaryFormatter.Result result = formatter.format(value);
        MaterialTextView summaryView = workspaceView.findViewById(R.id.global_prefill_summary);
        MaterialTextView missingFontView = workspaceView.findViewById(R.id.global_prefill_missing_font);
        MaterialButton editButton = workspaceView.findViewById(R.id.global_prefill_edit_button);
        MaterialButton resetButton = workspaceView.findViewById(R.id.global_prefill_reset_button);
        summaryView.setText(result.summary());
        bindMissingFont(missingFontView, result.typefaceStatus);
        editButton.setOnClickListener(v -> {
            if (globalPrefillActions != null) {
                globalPrefillActions.edit();
            }
        });
        resetButton.setOnClickListener(v -> {
            if (globalPrefillActions != null) {
                globalPrefillActions.reset();
            }
        });
    }

    private void bindCreateTemplateButton(View workspaceView) {
        MaterialButton createButton = workspaceView.findViewById(R.id.quick_template_create_button);
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
        showNotWiredToast();
    }

    private void onSelectTemplate(String templateId) {
        if (quickTemplateActions != null) {
            quickTemplateActions.select(templateId);
        }
    }

    private void bindMissingFont(MaterialTextView view,
            TemplateConfigSummaryFormatter.TypefaceStatus typefaceStatus) {
        if (typefaceStatus != null && typefaceStatus.missing) {
            view.setVisibility(View.VISIBLE);
            view.setText(context.getString(
                    R.string.template_workspace_missing_font,
                    typefaceStatus.typefaceId));
            return;
        }
        view.setVisibility(View.GONE);
        view.setText("");
    }

    private String formatUpdatedTime(long updatedAt) {
        if (updatedAt <= 0L) {
            return context.getString(R.string.template_workspace_updated_unknown);
        }
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return context.getString(R.string.template_workspace_updated_time,
                dateFormat.format(new Date(updatedAt)));
    }

    private void showNotWiredToast() {
        Toast.makeText(context, R.string.template_workspace_not_wired, Toast.LENGTH_SHORT).show();
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
        public String viewportScale(int wholePercent, int decimalPercent) {
            return context.getString(
                    R.string.template_workspace_summary_viewport_scale,
                    wholePercent,
                    decimalPercent);
        }

        @Override
        public String viewportWidth(int widthDp) {
            return context.getString(R.string.template_workspace_summary_viewport_width, widthDp);
        }

        @Override
        public String viewportMode(String modeLabel) {
            return context.getString(R.string.template_workspace_summary_viewport_mode, modeLabel);
        }

        @Override
        public String fontScale(int percent) {
            return context.getString(R.string.template_workspace_summary_font_scale, percent);
        }

        @Override
        public String fontMode(String modeLabel) {
            return context.getString(R.string.template_workspace_summary_font_mode, modeLabel);
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
