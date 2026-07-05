package com.dpis.module.fonts;

import java.util.ArrayDeque;
import java.util.List;

public final class ComposeFontRuntimeClassifier {
    private static final String COMPOSE_VIEW_CLASS_NAME =
            "androidx.compose.ui.platform.ComposeView";
    private static final String ANDROID_COMPOSE_VIEW_CLASS_NAME =
            "androidx.compose.ui.platform.AndroidComposeView";
    private static final int MAX_TRAVERSAL_DEPTH = 32;
    private static final int MAX_TRAVERSAL_NODES = 512;

    private ComposeFontRuntimeClassifier() {
    }

    public static boolean isKnownComposeViewClassName(String className) {
        return COMPOSE_VIEW_CLASS_NAME.equals(className)
                || ANDROID_COMPOSE_VIEW_CLASS_NAME.equals(className);
    }

    public static boolean isComposeHeavy(ViewTreeNode currentRoot) {
        if (currentRoot == null) {
            return false;
        }
        ArrayDeque<NodeVisit> pending = new ArrayDeque<>();
        pending.add(new NodeVisit(currentRoot, 0));
        int visited = 0;
        while (!pending.isEmpty() && visited < MAX_TRAVERSAL_NODES) {
            NodeVisit visit = pending.removeFirst();
            ViewTreeNode node = visit.node;
            if (node == null) {
                continue;
            }
            visited++;
            if (isKnownComposeViewClassName(node.className())) {
                return true;
            }
            if (visit.depth >= MAX_TRAVERSAL_DEPTH) {
                continue;
            }
            List<? extends ViewTreeNode> children = node.children();
            if (children == null || children.isEmpty()) {
                continue;
            }
            for (ViewTreeNode child : children) {
                pending.addLast(new NodeVisit(child, visit.depth + 1));
            }
        }
        return false;
    }

    private static final class NodeVisit {
        final ViewTreeNode node;
        final int depth;

        NodeVisit(ViewTreeNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    public interface ViewTreeNode {
        String className();

        List<? extends ViewTreeNode> children();
    }
}
