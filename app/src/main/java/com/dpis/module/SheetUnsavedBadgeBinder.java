package com.dpis.module;

import android.view.View;

import java.util.function.BooleanSupplier;

final class SheetUnsavedBadgeBinder {
    private final View container;
    private final View dragHandle;
    private final View unsavedBadge;
    private final View inlineBadge;
    private final BooleanSupplier hasUnsavedChanges;
    private final boolean showDragHandle;

    private SheetUnsavedBadgeBinder(View container,
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

    static SheetUnsavedBadgeBinder bind(View root,
            BooleanSupplier hasUnsavedChanges) {
        return bind(root, hasUnsavedChanges, true);
    }

    static SheetUnsavedBadgeBinder bind(View root,
            BooleanSupplier hasUnsavedChanges,
            boolean showDragHandle) {
        if (root == null) {
            return new SheetUnsavedBadgeBinder(null, null, null, null,
                    hasUnsavedChanges, showDragHandle);
        }
        return new SheetUnsavedBadgeBinder(
                root.findViewById(R.id.sheet_unsaved_badge_container),
                root.findViewById(R.id.sheet_drag_handle),
                root.findViewById(R.id.sheet_unsaved_badge),
                root.findViewById(R.id.sheet_unsaved_inline_badge),
                hasUnsavedChanges,
                showDragHandle);
    }

    void refresh() {
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
