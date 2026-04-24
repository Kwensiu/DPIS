package com.dpis.module;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

final class AppConfigDialogCoordinator {
    private final Activity activity;

    AppConfigDialogCoordinator(Activity activity) {
        this.activity = activity;
    }

    void show(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(dialogView);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setHalfExpandedRatio(0.5f);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        dialog.setOnShowListener(d -> onDialogShown(dialogView, dialog));
        dialog.show();
        refineHalfExpandedRatio(dialogView, dialog);
    }

    private void onDialogShown(View dialogView, BottomSheetDialog dialog) {
        int surfaceColor = MaterialColors.getColor(
                dialogView, com.google.android.material.R.attr.colorSurface);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(surfaceColor);
        }

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (view, insets) -> {
            boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(view);
            if (keyboardVisible) {
                if (behavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            } else {
                if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                }
            }
            return insets;
        });
    }

    private void refineHalfExpandedRatio(View dialogView, BottomSheetDialog dialog) {
        View expandAnchor = dialogView.findViewById(R.id.dialog_expand_anchor);
        if (expandAnchor == null) {
            return;
        }
        expandAnchor.post(() -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }
            View parent = (View) bottomSheet.getParent();
            int parentHeight = parent.getHeight();
            if (parentHeight <= 0) {
                return;
            }

            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
            int[] anchorPos = new int[2];
            expandAnchor.getLocationOnScreen(anchorPos);
            int anchorBottom = anchorPos[1] + expandAnchor.getHeight();
            int[] sheetPos = new int[2];
            bottomSheet.getLocationOnScreen(sheetPos);
            int halfExpandedDownOffsetPx = activity.getResources().getDimensionPixelSize(
                    R.dimen.dialog_app_config_half_expanded_down_offset);
            float ratio = (float) (anchorBottom - sheetPos[1] - halfExpandedDownOffsetPx)
                    / parentHeight;
            int contentHeight = dialogView.getHeight();
            float maxRatio = (float) contentHeight / parentHeight - 0.05f;
            ratio = Math.min(ratio, maxRatio);
            ratio = Math.min(Math.max(ratio, 0.3f), 0.75f);
            behavior.setHalfExpandedRatio(ratio);
        });
    }
}
