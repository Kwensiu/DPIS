package com.dpis.module.onboarding.presentation

import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.root.RootAccessProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class OnboardingPermissionSessionTest {
    @Test
    fun staleRefreshResultDoesNotFinishTheCurrentRequest() {
        val pending = ArrayDeque<(RootAccessProbe.Result) -> Unit>()
        val session = OnboardingPermissionSession(request { callback -> pending.addLast(callback) })

        session.refreshRoot()
        session.refreshRoot(force = true)
        pending.removeFirst()(RootAccessProbe.Result.available("stale"))

        assertTrue(session.checkingRoot)
        assertEquals(RootAccessProbe.Status.UNKNOWN, session.rootStatus)

        pending.removeFirst()(RootAccessProbe.Result.available("KernelSU"))

        assertEquals(RootAccessProbe.Status.AVAILABLE, session.rootStatus)
        assertEquals("KernelSU", session.rootProvider)
        assertFalse(session.checkingRoot)
    }

    @Test
    fun invalidatingThePageIgnoresARefreshThatReturnsAfterLeavingIt() {
        var callback: ((RootAccessProbe.Result) -> Unit)? = null
        val session = OnboardingPermissionSession(request { callback = it })

        session.refreshRoot()
        session.invalidatePendingRootRequest()
        callback?.invoke(RootAccessProbe.Result.available("stale"))

        assertEquals(RootAccessProbe.Status.UNKNOWN, session.rootStatus)
        assertTrue(session.checkingRoot)
    }

    private fun request(
        refreshRoot: (((RootAccessProbe.Result) -> Unit) -> Unit),
    ) = OnboardingGate.Request(
        owner = Any(),
        currentStep = { OnboardingStep.ROOT },
        acceptTerms = { true },
        refreshRoot = refreshRoot,
        isAppListAccessAvailable = { false },
        requestAppListAccess = {},
        isUsageAccessAvailable = { false },
        requestUsageAccess = {},
        complete = { true },
        onCompleted = {},
        onBack = {},
    )
}
