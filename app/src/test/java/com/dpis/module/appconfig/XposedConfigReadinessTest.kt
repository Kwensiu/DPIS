package com.dpis.module.appconfig

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XposedConfigReadinessTest {
    @Test
    fun eachFlavorRequiresItsOwnConfigurationDeliverySignal() {
        assertTrue(XposedConfigReadiness.isReady("modern", true, false))
        assertFalse(XposedConfigReadiness.isReady("modern", false, true))
        assertTrue(XposedConfigReadiness.isReady("legacy", false, true))
        assertFalse(XposedConfigReadiness.isReady("legacy", true, false))
        assertFalse(XposedConfigReadiness.isReady("unknown", true, true))
    }
}
