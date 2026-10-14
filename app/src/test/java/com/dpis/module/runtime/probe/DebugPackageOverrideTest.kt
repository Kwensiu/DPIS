package com.dpis.module.runtime.probe

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugPackageOverrideTest {
    @Test
    fun matchesConfiguredPackageAfterTrimmingPropertyValue() {
        assertTrue(
            DebugPackageOverride.matchesForTest(
                "debug.dpis.test.package",
                "com.example.target",
                "  com.example.target  ",
            ),
        )
    }

    @Test
    fun wildcardMatchesAnyNonBlankPackage() {
        assertTrue(
            DebugPackageOverride.matchesForTest(
                "debug.dpis.test.package",
                "com.example.target",
                "*",
            ),
        )
    }

    @Test
    fun unrelatedOrMissingValuesDoNotMatch() {
        assertFalse(
            DebugPackageOverride.matchesForTest(
                "debug.dpis.test.package",
                "com.example.target",
                "com.example.other",
            ),
        )
        assertFalse(
            DebugPackageOverride.matchesForTest(
                "debug.dpis.test.package",
                "",
                "*",
            ),
        )
        assertFalse(
            DebugPackageOverride.matchesForTest(
                "debug.dpis.test.package",
                "com.example.target",
                null,
            ),
        )
    }
}
