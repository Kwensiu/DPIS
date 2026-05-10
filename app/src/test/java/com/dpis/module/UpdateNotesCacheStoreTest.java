package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UpdateNotesCacheStoreTest {
    @Test
    public void returnsCachedBodyBeforeTtlExpires() {
        ReleaseNotesCacheStore store = new ReleaseNotesCacheStore(new FakePrefs(), 24L * 60L * 60L * 1000L);
        store.put("1.7.0", "body", 1_000L);

        assertEquals("body", store.getValidBody("v1.7.0", 2_000L));
    }

    @Test
    public void returnsNullAfterTtlExpires() {
        ReleaseNotesCacheStore store = new ReleaseNotesCacheStore(new FakePrefs(), 24L * 60L * 60L * 1000L);
        store.put("1.7.0", "body", 1_000L);

        assertNull(store.getValidBody("1.7.0", 1_000L + 24L * 60L * 60L * 1000L + 1L));
    }

    @Test
    public void cachesEmptyBodiesToAvoidRepeatedFetches() {
        ReleaseNotesCacheStore store = new ReleaseNotesCacheStore(new FakePrefs(), 24L * 60L * 60L * 1000L);
        store.put("1.7.0", "  ", 1_000L);

        assertEquals("", store.getValidBody("1.7.0", 2_000L));
    }

    @Test
    public void keepsVersionsIsolated() {
        ReleaseNotesCacheStore store = new ReleaseNotesCacheStore(new FakePrefs(), 24L * 60L * 60L * 1000L);
        store.put("1.7.0", "new body", 1_000L);

        assertNull(store.getValidBody("1.6.3", 2_000L));
    }
}
