package com.dpis.module;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.widget.NestedScrollView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

final class AppConfigDialogCoordinator {
    private static final long IME_LAYOUT_SETTLE_DELAY_MS = 120L;
    private final Activity activity;
    private boolean imeOffsetApplied;
    private boolean imeVisible;
    private boolean imeAnimationRunning;
    private boolean originalFitToContents;
    private int originalExpandedOffset;
    private int originalState;
    private int originalMaxHeight;

    AppConfigDialogCoordinator(Activity activity) {
        this.activity = activity;
    }

    BottomSheetDialog show(View dialogView) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(dialogView);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setHalfExpandedRatio(0.5f);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        dialog.setOnShowListener(d -> onDialogShown(dialogView, dialog));
        dialog.show();
        refineHalfExpandedRatio(dialogView, dialog);
        return dialog;
    }

    private void onDialogShown(View dialogView, BottomSheetDialog dialog) {
        int surfaceColor = MaterialColors.getColor(
                dialogView, com.google.android.material.R.attr.colorSurface);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setNavigationBarColor(surfaceColor);
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            );
        }

        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bindAdvancedWizardHint(dialogView);
        NestedScrollView scrollView = dialogView.findViewById(
                R.id.dialog_app_config_scroll
        );
        final int baseScrollPaddingBottom = scrollView != null
                ? scrollView.getPaddingBottom()
                : 0;
        if (scrollView != null) {
            scrollView.setClipToPadding(false);
        }
        int inputVerticalPadding = activity.getResources().getDimensionPixelSize(
                R.dimen.dialog_app_config_input_row_spacing_top
        );
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (view, insets) -> {
            boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            imeVisible = keyboardVisible;
            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(view);
            int bottomInset = keyboardVisible
                    ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    : insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            applyImeScrollPadding(scrollView, baseScrollPaddingBottom, bottomInset);
            if (keyboardVisible) {
                captureImeSheetState(behavior);
                int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                dialogView.postDelayed(
                        () -> {
                            if (imeVisible && !imeAnimationRunning) {
                                scrollFocusedInputIntoView(
                                        scrollView, dialogView, view, behavior,
                                        imeBottom, inputVerticalPadding);
                            }
                        },
                        IME_LAYOUT_SETTLE_DELAY_MS
                );
            } else {
                imeAnimationRunning = false;
                view.postDelayed(() -> {
                    if (!imeVisible) {
                        restoreImeSheetOffset(behavior, view);
                    }
                }, IME_LAYOUT_SETTLE_DELAY_MS);
            }
            return insets;
        });
        ViewCompat.setWindowInsetsAnimationCallback(
                bottomSheet,
                new WindowInsetsAnimationCompat.Callback(
                        WindowInsetsAnimationCompat.Callback
                                .DISPATCH_MODE_CONTINUE_ON_SUBTREE
                ) {
                    @Override
                    public WindowInsetsCompat onProgress(
                            WindowInsetsCompat insets,
                            java.util.List<WindowInsetsAnimationCompat> runningAnimations
                    ) {
                        if (imeOffsetApplied && hasImeAnimation(runningAnimations)) {
                            imeAnimationRunning = true;
                            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(
                                    bottomSheet);
                            int imeBottom = insets.getInsets(
                                    WindowInsetsCompat.Type.ime()).bottom;
                            updateImeMaxHeight(behavior, bottomSheet, imeBottom);
                            if (imeVisible && imeBottom > 0) {
                                scrollFocusedInputIntoView(
                                        scrollView, dialogView, bottomSheet, behavior,
                                        imeBottom, inputVerticalPadding);
                            }
                        }
                        return insets;
                    }
                }
        );
    }

    private void applyImeScrollPadding(
            NestedScrollView scrollView,
            int baseBottomPadding,
            int bottomInset
    ) {
        if (scrollView == null) {
            return;
        }
        int targetPaddingBottom = baseBottomPadding + Math.max(0, bottomInset);
        if (scrollView.getPaddingBottom() != targetPaddingBottom) {
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(),
                    targetPaddingBottom
            );
        }
    }

    private void scrollFocusedInputIntoView(
            NestedScrollView scrollView,
            View dialogView,
            View bottomSheet,
            BottomSheetBehavior<?> behavior,
            int imeBottom,
            int verticalPadding
    ) {
        View focused = dialogView.findFocus();
        if (scrollView == null
                || focused == null
                || !isDescendantOf(focused, scrollView)) {
            return;
        }
        Rect rect = new Rect();
        focused.getDrawingRect(rect);
        scrollView.offsetDescendantRectToMyCoords(focused, rect);
        if (imeBottom > 0 && bottomSheet != null) {
            int[] rootPos = new int[2];
            View root = bottomSheet.getRootView();
            root.getLocationOnScreen(rootPos);
            int keyboardTop = rootPos[1] + root.getHeight() - imeBottom;
            int[] focusedPos = new int[2];
            focused.getLocationOnScreen(focusedPos);
            int focusedBottom = focusedPos[1] + focused.getHeight();
            int screenDelta = focusedBottom + verticalPadding - keyboardTop;
            if (screenDelta > 0) {
                int sheetShift = applyImeSheetOffset(
                        behavior, bottomSheet, screenDelta, verticalPadding);
                int remainingDelta = screenDelta - sheetShift;
                if (remainingDelta > 0) {
                    scrollView.smoothScrollBy(0, remainingDelta);
                }
                return;
            }
        }
        int currentScrollY = scrollView.getScrollY();
        int visibleTop = currentScrollY;
        int visibleBottom = currentScrollY + scrollView.getHeight();
        int targetScrollY = currentScrollY;
        if (rect.bottom + verticalPadding > visibleBottom) {
            targetScrollY = rect.bottom + verticalPadding - scrollView.getHeight();
        } else if (rect.top - verticalPadding < visibleTop) {
            targetScrollY = rect.top - verticalPadding;
        }
        scrollView.smoothScrollTo(0, Math.max(0, targetScrollY));
    }

    private int applyImeSheetOffset(
            BottomSheetBehavior<?> behavior,
            View bottomSheet,
            int screenDelta,
            int verticalPadding
    ) {
        View parent = bottomSheet.getParent() instanceof View
                ? (View) bottomSheet.getParent()
                : null;
        if (!imeVisible || parent == null) {
            return 0;
        }
        captureImeSheetState(behavior);

        int[] sheetPos = new int[2];
        int[] parentPos = new int[2];
        bottomSheet.getLocationOnScreen(sheetPos);
        parent.getLocationOnScreen(parentPos);
        int currentOffset = sheetPos[1] - parentPos[1];
        int targetOffset = Math.max(verticalPadding, currentOffset - screenDelta);
        int sheetShift = currentOffset - targetOffset;
        if (sheetShift <= 0) {
            return 0;
        }

        imeOffsetApplied = true;
        if (behavior.isFitToContents()) {
            behavior.setFitToContents(false);
        }
        if (behavior.getExpandedOffset() != targetOffset) {
            behavior.setExpandedOffset(targetOffset);
        }
        if (behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        return sheetShift;
    }

    private void captureImeSheetState(BottomSheetBehavior<?> behavior) {
        if (imeOffsetApplied) {
            return;
        }
        originalFitToContents = behavior.isFitToContents();
        originalExpandedOffset = behavior.getExpandedOffset();
        originalState = behavior.getState();
        originalMaxHeight = behavior.getMaxHeight();
        imeOffsetApplied = true;
    }

    private void restoreImeSheetOffset(BottomSheetBehavior<?> behavior, View bottomSheet) {
        if (!imeOffsetApplied) {
            return;
        }
        imeOffsetApplied = false;
        if (behavior.getExpandedOffset() != originalExpandedOffset) {
            behavior.setExpandedOffset(originalExpandedOffset);
        }
        if (behavior.isFitToContents() != originalFitToContents) {
            behavior.setFitToContents(originalFitToContents);
        }
        if (behavior.getState() != originalState) {
            behavior.setState(originalState);
        }
        if (behavior.getMaxHeight() != originalMaxHeight) {
            behavior.setMaxHeight(originalMaxHeight);
        }
        imeAnimationRunning = false;
        if (originalState != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.requestLayout();
        }
    }

    private void updateImeMaxHeight(
            BottomSheetBehavior<?> behavior,
            View bottomSheet,
            int imeBottom
    ) {
        View parent = bottomSheet.getParent() instanceof View
                ? (View) bottomSheet.getParent()
                : null;
        if (parent == null) {
            return;
        }
        int maxHeight = Math.max(0, parent.getHeight() - Math.max(0, imeBottom));
        if (maxHeight > 0 && behavior.getMaxHeight() != maxHeight) {
            behavior.setMaxHeight(maxHeight);
            bottomSheet.requestLayout();
        }
    }

    private boolean hasImeAnimation(
            java.util.List<WindowInsetsAnimationCompat> runningAnimations
    ) {
        for (WindowInsetsAnimationCompat animation : runningAnimations) {
            if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDescendantOf(View child, View parent) {
        View current = child;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            if (!(current.getParent() instanceof View)) {
                return false;
            }
            current = (View) current.getParent();
        }
        return false;
    }

    private void bindAdvancedWizardHint(View dialogView) {
        View hint = dialogView.findViewById(R.id.dialog_advanced_wizard_hint_container);
        if (hint == null) {
            return;
        }
        if (!AppConfigSheetWizardStore.shouldShowAdvancedHint(activity)) {
            hint.setVisibility(View.GONE);
            return;
        }
        hint.setVisibility(View.VISIBLE);
        View closeButton = hint.findViewById(R.id.dialog_advanced_wizard_close_button);
        if (closeButton == null) {
            return;
        }
        hint.setClickable(true);
        closeButton.setOnClickListener(v -> {
            AppConfigSheetWizardStore.markAdvancedHintDismissed(activity);
            hint.setVisibility(View.GONE);
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
