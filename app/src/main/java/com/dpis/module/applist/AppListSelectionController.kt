package com.dpis.module.applist

/**
 * Owns the transient selection for one app-list page. Package names, rather than list indices,
 * keep selection stable while the catalogue refreshes or rows are recomposed.
 */
class AppListSelectionController {
    private var selectedPage: AppListPage? = null
    private var selectedPackageNames = linkedSetOf<String>()
    private var batchOperationRunning = false

    /**
     * Reconciles selection against the page's unfiltered package universe. Search and filter
     * changes must not discard a selected package merely because it is temporarily hidden.
     */
    fun reconcile(page: AppListPage, availablePackageNames: Collection<String>) {
        if (selectedPage != page) {
            if (!batchOperationRunning) exit()
            return
        }
        selectedPackageNames.retainAll(availablePackageNames.toSet())
    }

    /** Returns the current selection projected onto [page] without changing the controller. */
    fun snapshotFor(page: AppListPage): State {
        val onRequestedPage = selectedPage == page
        return State(
            onRequestedPage && selectedPackageNames.isNotEmpty(),
            if (onRequestedPage) selectedPage else null,
            if (onRequestedPage) selectedPackageNames.toSet() else emptySet(),
            batchOperationRunning
        )
    }

    fun begin(page: AppListPage, packageName: String) {
        if (batchOperationRunning) return
        selectedPage = page
        selectedPackageNames = linkedSetOf(packageName)
    }

    fun toggle(page: AppListPage, packageName: String) {
        if (batchOperationRunning) return
        if (selectedPage != page) {
            begin(page, packageName)
            return
        }
        if (!selectedPackageNames.add(packageName)) selectedPackageNames.remove(packageName)
    }

    fun selectAll(page: AppListPage, visiblePackageNames: Collection<String>) {
        if (batchOperationRunning) return
        selectedPage = page
        selectedPackageNames = LinkedHashSet(visiblePackageNames)
    }

    fun invert(page: AppListPage, visiblePackageNames: Collection<String>) {
        if (batchOperationRunning) return
        val visible = LinkedHashSet(visiblePackageNames)
        if (selectedPage != page) {
            selectedPage = page
            selectedPackageNames = visible
        } else {
            selectedPackageNames = visible.filterNotTo(linkedSetOf()) { it in selectedPackageNames }
        }
    }

    fun beginBatch(): List<String> {
        if (batchOperationRunning || selectedPackageNames.isEmpty()) return emptyList()
        batchOperationRunning = true
        return selectedPackageNames.toList()
    }

    /**
     * Completes a batch operation without changing the user's selection.
     *
     * The next catalogue snapshot removes packages that are no longer visible, such as an
     * app whose only DPIS configuration was reset from the configured-apps page.
     */
    fun finishBatch() {
        batchOperationRunning = false
    }

    fun exit() {
        if (batchOperationRunning) return
        selectedPage = null
        selectedPackageNames.clear()
    }

    fun isBatchOperationRunning(): Boolean = batchOperationRunning

    data class State(
        val active: Boolean,
        val page: AppListPage?,
        val packageNames: Set<String>,
        val batchOperationRunning: Boolean,
    )
}
