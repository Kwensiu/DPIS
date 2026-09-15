package com.dpis.module.hyperos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperOsNativeRoutePolicyTest {
    @Test
    fun routeForPackageSelectsNamedRoutesNotAppBehavior() {
        assertEquals(
            HyperOsNativeRoutePolicy.PARAGRAPH_BUILDER,
            HyperOsNativeRoutePolicy.routeForPackage("com.miui.gallery"),
        )
        assertEquals(
            HyperOsNativeRoutePolicy.CONFIGURATION_GOT,
            HyperOsNativeRoutePolicy.routeForPackage("com.miui.weather2"),
        )
        assertNull(HyperOsNativeRoutePolicy.routeForPackage("com.example.app"))
        assertNull(HyperOsNativeRoutePolicy.routeForPackage(null))
        assertNull(HyperOsNativeRoutePolicy.routeForPackage(""))
    }

    @Test
    fun knownPackagesAreTheOnlyProbeFragments() {
        assertEquals(
            setOf("com.miui.gallery", "com.miui.weather2"),
            HyperOsNativeRoutePolicy.knownPackages(),
        )
        assertTrue(HyperOsNativeRoutePolicy.isKnownPackageFragment("pkg=com.miui.weather2"))
        assertTrue(HyperOsNativeRoutePolicy.isKnownPackageFragment("com.miui.gallery"))
        assertFalse(HyperOsNativeRoutePolicy.isKnownPackageFragment("com.example.app"))
        assertFalse(HyperOsNativeRoutePolicy.isKnownPackageFragment(null))
    }

    @Test
    fun envKeyMatchesNativeContract() {
        assertEquals("DPIS_NATIVE_ROUTE", HyperOsNativeRoutePolicy.ENV_KEY)
        assertEquals("PARAGRAPH_BUILDER", HyperOsNativeRoutePolicy.PARAGRAPH_BUILDER)
        assertEquals("CONFIGURATION_GOT", HyperOsNativeRoutePolicy.CONFIGURATION_GOT)
    }
}
