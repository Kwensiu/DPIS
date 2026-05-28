package com.dpis.module;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

final class QuickTemplateListAdapter {
    interface UpdatedTimeFormatter {
        String format(long updatedAt);
    }

    private final Context context;
    private final TemplateConfigSummaryFormatter formatter;
    private final UpdatedTimeFormatter updatedTimeFormatter;
    private final Runnable placeholderAction;

    QuickTemplateListAdapter(Context context,
            TemplateConfigSummaryFormatter formatter,
            UpdatedTimeFormatter updatedTimeFormatter,
            Runnable placeholderAction) {
        this.context = context;
        this.formatter = formatter;
        this.updatedTimeFormatter = updatedTimeFormatter;
        this.placeholderAction = placeholderAction;
    }

    void bind(LinearLayout listContainer,
            MaterialTextView emptyState,
            List<QuickTemplateStore.QuickTemplate> templates) {
        if (listContainer == null || emptyState == null) {
            return;
        }
        listContainer.removeAllViews();
        boolean empty = templates == null || templates.isEmpty();
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
        MaterialTextView summaryView = card.findViewById(R.id.quick_template_summary);
        MaterialTextView updatedView = card.findViewById(R.id.quick_template_updated);
        MaterialTextView missingFontView = card.findViewById(R.id.quick_template_missing_font);
        titleView.setText(template.name);
        summaryView.setText(result.summary());
        updatedView.setText(updatedTimeFormatter.format(template.updatedAt));
        bindMissingFont(missingFontView, result.typefaceStatus);
        bindPlaceholderAction(card.findViewById(R.id.quick_template_apply_button));
        bindPlaceholderAction(card.findViewById(R.id.quick_template_edit_button));
        bindPlaceholderAction(card.findViewById(R.id.quick_template_select_button));
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

    private void bindPlaceholderAction(MaterialButton button) {
        button.setOnClickListener(v -> {
            if (placeholderAction != null) {
                placeholderAction.run();
            }
        });
    }
}
