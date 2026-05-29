package com.dpis.module;

import android.view.View;

import java.util.function.BooleanSupplier;

final class SheetUnsavedBadgeBinder {
    private final View dragHandle;
    private final View unsavedBadge;
    private final BooleanSupplier hasUnsavedChanges;

    private SheetUnsavedBadgeBinder(View dragHandle,
            View unsavedBadge,
            BooleanSupplier hasUnsavedChanges) {
        this.dragHandle = dragHandle;
        this.unsavedBadge = unsavedBadge;
        this.hasUnsavedChanges = hasUnsavedChanges;
    }

    static SheetUnsavedBadgeBinder bind(View root,
            BooleanSupplier hasUnsavedChanges) {
        if (root == null) {
            return new SheetUnsavedBadgeBinder(null, null, hasUnsavedChanges);
        }
        return new SheetUnsavedBadgeBinder(
                root.findViewById(R.id.sheet_drag_handle),
                root.findViewById(R.id.sheet_unsaved_badge),
                hasUnsavedChanges);
    }

    void refresh() {
        boolean dirty = hasUnsavedChanges != null && hasUnsavedChanges.getAsBoolean();
        setVisible(dragHandle, !dirty);
        setVisible(unsavedBadge, dirty);
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
