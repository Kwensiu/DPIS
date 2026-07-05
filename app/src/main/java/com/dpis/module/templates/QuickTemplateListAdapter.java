package com.dpis.module.templates;

import com.dpis.module.R;
import com.dpis.module.ui.TouchFeedbackBinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public final class QuickTemplateListAdapter {
    public interface TemplateAction {
        void run(String templateId);
    }

    private final Context context;
    private final TemplateConfigSummaryFormatter formatter;
    private final TemplateSummaryChipBinder summaryChipBinder;
    private final TemplateAction editAction;
    private final TemplateAction applyAction;
    private final TemplateAction selectAction;

    public QuickTemplateListAdapter(Context context,
            TemplateConfigSummaryFormatter formatter,
            TemplateSummaryChipBinder summaryChipBinder,
            TemplateAction editAction,
            TemplateAction applyAction,
            TemplateAction selectAction) {
        this.context = context;
        this.formatter = formatter;
        this.summaryChipBinder = summaryChipBinder;
        this.editAction = editAction;
        this.applyAction = applyAction;
        this.selectAction = selectAction;
    }

    public void bind(LinearLayout listContainer,
            MaterialTextView emptyState,
            List<QuickTemplateStore.QuickTemplate> templates) {
        bind(listContainer, emptyState, templates,
                context.getString(R.string.template_workspace_quick_templates_empty));
    }

    public void bind(LinearLayout listContainer,
            MaterialTextView emptyState,
            List<QuickTemplateStore.QuickTemplate> templates,
            String emptyText) {
        if (listContainer == null || emptyState == null) {
            return;
        }
        listContainer.removeAllViews();
        boolean empty = templates == null || templates.isEmpty();
        emptyState.setText(emptyText);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        for (QuickTemplateStore.QuickTemplate template : templates) {
            View card = inflater.inflate(R.layout.item_quick_template_card, listContainer, false);
            bindCard(card, template);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (listContainer.getChildCount() > 0) {
                params.topMargin = context.getResources().getDimensionPixelSize(
                        R.dimen.template_workspace_card_spacing_top);
            }
            listContainer.addView(card, params);
        }
    }

    private void bindCard(View card, QuickTemplateStore.QuickTemplate template) {
        TemplateConfigSummaryFormatter.Result result = formatter.format(template.configValue);
        MaterialTextView titleView = card.findViewById(R.id.quick_template_title);
        ChipGroup summaryChips = card.findViewById(R.id.quick_template_summary_chips);
        MaterialTextView emptySummaryView = card.findViewById(R.id.quick_template_empty_summary);
        titleView.setText(template.name);
        summaryChipBinder.bind(summaryChips, emptySummaryView, result);
        bindAction(card.findViewById(R.id.quick_template_apply_button), template.id, applyAction);
        bindAction(card.findViewById(R.id.quick_template_edit_button), template.id, editAction);
        bindAction(card.findViewById(R.id.quick_template_select_button), template.id, selectAction);
    }

    private void bindAction(View button, String templateId, TemplateAction action) {
        TouchFeedbackBinder.bindPressHaptic(button);
        button.setOnClickListener(v -> {
            if (action != null) {
                action.run(templateId);
            }
        });
    }
}
