package com.dpis.module.updates

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotesCacheStoreTest {
    @Test
    fun returnsCachedBodyBeforeTtlExpires() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "body", 1_000L)

        assertEquals("body", store.getValidBody("v1.7.0", 2_000L))
    }

    @Test
    fun returnsNullAfterTtlExpires() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "body", 1_000L)

        assertNull(store.getValidBody("1.7.0", 1_000L + DAY_MS + 1L))
    }

    @Test
    fun cachesEmptyBodiesToAvoidRepeatedFetches() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "  ", 1_000L)

        assertEquals("", store.getValidBody("1.7.0", 2_000L))
    }

    @Test
    fun keepsVersionsIsolated() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "new body", 1_000L)

        assertNull(store.getValidBody("1.6.3", 2_000L))
    }

    @Test
    fun rejectsFutureEntriesAndBlankVersionNames() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "body", 2_000L)

        assertNull(store.getValidBody("1.7.0", 1_999L))
        assertNull(store.getValidBody("  ", 2_000L))
    }

    @Test
    fun acceptsEntriesAtTheExactTtlBoundaryAndRejectsMissingBodies() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("v1.7.0", "body", 1_000L)
        store.put(" ", "ignored", 1_000L)
        store.put("1.8.0", null, 1_000L)

        assertEquals("body", store.getValidBody("1.7.0", 1_000L + DAY_MS))
        assertNull(store.getValidBody("1.8.0", 2_000L))
    }

    @Test
    fun clearRemovesAllCachedVersionsAndByteEstimateAccountsForStoredValues() {
        val store = ReleaseNotesCacheStore(FakePrefs(), DAY_MS)
        store.put("1.7.0", "body", 1_000L)

        assertTrue(store.estimateCacheBytes() > 0L)
        store.clear()
        assertEquals(0L, store.estimateCacheBytes())
        assertNull(store.getValidBody("1.7.0", 2_000L))
    }

    @Test
    fun negativeTtlExpiresAnyEntryAfterItsWriteTime() {
        val store = ReleaseNotesCacheStore(FakePrefs(), -1L)
        store.put("1.7.0", "body", 1_000L)

        assertEquals("body", store.getValidBody("1.7.0", 1_000L))
        assertNull(store.getValidBody("1.7.0", 1_001L))
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
