package com.dpis.module.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListSelectionControllerTest {
    @Test
    fun selectionRemainsActiveWhenFilteringTemporarilyHidesThePackage() {
        val controller = AppListSelectionController()

        controller.begin(AppListPage.ALL_APPS, "com.example.one")
        controller.reconcile(
            AppListPage.ALL_APPS,
            listOf("com.example.one", "com.example.two"),
        )
        val initial = controller.snapshotFor(AppListPage.ALL_APPS)
        val filtered = controller.snapshotFor(AppListPage.ALL_APPS)

        assertTrue(initial.active)
        assertEquals(setOf("com.example.one"), initial.packageNames)
        assertTrue(filtered.active)
        assertEquals(setOf("com.example.one"), filtered.packageNames)
    }

    @Test
    fun invertOnlyChangesTheCurrentVisibleUniverse() {
        val controller = AppListSelectionController()
        controller.begin(AppListPage.ALL_APPS, "com.example.one")

        controller.invert(
            AppListPage.ALL_APPS,
            listOf("com.example.one", "com.example.two", "com.example.three"),
        )
        val state = controller.snapshotFor(AppListPage.ALL_APPS)

        assertEquals(setOf("com.example.two", "com.example.three"), state.packageNames)
    }

    @Test
    fun changingPageExitsSelectionInsteadOfRetainingOverlappingRows() {
        val controller = AppListSelectionController()
        controller.begin(AppListPage.ALL_APPS, "com.example.shared")

        val state = controller.snapshotFor(AppListPage.CONFIGURED_APPS)

        assertFalse(state.active)
        assertTrue(state.packageNames.isEmpty())
        assertEquals(
            setOf("com.example.shared"),
            controller.snapshotFor(AppListPage.ALL_APPS).packageNames
        )
    }

    @Test
    fun batchCompletionRetainsSelectionForSubsequentOperations() {
        val controller = AppListSelectionController()
        controller.selectAll(AppListPage.ALL_APPS, listOf("com.example.one", "com.example.two"))

        assertEquals(
            listOf("com.example.one", "com.example.two"),
            controller.beginBatch(),
        )
        controller.finishBatch()
        controller.reconcile(
            AppListPage.ALL_APPS,
            listOf("com.example.one", "com.example.two"),
        )
        val state = controller.snapshotFor(AppListPage.ALL_APPS)

        assertTrue(state.active)
        assertEquals(setOf("com.example.one", "com.example.two"), state.packageNames)
        assertFalse(state.batchOperationRunning)
    }

    @Test
    fun snapshotDoesNotDiscardSelectionUntilDirectoryReconcileRuns() {
        val controller = AppListSelectionController()
        controller.begin(AppListPage.ALL_APPS, "com.example.one")

        assertEquals(
            setOf("com.example.one"),
            controller.snapshotFor(AppListPage.ALL_APPS).packageNames
        )
        controller.reconcile(AppListPage.ALL_APPS, listOf("com.example.two"))
        assertTrue(controller.snapshotFor(AppListPage.ALL_APPS).packageNames.isEmpty())
    }
}
