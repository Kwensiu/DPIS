package com.dpis.module.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PackageIdentityTest {
    @Test
    fun samePackageForDifferentAndroidUsersIsDifferentIdentity() {
        val primary = PackageIdentity("com.example.app", 0)
        val clone = PackageIdentity("com.example.app", 999)

        assertNotEquals(primary, clone)
        assertEquals("com.example.app", primary.packageName)
        assertEquals(0, primary.userId)
        assertEquals(999, clone.userId)
    }

    @Test
    fun rejectsMissingPackageNameAndNegativeUserId() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageIdentity("  ", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PackageIdentity("com.example.app", -1)
        }
    }
}
