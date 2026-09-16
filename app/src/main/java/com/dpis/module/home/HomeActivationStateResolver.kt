package com.dpis.module.home

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

    fun hasModernLibXposedService(readApiVersion: (() -> Int)?): Boolean =
        readApiVersion != null && isModernLibXposedServiceApi(readApiVersion)

    internal fun isModernLibXposedServiceApi(readApiVersion: () -> Int): Boolean = try {
        isModernLibXposedServiceApi(readApiVersion())
    } catch (_: RuntimeException) {
        false
    }

    @JvmStatic
    fun isModernLibXposedServiceApi(apiVersion: Int): Boolean = apiVersion >= 101
}
