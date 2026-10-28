package com.dpis.module.onboarding

import com.dpis.module.root.RootAccessProbe

class OnboardingCoordinator(
    private val store: OnboardingStore,
    private val rootRefresh: ((RootAccessProbe.Result?) -> Unit) -> Unit = { onResult ->
        RootAccessProbe.refreshAsync { onResult(it) }
    },
) {
    private var termsAcceptedThisSession = store.importedLegacyTerms

    fun state(): OnboardingState = store.read()

    fun requiredStep(): OnboardingStep? {
        val state = store.read()
        if (state.termsVersion != REQUIRED_TERMS_VERSION) {
            return state.currentStep(REQUIRED_TERMS_VERSION)
        }
        if (state.completed) return null
        if (state.lastStep != null) return state.lastStep
        if (state.termsAccepted && !termsAcceptedThisSession) return OnboardingStep.TERMS
        return state.currentStep(REQUIRED_TERMS_VERSION)
    }

    fun acceptTerms(): Boolean = store.acceptTerms().also {
        if (it) termsAcceptedThisSession = true
    }

    fun persistStep(step: OnboardingStep) = store.setCurrentStep(step)

    fun restart() = store.restart()


    fun refreshRootAsync(onResult: (RootAccessProbe.Result) -> Unit) {
        rootRefresh { result ->
            onResult(result ?: RootAccessProbe.Result.unavailable())
        }
    }

    fun complete(): Boolean = store.markCompleted()

    companion object {
        const val REQUIRED_TERMS_VERSION = 1
    }
}
