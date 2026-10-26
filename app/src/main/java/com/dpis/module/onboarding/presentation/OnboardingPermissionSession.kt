package com.dpis.module.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpis.module.root.RootAccessProbe

/** Owns capability state for the permissions page and ignores stale root results. */
internal class OnboardingPermissionSession(
    private val request: OnboardingGate.Request,
) {
    var rootStatus by mutableStateOf(RootAccessProbe.Status.UNKNOWN)
        private set
    var rootProvider by mutableStateOf("")
        private set
    var appListAccess by mutableStateOf(false)
        private set
    var usageAccess by mutableStateOf(false)
        private set
    var checkingRoot by mutableStateOf(false)
        private set

    private var requestGeneration by mutableIntStateOf(0)

    fun refreshRoot(force: Boolean = false) {
        if (checkingRoot && !force) return
        val generation = ++requestGeneration
        checkingRoot = true
        request.refreshRoot { result ->
            if (generation != requestGeneration) return@refreshRoot
            rootStatus = result.status
            rootProvider = result.provider
            checkingRoot = false
        }
    }

    fun refreshOptionalAccess() {
        appListAccess = request.isAppListAccessAvailable()
        usageAccess = request.isUsageAccessAvailable()
    }

    fun invalidatePendingRootRequest() {
        requestGeneration++
    }
}
