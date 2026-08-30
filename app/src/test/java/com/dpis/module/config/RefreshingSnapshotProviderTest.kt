package com.dpis.module.config

import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshingSnapshotProviderTest {
    @Test
    fun loadsSnapshotOnFirstRead() {
        val expected = snapshotWithPackage("com.example.one")
        val provider = PerAppDisplayConfigSource.RefreshingSnapshotProvider(
            { expected },
            2_000L,
            { 0L },
        )

        assertSame(expected, provider.get())
    }

    @Test
    fun reusesSnapshotWithinTtl() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val provider = PerAppDisplayConfigSource.RefreshingSnapshotProvider(
            {
                loads.incrementAndGet()
                snapshotWithPackage("com.example.one")
            },
            2_000L,
            now::get,
        )

        provider.get()
        now.set(1_000L)
        provider.get()

        assertEquals(1, loads.get())
    }

    @Test
    fun refreshesSnapshotAfterTtl() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val provider = PerAppDisplayConfigSource.RefreshingSnapshotProvider(
            {
                snapshotWithPackage("com.example.${loads.incrementAndGet()}")
            },
            2_000L,
            now::get,
        )

        val first = provider.get()
        now.set(2_100L)
        val second = provider.get()

        assertEquals(2, loads.get())
        assertTrue(first !== second)
        assertTrue(second!!.isConfigured("com.example.2"))
    }

    @Test
    fun keepsPreviousSnapshotWhenRefreshFails() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val provider = PerAppDisplayConfigSource.RefreshingSnapshotProvider(
            {
                if (loads.incrementAndGet() == 1) {
                    snapshotWithPackage("com.example.ok")
                } else {
                    throw IllegalStateException("boom")
                }
            },
            2_000L,
            now::get,
        )

        val first = provider.get()
        now.set(2_100L)
        val second = provider.get()

        assertSame(first, second)
        assertTrue(second!!.isConfigured("com.example.ok"))
    }

    @Test
    fun failedRefreshRetriesAtTtlCadence() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val provider = PerAppDisplayConfigSource.RefreshingSnapshotProvider(
            {
                loads.incrementAndGet()
                throw IllegalStateException("boom")
            },
            2_000L,
            now::get,
        )

        provider.get()
        now.set(1_000L)
        provider.get()
        now.set(2_100L)
        provider.get()

        assertEquals(2, loads.get())
    }

    private fun snapshotWithPackage(packageName: String): ConfigSnapshot {
        val packages = LinkedHashMap<String, PackageConfigSnapshot>()
        packages[packageName] = PackageConfigSnapshot(
            packageName,
            true,
            ViewportTargetSpec.absoluteDp(360),
            ViewportApplyMode.AUTO,
            null,
            null,
            null,
            false,
            false,
            false,
            HookDomainOverride.automatic(),
        )
        return ConfigSnapshot(
            Collections.singleton(packageName),
            packages,
            true,
            true,
            false,
            false,
            false,
            false,
        )
    }
}
