package com.dpis.module.applist.presentation

import com.dpis.module.applist.AppListPage

/** Session-owned scroll positions for the two app catalogue pages. */
class AppListScrollStateStore {
    private val positions = IntArray(AppListPage.entries.size * VALUES_PER_PAGE)

    fun positionFor(page: AppListPage?): AppListPresentation.ScrollPosition {
        val offset = pageOffset(page)
        return AppListPresentation.ScrollPosition(positions[offset], positions[offset + 1])
    }

    fun update(page: AppListPage?, index: Int, scrollOffset: Int) {
        val offset = pageOffset(page)
        positions[offset] = index.coerceAtLeast(0)
        positions[offset + 1] = scrollOffset.coerceAtLeast(0)
    }

    fun snapshot(): IntArray = positions.copyOf()

    fun restore(snapshot: IntArray?) {
        if (snapshot == null || snapshot.size < positions.size) return
        for (page in AppListPage.entries) {
            val offset = pageOffset(page)
            update(page, snapshot[offset], snapshot[offset + 1])
        }
    }

    private fun pageOffset(page: AppListPage?): Int =
        (page ?: AppListPage.ALL_APPS).position() * VALUES_PER_PAGE

    private companion object {
        const val VALUES_PER_PAGE = 2
    }
}
