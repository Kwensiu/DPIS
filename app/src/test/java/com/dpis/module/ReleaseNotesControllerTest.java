package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ReleaseNotesControllerTest {
    @Test
    public void cacheHitDoesNotFetchRemoteBody() {
        ReleaseNotesCacheStore cacheStore = new ReleaseNotesCacheStore(new FakePrefs(), 10_000L);
        cacheStore.put("1.7.0", "cached body", 1_000L);
        AtomicBoolean fetched = new AtomicBoolean(false);
        RecordingListener listener = new RecordingListener();
        ReleaseNotesController controller = newController(
                cacheStore,
                (versionName, connectTimeoutMs, readTimeoutMs) -> {
                    fetched.set(true);
                    return "remote body";
                },
                () -> 2_000L);

        controller.load("v1.7.0", false, listener);

        assertEquals("cached body", listener.body);
        assertFalse(fetched.get());
    }

    @Test
    public void emptyRemoteBodyIsCachedAndReportedAsEmpty() {
        FakePrefs prefs = new FakePrefs();
        ReleaseNotesCacheStore cacheStore = new ReleaseNotesCacheStore(prefs, 10_000L);
        RecordingListener listener = new RecordingListener();
        ReleaseNotesController controller = newController(
                cacheStore,
                (versionName, connectTimeoutMs, readTimeoutMs) -> "  ",
                () -> 1_000L);

        controller.load("1.7.0", false, listener);

        assertEquals(1, listener.emptyCount);
        assertEquals("", cacheStore.getValidBody("1.7.0", 2_000L));
    }

    @Test
    public void cachedEmptyBodyDoesNotOverwriteEmbeddedNotes() {
        ReleaseNotesCacheStore cacheStore = new ReleaseNotesCacheStore(new FakePrefs(), 10_000L);
        cacheStore.put("1.7.0", "", 1_000L);
        RecordingListener listener = new RecordingListener();
        ReleaseNotesController controller = newController(
                cacheStore,
                (versionName, connectTimeoutMs, readTimeoutMs) -> "remote body",
                () -> 2_000L);

        controller.load("1.7.0", true, listener);

        assertEquals(0, listener.emptyCount);
        assertEquals(0, listener.failureCount);
        assertNull(listener.body);
    }

    private static ReleaseNotesController newController(ReleaseNotesCacheStore cacheStore,
            ReleaseNotesController.Fetcher fetcher,
            ReleaseNotesController.Clock clock) {
        Executor directExecutor = Runnable::run;
        return new ReleaseNotesController(
                cacheStore,
                directExecutor,
                Runnable::run,
                fetcher,
                clock,
                1,
                1);
    }

    private static final class RecordingListener implements ReleaseNotesController.Listener {
        String body;
        int emptyCount;
        int failureCount;

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public void onBody(String body) {
            this.body = body;
        }

        @Override
        public void onEmptyBody() {
            emptyCount++;
        }

        @Override
        public void onFailure() {
            failureCount++;
        }
    }
}
