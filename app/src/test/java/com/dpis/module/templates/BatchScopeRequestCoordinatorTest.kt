package com.dpis.module.templates

import com.dpis.module.R
import io.github.libxposed.service.XposedService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchScopeRequestCoordinatorTest {
    @Test
    fun modernRequestsMissingPackagesInOneBatch() {
        val host = RecordingHost()
        val requester = FakeRequester(scopePackages = listOf("com.example.in.scope"))
        val coordinator = BatchScopeRequestCoordinator(host, requester, true)

        val result = coordinator.requestMissingScope(
            listOf("com.example.in.scope", "com.example.missing.one", "com.example.missing.two"),
        )

        assertTrue(result.requestStarted)
        assertEquals(listOf("com.example.missing.one", "com.example.missing.two"), requester.requestedPackages)
        assertEquals(1, requester.requestCallCount)
        assertEquals(R.string.quick_template_scope_request_started, host.toastIds.first())
        requester.listener!!.onScopeRequestFailed("dismissed")
    }

    @Test
    fun compatDoesNotRequestAndShowsManualGuidance() {
        val host = RecordingHost()
        val requester = FakeRequester()
        val result = BatchScopeRequestCoordinator(host, requester, false)
            .requestMissingScope(listOf("com.example.app"))

        assertFalse(result.requestStarted)
        assertTrue(result.manualRequired)
        assertEquals(0, requester.requestCallCount)
        assertEquals(R.string.quick_template_scope_manual_required, host.toastIds.first())
    }

    @Test
    fun unknownScopeDoesNotRollbackAndShowsManualGuidance() {
        val host = RecordingHost()
        val requester = FakeRequester(throwOnGetScope = true)
        val result = BatchScopeRequestCoordinator(host, requester, true)
            .requestMissingScope(listOf("com.example.app"))

        assertFalse(result.requestStarted)
        assertTrue(result.manualRequired)
        assertEquals(0, requester.requestCallCount)
        assertEquals(R.string.quick_template_scope_manual_required, host.toastIds.first())
    }

    @Test
    fun partialApprovalRefreshesActualScopeState() {
        val host = RecordingHost()
        val requester = FakeRequester()
        val coordinator = BatchScopeRequestCoordinator(host, requester, true)

        coordinator.requestMissingScope(listOf("com.example.one", "com.example.two"))
        requester.listener!!.onScopeRequestApproved(mutableListOf("com.example.one"))

        assertEquals(1, host.requestAppsLoadCount)
        assertEquals(R.string.quick_template_scope_request_approved, host.toastIds.last())
    }

    @Test
    fun ignoresBatchWhileAnotherScopeRequestIsAwaitingConfirmation() {
        val host = RecordingHost()
        val requester = FakeRequester()
        val coordinator = BatchScopeRequestCoordinator(host, requester, true)

        coordinator.requestMissingScope(listOf("com.example.one"))
        val second = coordinator.requestMissingScope(listOf("com.example.two"))

        assertFalse(second.requestStarted)
        assertFalse(second.manualRequired)
        assertEquals(1, requester.requestCallCount)
        assertEquals(R.string.scope_request_pending, host.toastIds.last())
        requester.listener!!.onScopeRequestFailed("dismissed")
    }

    private class FakeRequester(
        var scopePackages: List<String> = emptyList(),
        var throwOnGetScope: Boolean = false,
    ) : BatchScopeRequestCoordinator.ScopeRequester {
        var requestedPackages: List<String> = emptyList()
        var requestCallCount = 0
        var listener: XposedService.OnScopeEventListener? = null

        override fun getScope(): List<String> {
            check(!throwOnGetScope) { "scope unavailable" }
            return scopePackages
        }

        override fun requestScope(
            packages: List<String>,
            listener: XposedService.OnScopeEventListener,
        ) {
            requestCallCount++
            requestedPackages = packages.toList()
            this.listener = listener
        }
    }

    private class RecordingHost : BatchScopeRequestCoordinator.Host {
        val toastIds = mutableListOf<Int>()
        var requestAppsLoadCount = 0

        override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
            toastIds += messageResId
        }

        override fun requestAppsLoad() {
            requestAppsLoadCount++
        }

        override fun runOnUiThread(runnable: Runnable) = runnable.run()
    }
}
