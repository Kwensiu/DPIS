package com.dpis.module.applist

import com.dpis.module.FakePrefs
import com.dpis.module.applist.presentation.AppListBatchActionCoordinator
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListBatchActionCoordinatorTest {
    @Test
    fun scopeResultCountsAlreadyScopedAndDeniedPackagesAsSkipped() {
        val result = AppListBatchActionCoordinator.scopeResult(
            selectedPackages = listOf("already", "approved", "denied"),
            currentScope = setOf("already"),
            approvedPackages = listOf("approved"),
        )

        assertEquals(1, result.updatedCount)
        assertEquals(2, result.skippedCount)
    }

    @Test
    fun unreadyBatchMutationsLeaveConfigAndRuntimePublicationUntouched() {
        val packageName = "com.example.app"
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetDpisEnabled(packageName, false))
        val selected = AppListItem(
            "Example",
            packageName,
            false,
            true,
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            false,
            false,
            false,
            null,
        )
        val host = RecordingHost(store)
        val selection = AppListSelectionController().apply {
            begin(AppListPage.CONFIGURED_APPS, packageName)
        }
        val coordinator = AppListBatchActionCoordinator(
            host,
            selection,
            { false },
            { host.runtimePublications++ },
        )

        coordinator.setConfigsEnabled(true, listOf(selected))
        coordinator.reset(listOf(selected))

        assertFalse(store.isTargetDpisEnabled(packageName))
        assertTrue(store.hasRealPackageConfig(packageName))
        assertEquals(0, host.backgroundTasks)
        assertEquals(0, host.runtimePublications)
    }

    @Test
    fun batchTaskRechecksReadinessImmediatelyBeforeWriting() {
        val packageName = "com.example.app"
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetDpisEnabled(packageName, false))
        val selected = AppListItem(
            "Example",
            packageName,
            false,
            true,
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            false,
            false,
            false,
            null,
        )
        var ready = true
        val host = RecordingHost(store)
        val enableCoordinator = AppListBatchActionCoordinator(
            host,
            AppListSelectionController().apply {
                begin(AppListPage.CONFIGURED_APPS, packageName)
            },
            { ready },
            { host.runtimePublications++ },
        )
        val resetCoordinator = AppListBatchActionCoordinator(
            host,
            AppListSelectionController().apply {
                begin(AppListPage.CONFIGURED_APPS, packageName)
            },
            { ready },
            { host.runtimePublications++ },
        )

        enableCoordinator.setConfigsEnabled(true, listOf(selected))
        resetCoordinator.reset(listOf(selected))
        assertEquals(2, host.backgroundActions.size)

        ready = false
        host.backgroundActions.forEach(Runnable::run)

        assertFalse(store.isTargetDpisEnabled(packageName))
        assertTrue(store.hasRealPackageConfig(packageName))
        assertEquals(0, host.runtimePublications)
        assertEquals(0, host.enabledStatePublications)
    }

    private class RecordingHost(
        private val store: DpisConfigStore,
    ) : AppListBatchActionCoordinator.Host {
        var backgroundTasks = 0
        var runtimePublications = 0
        var enabledStatePublications = 0
        val backgroundActions = mutableListOf<Runnable>()

        override fun configStore() = store
        override fun showToast(messageResId: Int, vararg formatArgs: Any?) = Unit
        override fun refreshApps() = Unit
        override fun publishSelection() = Unit
        override fun publishDpisEnabled(packageNames: Collection<String>, enabled: Boolean) {
            enabledStatePublications++
        }

        override fun runOnUiThread(action: Runnable) = action.run()
        override fun runInBackground(action: Runnable) {
            backgroundTasks++
            backgroundActions += action
        }
    }
}
