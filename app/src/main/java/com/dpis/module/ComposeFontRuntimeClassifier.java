package com.dpis.module;

import java.util.List;

final class ComposeFontRuntimeClassifier {
    private static final String COMPOSE_VIEW_CLASS_NAME =
            "androidx.compose.ui.platform.ComposeView";
    private static final String ANDROID_COMPOSE_VIEW_CLASS_NAME =
            "androidx.compose.ui.platform.AndroidComposeView";

    private ComposeFontRuntimeClassifier() {
    }

    static boolean isKnownComposeViewClassName(String className) {
        return COMPOSE_VIEW_CLASS_NAME.equals(className)
                || ANDROID_COMPOSE_VIEW_CLASS_NAME.equals(className);
    }

    static boolean isComposeHeavy(ViewTreeNode currentRoot) {
        if (currentRoot == null) {
            return false;
        }
        if (isKnownComposeViewClassName(currentRoot.className())) {
            return true;
        }
        List<? extends ViewTreeNode> children = currentRoot.children();
        if (children == null || children.isEmpty()) {
            return false;
        }
        for (ViewTreeNode child : children) {
            if (isComposeHeavy(child)) {
                return true;
            }
        }
        return false;
    }

    interface ViewTreeNode {
        String className();

        List<? extends ViewTreeNode> children();
    }
}
