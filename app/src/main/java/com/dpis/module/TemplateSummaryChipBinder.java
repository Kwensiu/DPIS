package com.dpis.module;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

final class TemplateSummaryChipBinder {
    private final Context context;

    TemplateSummaryChipBinder(Context context) {
        this.context = context;
    }

    void bind(ChipGroup chipGroup,
            MaterialTextView emptyView,
            TemplateConfigSummaryFormatter.Result result) {
        if (chipGroup == null || emptyView == null || result == null) {
            return;
        }
        chipGroup.removeAllViews();
        boolean empty = result.summaryParts.isEmpty() && !result.typefaceStatus.missing;
        chipGroup.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            emptyView.setText(result.summary());
            return;
        }
        for (int i = 0; i < result.summaryParts.size(); i++) {
            chipGroup.addView(createChip(result.summaryParts.get(i), i == 0, false));
        }
        if (result.typefaceStatus.missing) {
            chipGroup.addView(createChip(context.getString(
                    R.string.template_workspace_missing_font,
                    result.typefaceStatus.typefaceId), false, true));
        }
    }

    private Chip createChip(String text, boolean primary, boolean warning) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setFocusable(false);
        chip.setMinHeight(resourcePx(R.dimen.template_workspace_chip_min_height));
        chip.setMinWidth(0);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTextAppearanceResource(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
        chip.setChipStartPadding(resourcePx(R.dimen.template_workspace_chip_padding_horizontal));
        chip.setChipEndPadding(resourcePx(R.dimen.template_workspace_chip_padding_horizontal));
        chip.setTextStartPadding(0);
        chip.setTextEndPadding(0);
        chip.setChipCornerRadius(resourcePx(R.dimen.template_workspace_chip_corner_radius));
        chip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor(primary, warning)));
        chip.setChipStrokeWidth(0);
        chip.setTextColor(textColor(primary, warning));
        return chip;
    }

    private int backgroundColor(boolean primary, boolean warning) {
        if (warning) {
            return color(R.color.dpis_warn_container);
        }
        return attrColor(primary
                ? com.google.android.material.R.attr.colorPrimaryContainer
                : com.google.android.material.R.attr.colorSurfaceContainerHighest);
    }

    private int textColor(boolean primary, boolean warning) {
        if (warning) {
            return color(R.color.dpis_on_warn_container);
        }
        return attrColor(primary
                ? com.google.android.material.R.attr.colorOnPrimaryContainer
                : com.google.android.material.R.attr.colorOnSurfaceVariant);
    }

    private int attrColor(int attr) {
        return MaterialColors.getColor(context, attr, TemplateSummaryChipBinder.class.getSimpleName());
    }

    private int color(int resId) {
        return context.getColor(resId);
    }

    private int resourcePx(int resId) {
        return context.getResources().getDimensionPixelSize(resId);
    }
}
