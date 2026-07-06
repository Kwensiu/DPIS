package com.dpis.module.config;

import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class PerAppDisplayConfigSourceRefreshingSnapshotProviderTest {
    @Test
    public void loadsSnapshotOnFirstRead() {
        ConfigSnapshot expected = snapshotWithPackage("com.example.one");
        PerAppDisplayConfigSource.RefreshingSnapshotProvider provider =
                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                        () -> expected,
                        2_000L,
                        () -> 0L);

        assertSame(expected, provider.get());
    }

    @Test
    public void reusesSnapshotWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        PerAppDisplayConfigSource.RefreshingSnapshotProvider provider =
                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                        () -> {
                            loads.incrementAndGet();
                            return snapshotWithPackage("com.example.one");
                        },
                        2_000L,
                        now::get);

        provider.get();
        now.set(1_000L);
        provider.get();

        assertEquals(1, loads.get());
    }

    @Test
    public void refreshesSnapshotAfterTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        PerAppDisplayConfigSource.RefreshingSnapshotProvider provider =
                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                        () -> snapshotWithPackage(
                                "com.example." + loads.incrementAndGet()),
                        2_000L,
                        now::get);

        ConfigSnapshot first = provider.get();
        now.set(2_100L);
        ConfigSnapshot second = provider.get();

        assertEquals(2, loads.get());
        assertTrue(first != second);
        assertTrue(second.isConfigured("com.example.2"));
    }

    @Test
    public void keepsPreviousSnapshotWhenRefreshFails() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        PerAppDisplayConfigSource.RefreshingSnapshotProvider provider =
                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                        () -> {
                            if (loads.incrementAndGet() == 1) {
                                return snapshotWithPackage("com.example.ok");
                            }
                            throw new IllegalStateException("boom");
                        },
                        2_000L,
                        now::get);

        ConfigSnapshot first = provider.get();
        now.set(2_100L);
        ConfigSnapshot second = provider.get();

        assertSame(first, second);
        assertTrue(second.isConfigured("com.example.ok"));
    }

    @Test
    public void failedRefreshRetriesAtTtlCadence() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        PerAppDisplayConfigSource.RefreshingSnapshotProvider provider =
                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                        () -> {
                            loads.incrementAndGet();
                            throw new IllegalStateException("boom");
                        },
                        2_000L,
                        now::get);

        provider.get();
        now.set(1_000L);
        provider.get();
        now.set(2_100L);
        provider.get();

        assertEquals(2, loads.get());
    }

    private static ConfigSnapshot snapshotWithPackage(String packageName) {
        LinkedHashMap<String, PackageConfigSnapshot> packages = new LinkedHashMap<>();
        packages.put(packageName, new PackageConfigSnapshot(
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
                HookDomainOverride.automatic()));
        return new ConfigSnapshot(
                Collections.singleton(packageName),
                packages,
                true,
                true,
                false,
                false,
                false,
                false);
    }
}
