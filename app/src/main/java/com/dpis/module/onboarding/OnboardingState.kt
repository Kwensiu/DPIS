package com.dpis.module.onboarding

enum class OnboardingStep {
    WELCOME,
    TERMS,
    ROOT,
    CONFIGURATION,
}

data class OnboardingState(
    val termsVersion: Int,
    val termsAccepted: Boolean,
    val completed: Boolean,
    val lastStep: OnboardingStep? = null,
) {
    fun currentStep(requiredTermsVersion: Int): OnboardingStep? = when {
        termsVersion != requiredTermsVersion ->
            if (termsVersion == 0 && !termsAccepted) OnboardingStep.WELCOME
            else OnboardingStep.TERMS

        completed -> null
        lastStep != null -> lastStep
        !termsAccepted -> OnboardingStep.WELCOME
        else -> OnboardingStep.ROOT
    }
}
