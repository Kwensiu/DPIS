package com.dpis.module.onboarding.presentation

import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.root.RootAccessProbe
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGateTest {
    @After
    fun clearPendingRequest() {
        OnboardingGate.clearRequest()
    }

    @Test
    fun pendingOnboardingRebindsAcrossHostRecreationAndCompletesOnlyCurrentOwner() {
        val oldOwner = Any()
        val newOwner = Any()
        var oldCompleted = 0
        var newCompleted = 0
        val oldHost = RecordingPresenter()
        val recreatedHost = RecordingPresenter()

        assertTrue(OnboardingGate.show(request(oldOwner) { oldCompleted++ }))
        OnboardingGate.bind(oldHost)
        assertEquals(1, oldHost.showCount)

        OnboardingGate.clear(oldHost)
        assertTrue(OnboardingGate.show(request(newOwner) { newCompleted++ }))
        OnboardingGate.bind(recreatedHost)
        assertEquals(1, recreatedHost.showCount)

        recreatedHost.complete()
        assertEquals(0, oldCompleted)
        assertEquals(1, newCompleted)
        assertEquals(1, recreatedHost.dismissCount)

        OnboardingGate.show(request(newOwner) { newCompleted++ })
        assertEquals(2, recreatedHost.showCount)
    }

    @Test
    fun duplicateRequestDoesNotReplaceTheActiveOnboardingPage() {
        val host = RecordingPresenter()
        OnboardingGate.bind(host)

        val owner = Any()
        OnboardingGate.show(request(owner) {})
        OnboardingGate.show(request(owner) {})

        assertEquals(1, host.showCount)
    }

    private fun request(owner: Any, onCompleted: () -> Unit) = OnboardingGate.Request(
        owner = owner,
        currentStep = { OnboardingStep.WELCOME },
        acceptTerms = { true },
        refreshRoot = { it(RootAccessProbe.Result.unavailable()) },
        isAppListAccessAvailable = { false },
        requestAppListAccess = {},
        isUsageAccessAvailable = { false },
        requestUsageAccess = {},
        complete = { true },
        onCompleted = onCompleted,
        onBack = {},
    )

    private class RecordingPresenter : OnboardingGate.Presenter {
        var showCount = 0
        var dismissCount = 0
        private var currentRequest: OnboardingGate.Request? = null

        override fun show(request: OnboardingGate.Request): Boolean {
            showCount++
            currentRequest = request
            return true
        }

        override fun dismiss() {
            dismissCount++
            currentRequest = null
        }

        fun complete() {
            currentRequest?.let {
                if (it.complete()) {
                    OnboardingGate.clearRequest()
                    it.onCompleted()
                }
            }
        }
    }
}
