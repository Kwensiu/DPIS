package com.dpis.module.onboarding.presentation

import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.root.RootAccessProbe

object OnboardingGate {
    private var presenter: Presenter? = null
    private var pendingRequest: Request? = null
    private var presentedBy: Presenter? = null

    fun bind(nextPresenter: Presenter) {
        presenter = nextPresenter
        presentedBy = null
        presentPendingRequest()
    }

    fun clear(expectedPresenter: Presenter) {
        if (presenter === expectedPresenter) presenter = null
        if (presentedBy === expectedPresenter) presentedBy = null
    }

    fun show(request: Request): Boolean {
        val activePresenter = presenter
        val currentRequest = pendingRequest
        if (currentRequest != null && currentRequest.owner === request.owner &&
            activePresenter != null && presentedBy === activePresenter
        ) {
            return true
        }
        pendingRequest = request
        presentedBy = null
        presentPendingRequest()
        return true
    }

    fun clearRequest() {
        presenter?.dismiss()
        pendingRequest = null
        presentedBy = null
    }

    private fun presentPendingRequest() {
        val activePresenter = presenter ?: return
        val request = pendingRequest ?: return
        if (presentedBy === activePresenter) return
        if (activePresenter.show(request)) presentedBy = activePresenter
    }

    class Request(
        val owner: Any,
        val currentStep: () -> OnboardingStep,
        val acceptTerms: () -> Boolean,
        val persistStep: (OnboardingStep) -> Unit = {},
        val refreshRoot: ((RootAccessProbe.Result) -> Unit) -> Unit,
        val isAppListAccessAvailable: () -> Boolean,
        val requestAppListAccess: () -> Unit,
        val isUsageAccessAvailable: () -> Boolean,
        val requestUsageAccess: () -> Unit,
        val complete: () -> Boolean,
        val onCompleted: () -> Unit,
        val onBack: () -> Unit,
        val configuration: OnboardingConfigurationPresenter? = null,
        val onAppearanceChanged: () -> Unit = {},
        val onConfigurationChanged: () -> Unit = {},
    )

    interface Presenter {
        fun show(request: Request): Boolean
        fun dismiss()
    }
}
