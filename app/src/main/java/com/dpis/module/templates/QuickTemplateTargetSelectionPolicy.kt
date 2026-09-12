package com.dpis.module.templates

import java.util.Locale

/** JVM-testable filter, sort, and draft-selection rules for the quick-template target picker. */
object QuickTemplateTargetSelectionPolicy {
    const val SORT_NAME = 0
    const val SORT_UPDATED = 1
    const val SORT_INSTALLED = 2

    fun matches(
        label: String,
        packageName: String,
        configured: Boolean,
        systemApp: Boolean,
        query: String,
        showAllApps: Boolean,
        showSystemApps: Boolean,
        showUserApps: Boolean,
        showConfiguredApps: Boolean,
    ): Boolean {
        if (!showAllApps && systemApp && !showSystemApps && !(showConfiguredApps && configured)) {
            return false
        }
        if (!showAllApps && !systemApp && !showUserApps && !(showConfiguredApps && configured)) {
            return false
        }
        if (!showConfiguredApps && configured) return false
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) return true
        return label.lowercase(Locale.ROOT).contains(normalizedQuery) ||
            packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
    }

    data class SortableTarget(
        val label: String,
        val packageName: String,
        val configured: Boolean,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
    )

    fun compare(
        left: SortableTarget,
        right: SortableTarget,
        savedSelectedPackages: Set<String>,
        sortMode: Int,
        reverseOrder: Boolean,
    ): Int {
        val priority = Integer.compare(
            QuickTemplateTargetOrdering.priority(
                savedSelectedPackages.contains(left.packageName),
                left.configured,
            ),
            QuickTemplateTargetOrdering.priority(
                savedSelectedPackages.contains(right.packageName),
                right.configured,
            ),
        )
        if (priority != 0) return priority
        val result = when (sortMode) {
            SORT_UPDATED -> right.lastUpdateTime.compareTo(left.lastUpdateTime)
            SORT_INSTALLED -> right.firstInstallTime.compareTo(left.firstInstallTime)
            else -> left.label.compareTo(right.label, ignoreCase = true)
        }
        return if (reverseOrder) -result else result
    }

    fun retainInstalled(packages: MutableSet<String>, installedPackageNames: Set<String>) {
        packages.retainAll(installedPackageNames)
    }

    fun hasUnsavedChanges(selected: Set<String>, saved: Set<String>): Boolean = selected != saved
}
