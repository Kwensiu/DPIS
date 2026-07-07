package com.dpis.module.appconfig;

import com.dpis.module.appconfig.UnsavedBadgeBinder;

import android.view.View;

import com.dpis.module.R;

import java.util.function.BooleanSupplier;

public final class UnsavedBadgeBinder {
    private final View container;
    private final View dragHandle;
    private final View unsavedBadge;
    private final View inlineBadge;
    private final BooleanSupplier hasUnsavedChanges;
    private final boolean showDragHandle;

    private UnsavedBadgeBinder(View container,
            View dragHandle,
            View unsavedBadge,
            View inlineBadge,
            BooleanSupplier hasUnsavedChanges,
            boolean showDragHandle) {
        this.container = container;
        this.dragHandle = dragHandle;
        this.unsavedBadge = unsavedBadge;
        this.inlineBadge = inlineBadge;
        this.hasUnsavedChanges = hasUnsavedChanges;
        this.showDragHandle = showDragHandle;
    }

    public static UnsavedBadgeBinder bind(View root,
            BooleanSupplier hasUnsavedChanges) {
        return bind(root, hasUnsavedChanges, true);
    }

    public static UnsavedBadgeBinder bind(View root,
            BooleanSupplier hasUnsavedChanges,
            boolean showDragHandle) {
        if (root == null) {
            return new UnsavedBadgeBinder(null, null, null, null,
                    hasUnsavedChanges, showDragHandle);
        }
        return new UnsavedBadgeBinder(
                root.findViewById(R.id.sheet_unsaved_badge_container),
                root.findViewById(R.id.sheet_drag_handle),
                root.findViewById(R.id.sheet_unsaved_badge),
                root.findViewById(R.id.sheet_unsaved_inline_badge),
                hasUnsavedChanges,
                showDragHandle);
    }

    public void refresh() {
        boolean dirty = hasUnsavedChanges != null && hasUnsavedChanges.getAsBoolean();
        setVisible(container, showDragHandle);
        setVisible(dragHandle, showDragHandle && !dirty);
        setVisible(unsavedBadge, showDragHandle && dirty);
        setVisible(inlineBadge, !showDragHandle && dirty);
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
