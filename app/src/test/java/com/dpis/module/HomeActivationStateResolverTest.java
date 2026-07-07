package com.dpis.module;

import com.dpis.module.home.HomeActivationStateResolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class HomeActivationStateResolverTest {
    @Test
    public void missingServiceAndSelfLoadDoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome(false, false));
    }

    @Test
    public void legacySelfLoadMarksHomeActivated() {
        assertTrue(HomeActivationStateResolver.isActivatedForHome(false, true));
    }

    @Test
    public void libXposedServiceBelow101DoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isModernLibXposedServiceApi(100));
    }

    @Test
    public void api101ServicePresenceActivatesHome() {
        assertTrue(HomeActivationStateResolver.isModernLibXposedServiceApi(101));
    }

    @Test
    public void serviceApiBelow101DoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome(false, false));
    }

    @Test
    public void serviceApi101ActivatesHome() {
        assertTrue(HomeActivationStateResolver.isActivatedForHome(true, false));
    }
}
