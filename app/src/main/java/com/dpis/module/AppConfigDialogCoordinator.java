package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        bindAdvancedWizardHint(dialogView, bottomSheet);
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

    private void bindAdvancedWizardHint(View dialogView, FrameLayout bottomSheet) {
        if (!AppConfigSheetWizardStore.shouldShowAdvancedHint(activity)) {
            return;
        }
        View dragHandle = dialogView.findViewById(R.id.dialog_drag_handle);
        ViewGroup overlayParent = (ViewGroup) bottomSheet.getParent();
        if (dragHandle == null) {
            return;
        }
        if (overlayParent == null) {
            return;
        }
        overlayParent.setClipChildren(false);
        overlayParent.setClipToPadding(false);
        View hint = LayoutInflater.from(activity).inflate(
                R.layout.view_app_config_wizard_hint, overlayParent, false);
        View closeButton = hint.findViewById(R.id.dialog_advanced_wizard_close_button);
        if (closeButton == null) {
            return;
        }
        hint.setClickable(true);
        overlayParent.addView(hint);
        BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
        Runnable positionHint = () -> positionAdvancedWizardHint(
                hint, dragHandle, bottomSheet, overlayParent);
        hint.post(positionHint);
        View.OnLayoutChangeListener sheetLayoutListener = (view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> positionHint.run();
        View.OnLayoutChangeListener handleLayoutListener = (view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> positionHint.run();
        BottomSheetBehavior.BottomSheetCallback sheetCallback =
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(View bottomSheetView, int newState) {
                        positionHint.run();
                    }

                    @Override
                    public void onSlide(View bottomSheetView, float slideOffset) {
                        positionHint.run();
                    }
                };
        bottomSheet.addOnLayoutChangeListener(sheetLayoutListener);
        dragHandle.addOnLayoutChangeListener(handleLayoutListener);
        behavior.addBottomSheetCallback(sheetCallback);
        closeButton.setOnClickListener(v -> {
            AppConfigSheetWizardStore.markAdvancedHintDismissed(activity);
            bottomSheet.removeOnLayoutChangeListener(sheetLayoutListener);
            dragHandle.removeOnLayoutChangeListener(handleLayoutListener);
            behavior.removeBottomSheetCallback(sheetCallback);
            overlayParent.removeView(hint);
        });
    }

    private void positionAdvancedWizardHint(View hint,
                                            View dragHandle,
                                            View bottomSheet,
                                            ViewGroup overlayParent) {
        int hintWidth = hint.getWidth();
        int hintHeight = hint.getHeight();
        int parentWidth = overlayParent.getWidth();
        if (hintWidth <= 0 || hintHeight <= 0 || parentWidth <= 0) {
            return;
        }
        int[] handlePos = new int[2];
        dragHandle.getLocationOnScreen(handlePos);
        int[] parentPos = new int[2];
        overlayParent.getLocationOnScreen(parentPos);
        int[] sheetPos = new int[2];
        bottomSheet.getLocationOnScreen(sheetPos);
        float handleCenterX = handlePos[0] - parentPos[0] + dragHandle.getWidth() / 2f;
        int sheetTop = sheetPos[1] - parentPos[1];
        int sideMargin = dp(12);
        float targetX = handleCenterX - hintWidth / 2f;
        targetX = Math.max(sideMargin, Math.min(targetX, parentWidth - hintWidth - sideMargin));
        float targetY = sheetTop - hintHeight - dp(8);
        hint.setX(targetX);
        hint.setY(targetY);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
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
