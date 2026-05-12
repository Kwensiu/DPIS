package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class RefreshingConfigSnapshotProviderTest {
    @Test
    public void loadsSnapshotOnFirstRead() {
        ConfigSnapshot expected = snapshotWithPackage("com.example.one");
        RefreshingConfigSnapshotProvider provider = new RefreshingConfigSnapshotProvider(
                () -> expected,
                2_000L,
                () -> 0L);

        assertSame(expected, provider.get());
    }

    @Test
    public void reusesSnapshotWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        RefreshingConfigSnapshotProvider provider = new RefreshingConfigSnapshotProvider(
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
        RefreshingConfigSnapshotProvider provider = new RefreshingConfigSnapshotProvider(
                () -> snapshotWithPackage("com.example." + loads.incrementAndGet()),
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
        RefreshingConfigSnapshotProvider provider = new RefreshingConfigSnapshotProvider(
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
        RefreshingConfigSnapshotProvider provider = new RefreshingConfigSnapshotProvider(
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
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetViewportWidthDp(packageName, 360);
        return ConfigSnapshotLoader.fromStore(store);
    }
}
