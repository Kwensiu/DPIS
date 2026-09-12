package com.dpis.module.config

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageConfigRepositoryTest {
    @Test
    fun missingStoreOrPackageIsNotConfigured() {
        val repository = PackageConfigRepository(null)

        assertFalse(repository.hasConfiguredPackage(null))
        assertFalse(repository.hasConfiguredPackage("com.example.app"))
        assertFalse(repository.hasRealPackageConfig("com.example.app"))
    }

    @Test
    fun configuredMembershipFollowsTheStoreSnapshot() {
        val store = DpisConfigStore(FakePrefs())
        val repository = PackageConfigRepository(store)

        assertFalse(repository.hasConfiguredPackage("com.example.app"))
        assertFalse(repository.hasRealPackageConfig("com.example.app"))

        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(90_000)))

        assertTrue(repository.hasConfiguredPackage("com.example.app"))
        assertTrue(repository.hasRealPackageConfig("com.example.app"))
        assertFalse(repository.hasConfiguredPackage("com.example.other"))
    }
}
