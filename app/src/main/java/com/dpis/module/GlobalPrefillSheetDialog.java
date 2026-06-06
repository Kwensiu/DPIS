package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

final class GlobalPrefillSheetDialog {

    static void show(Activity activity) {
        show(activity, null);
    }

    static void show(Activity activity, Runnable onUpdated) {
        show(activity, onUpdated, null);
    }

    static void show(
            Activity activity,
            Runnable onUpdated,
            Runnable onDismissed
    ) {
        new GlobalPrefillSheetDialog(activity, onUpdated, onDismissed).show();
    }

    private final Activity activity;
    private final BottomSheetDialog dialog;
    private final View root;
    private final Runnable onDismissed;
    private final GlobalPrefillEditorBinder binder;
    private final boolean ready;

    GlobalPrefillSheetDialog(
            Activity activity,
            Runnable onUpdated,
            Runnable onDismissed,
            GlobalPrefillEditorBinder.Draft initialDraft
    ) {
        this.activity = activity;
        this.onDismissed = onDismissed;
        dialog = new BottomSheetDialog(activity);
        root = LayoutInflater.from(activity).inflate(
                R.layout.dialog_global_prefill_sheet,
                null,
                false
        );
        dialog.setContentView(root);
        applySheetInsets(root);
        binder = GlobalPrefillEditorBinder.bind(
                activity,
                root,
                onUpdated,
                dialog::dismiss,
                true,
                initialDraft
        );
        ready = binder != null;
    }

    GlobalPrefillSheetDialog(
            Activity activity,
            Runnable onUpdated,
            Runnable onDismissed
    ) {
        this(activity, onUpdated, onDismissed, null);
    }

    void show() {
        if (!ready) {
            return;
        }
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.setOnShowListener(unused -> {
            if (dialog.getWindow() != null) {
                int surfaceColor = MaterialColors.getColor(
                        dialog.getWindow().getDecorView(),
                        com.google.android.material.R.attr.colorSurface
                );
                dialog.getWindow().setNavigationBarColor(surfaceColor);
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                );
            }
            applyWrapContentSheetHeight();
        });
        dialog.setOnDismissListener(unused -> {
            if (onDismissed != null && !activity.isChangingConfigurations()) {
                onDismissed.run();
            }
        });
        dialog.show();
    }

    boolean isShowing() {
        return dialog.isShowing();
    }

    void dismiss() {
        dialog.dismiss();
    }

    GlobalPrefillEditorBinder.Draft snapshotDraft() {
        return binder != null ? binder.snapshotDraft() : null;
    }

    private void applyWrapContentSheetHeight() {
        FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
        );
        if (bottomSheet == null) {
            return;
        }
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        if (params != null
                && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            bottomSheet.setLayoutParams(params);
        }
        BottomSheetBehavior.from(bottomSheet).setState(
                BottomSheetBehavior.STATE_EXPANDED
        );
    }

    private static void applySheetInsets(View root) {
        View content = root.findViewById(R.id.global_prefill_scroll);
        if (content == null) {
            return;
        }
        final int baseBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets navigationBars = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars()
            );
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop(),
                    view.getPaddingRight(),
                    baseBottomPadding + navigationBars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
