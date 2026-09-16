package com.dpis.module.home

import io.github.libxposed.service.XposedService

object HomeActivationStateResolver {
    @JvmStatic
    fun isActivatedForHome(
        hasModernLibXposedService: Boolean,
        selfLoaded: Boolean,
    ): Boolean = hasModernLibXposedService || selfLoaded

    /** A disabled detector deliberately presents Home as enabled for known card false positives. */
    @JvmStatic
    fun isActivatedForHome(
        detectionEnabled: Boolean,
        hasModernLibXposedService: Boolean,
        selfLoaded: Boolean,
    ): Boolean = !detectionEnabled || isActivatedForHome(hasModernLibXposedService, selfLoaded)

    @JvmStatic
    fun hasModernLibXposedService(service: XposedService?): Boolean {
        if (service == null) {
            return false
        }
        return try {
            isModernLibXposedServiceApi(service.apiVersion)
        } catch (_: RuntimeException) {
            false
        }
    }

    @JvmStatic
    fun isModernLibXposedServiceApi(apiVersion: Int): Boolean = apiVersion >= 101
}
