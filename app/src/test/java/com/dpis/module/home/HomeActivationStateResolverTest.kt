package com.dpis.module.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeActivationStateResolverTest {
    @Test
    fun missingServiceAndSelfLoadDoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome(false, false))
    }

    @Test
    fun legacySelfLoadMarksHomeActivated() {
        assertTrue(HomeActivationStateResolver.isActivatedForHome(false, true))
    }

    @Test
    fun libXposedServiceBelow101DoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isModernLibXposedServiceApi(100))
    }

    @Test
    fun api101ServicePresenceActivatesHome() {
        assertTrue(HomeActivationStateResolver.isModernLibXposedServiceApi(101))
    }

    @Test
    fun serviceApiBelow101DoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome(false, false))
    }

    @Test
    fun serviceApi101ActivatesHome() {
        assertTrue(HomeActivationStateResolver.isActivatedForHome(true, false))
    }

    @Test
    fun disabledDetectionAlwaysMarksHomeActivated() {
        assertTrue(HomeActivationStateResolver.isActivatedForHome(false, false, false))
    }

    @Test
    fun enabledDetectionStillRequiresAnActivationSignal() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome(true, false, false))
    }

    @Test
    fun missingLibXposedServiceIsNotModern() {
        assertFalse(HomeActivationStateResolver.hasModernLibXposedService(null))
    }
}
