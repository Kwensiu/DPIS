package com.dpis.module.templates;

/** Shared ordering rule for target pickers across Compose and legacy landscape surfaces. */
public final class QuickTemplateTargetOrdering {
    private QuickTemplateTargetOrdering() {
    }

    public static int priority(boolean selected, boolean configured) {
        if (selected) {
            return 0;
        }
        return configured ? 1 : 2;
    }
}
