package com.dpis.module.onboarding

import android.content.SharedPreferences

class OnboardingStore(
    private val preferences: SharedPreferences,
    private val requiredTermsVersion: Int,
    legacyTermsAccepted: Boolean = false,
) {
    internal val importedLegacyTerms = legacyTermsAccepted &&
            !preferences.contains(KEY_TERMS_VERSION) &&
            preferences.edit()
                .putInt(KEY_TERMS_VERSION, requiredTermsVersion)
                .putBoolean(KEY_TERMS_ACCEPTED, true)
                .putBoolean(KEY_COMPLETED, false)
                .commit()

    fun read(): OnboardingState = OnboardingState(
        termsVersion = preferences.getInt(KEY_TERMS_VERSION, 0),
        termsAccepted = preferences.getBoolean(KEY_TERMS_ACCEPTED, false),
        completed = preferences.getBoolean(KEY_COMPLETED, false),
        lastStep = preferences.getString(KEY_CURRENT_STEP, null)
            ?.let { value -> runCatching { OnboardingStep.valueOf(value) }.getOrNull() },
    )

    fun setCurrentStep(step: OnboardingStep) {
        preferences.edit().putString(KEY_CURRENT_STEP, step.name).apply()
    }

    fun restart() {
        preferences.edit()
            .putBoolean(KEY_COMPLETED, false)
            .putString(KEY_CURRENT_STEP, OnboardingStep.WELCOME.name)
            .commit()
    }


    fun acceptTerms(): Boolean = preferences.edit()
        .putInt(KEY_TERMS_VERSION, requiredTermsVersion)
        .putBoolean(KEY_TERMS_ACCEPTED, true)
        .putBoolean(KEY_COMPLETED, false)
        .commit()

    fun markCompleted(): Boolean {
        if (!termsAcceptedForCurrentVersion()) return false
        return preferences.edit()
            .putBoolean(KEY_COMPLETED, true)
            .remove(KEY_CURRENT_STEP)
            .commit()
    }

    private fun termsAcceptedForCurrentVersion(): Boolean {
        val state = read()
        return state.termsVersion == requiredTermsVersion && state.termsAccepted
    }

    companion object {
        const val PREFS_NAME = "dpis.onboarding"
        private const val KEY_TERMS_VERSION = "terms_version"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_CURRENT_STEP = "current_step"
    }
}
