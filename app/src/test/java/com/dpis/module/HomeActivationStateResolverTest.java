package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class HomeActivationStateResolverTest {
    @Before
    public void setUp() {
        DpisApplication.clearXposedSelfLoadedForTest();
    }

    @After
    public void tearDown() {
        HomeActivationStateResolver.setServiceApiResolverForTest(null);
        HomeActivationStateResolver.setModernServiceProbeForTest(null);
        DpisApplication.clearXposedSelfLoadedForTest();
    }

    @Test
    public void missingServiceAndSelfLoadDoesNotActivateHome() {
        assertFalse(HomeActivationStateResolver.isActivatedForHome());
    }

    @Test
    public void legacySelfLoadMarksHomeActivated() {
        DpisApplication.markXposedSelfLoaded();

        assertTrue(HomeActivationStateResolver.isActivatedForHome());
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
        HomeActivationStateResolver.setModernServiceProbeForTest(() -> false);

        assertFalse(HomeActivationStateResolver.isActivatedForHome());
    }

    @Test
    public void serviceApi101ActivatesHome() {
        HomeActivationStateResolver.setModernServiceProbeForTest(() -> true);

        assertTrue(HomeActivationStateResolver.isActivatedForHome());
    }

    @Test
    public void serviceApiFailureDoesNotActivateHomeWithoutSelfLoad() {
        HomeActivationStateResolver.setModernServiceProbeForTest(() -> {
            throw new RuntimeException("boom");
        });

        assertFalse(HomeActivationStateResolver.isActivatedForHome());
    }
}
