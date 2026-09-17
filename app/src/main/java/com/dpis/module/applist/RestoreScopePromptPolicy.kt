package com.dpis.module.applist

import com.dpis.module.backup.ModuleScopeSnapshotPolicy

/** Candidate set for the one-shot prompt to restore a backup's application scope. */
object RestoreScopePromptPolicy {
    fun candidatePackages(apps: List<AppListItem>, restoredScope: Set<String>): List<String> {
        val packages = LinkedHashSet<String>()
        val importedPackages = ModuleScopeSnapshotPolicy.normalize(restoredScope).toSet()
        for (app in apps) {
            if (app.installed && app.scopeKnown && !app.inScope && app.packageName in importedPackages) {
                val packageName = app.packageName.trim()
                if (packageName.isNotEmpty()) {
                    packages.add(packageName)
                }
            }
        }
        return packages.toList()
    }

    fun restoredScopeReadable(apps: List<AppListItem>, restoredScope: Set<String>): Boolean {
        val importedPackages = ModuleScopeSnapshotPolicy.normalize(restoredScope).toSet()
        return apps.asSequence()
            .filter { it.installed && it.packageName in importedPackages }
            .all { it.scopeKnown }
    }

    fun shouldShowCard(
        pending: Boolean,
        modernFlavor: Boolean,
        configuredPage: Boolean,
        scopeReadable: Boolean,
        catalogSettled: Boolean,
        hasCandidates: Boolean,
    ): Boolean {
        return pending &&
            modernFlavor &&
            configuredPage &&
            scopeReadable &&
            catalogSettled &&
            hasCandidates
    }

    fun shouldConsumeIdle(
        pending: Boolean,
        modernFlavor: Boolean,
        scopeReadable: Boolean,
        catalogSettled: Boolean,
        hasCandidates: Boolean,
    ): Boolean {
        return pending && modernFlavor && scopeReadable && catalogSettled && !hasCandidates
    }

    fun shouldClearAfterRequest(requestStarted: Boolean, manualRequired: Boolean, affectedCount: Int): Boolean {
        return requestStarted || (!manualRequired && affectedCount == 0)
    }
}
