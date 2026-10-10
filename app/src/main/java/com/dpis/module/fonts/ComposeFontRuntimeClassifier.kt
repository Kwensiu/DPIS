package com.dpis.module.fonts

import java.util.ArrayDeque

object ComposeFontRuntimeClassifier {
    private const val COMPOSE_VIEW_CLASS_NAME = "androidx.compose.ui.platform.ComposeView"
    private const val ANDROID_COMPOSE_VIEW_CLASS_NAME =
        "androidx.compose.ui.platform.AndroidComposeView"
    private const val MAX_TRAVERSAL_DEPTH = 32
    private const val MAX_TRAVERSAL_NODES = 512

    @JvmStatic
    fun isKnownComposeViewClassName(className: String?): Boolean =
        className == COMPOSE_VIEW_CLASS_NAME || className == ANDROID_COMPOSE_VIEW_CLASS_NAME

    @JvmStatic
    fun isComposeHeavy(currentRoot: ViewTreeNode?): Boolean {
        if (currentRoot == null) return false
        val pending = ArrayDeque<NodeVisit>()
        pending.add(NodeVisit(currentRoot, 0))
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_TRAVERSAL_NODES) {
            val visit = pending.removeFirst()
            val node = visit.node ?: continue
            visited++
            if (isKnownComposeViewClassName(node.className())) return true
            if (visit.depth >= MAX_TRAVERSAL_DEPTH) continue
            node.children()?.forEach { pending.addLast(NodeVisit(it, visit.depth + 1)) }
        }
        return false
    }

    private data class NodeVisit(val node: ViewTreeNode?, val depth: Int)

    interface ViewTreeNode {
        fun className(): String?
        fun children(): List<@JvmWildcard ViewTreeNode>?
    }
}
