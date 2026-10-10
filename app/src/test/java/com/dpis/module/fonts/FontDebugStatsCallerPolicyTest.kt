package com.dpis.module

import com.dpis.module.fonts.FontDebugStatsCallerPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontDebugStatsCallerPolicyTest {
    @Test
    fun acceptsStatsOnlyFromAConfiguredPackageOwnedByTheCallingUid() {
        assertTrue(
            FontDebugStatsCallerPolicy.isAuthorized(
                sourcePackage = "com.example.target",
                packagesForUid = setOf("com.example.target"),
                configuredPackages = setOf("com.example.target"),
            ),
        )
    }

    @Test
    fun rejectsAnUnconfiguredPackageEvenWhenItOwnsTheCallingUid() {
        assertFalse(
            FontDebugStatsCallerPolicy.isAuthorized(
                sourcePackage = "com.example.other",
                packagesForUid = setOf("com.example.other"),
                configuredPackages = setOf("com.example.target"),
            ),
        )
    }

    @Test
    fun rejectsAConfiguredPackageNotOwnedByTheCallingUid() {
        assertFalse(
            FontDebugStatsCallerPolicy.isAuthorized(
                sourcePackage = "com.example.target",
                packagesForUid = setOf("com.example.other"),
                configuredPackages = setOf("com.example.target"),
            ),
        )
    }
}
