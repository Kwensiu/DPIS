package com.dpis.module;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCoordinatorTest {
    @Test
    public void evaluateStartupCheck_allowsWhenNormalIntervalElapsed() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        long intervalMs = UpdateCoordinator.DEFAULT_STARTUP_CHECK_INTERVAL_MS;
        UpdateCoordinator.State state = UpdateCoordinator.State.initial(1_000L, false, 0);

        UpdateCoordinator.StartupCheckGate gate =
                coordinator.evaluateStartupCheck(state, 1_000L + intervalMs);

        assertTrue(gate.shouldStart);
        assertEquals(UpdateCoordinator.StartupCheckReason.ALLOWED, gate.reason);
        assertEquals(intervalMs, gate.requiredIntervalMs);
    }

    @Test
    public void evaluateStartupCheck_usesFailureRetryWindow() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        long retryMs = UpdateCoordinator.DEFAULT_FAILURE_RETRY_INTERVAL_MS;
        UpdateCoordinator.State failed = UpdateCoordinator.State.initial(5_000L, true, 0);

        UpdateCoordinator.StartupCheckGate early =
                coordinator.evaluateStartupCheck(failed, 5_000L + retryMs - 1L);
        assertFalse(early.shouldStart);
        assertEquals(UpdateCoordinator.StartupCheckReason.WAITING_FOR_INTERVAL, early.reason);
        assertEquals(retryMs, early.requiredIntervalMs);

        UpdateCoordinator.StartupCheckGate onTime =
                coordinator.evaluateStartupCheck(failed, 5_000L + retryMs);
        assertTrue(onTime.shouldStart);
        assertEquals(UpdateCoordinator.StartupCheckReason.ALLOWED, onTime.reason);
    }

    @Test
    public void evaluateStartupCheck_blocksWhenAlreadyInProgress() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = new UpdateCoordinator.State(
                0L,
                false,
                0,
                true,
                false,
                false);

        UpdateCoordinator.StartupCheckGate gate = coordinator.evaluateStartupCheck(state, 100L);

        assertFalse(gate.shouldStart);
        assertEquals(UpdateCoordinator.StartupCheckReason.CHECK_IN_PROGRESS, gate.reason);
    }

    @Test
    public void markStartupCheckFinished_updatesPersistentFieldsAndClearsInProgress() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State started = coordinator.markStartupCheckStarted(
                UpdateCoordinator.State.initial(100L, false, 7));

        UpdateCoordinator.State finished = coordinator.markStartupCheckFinished(
                started,
                999L,
                false);

        assertEquals(999L, finished.lastUpdateCheckTimestampMs);
        assertTrue(finished.lastUpdateCheckFailed);
        assertFalse(finished.startupCheckInProgress);
        assertEquals(7, finished.lastPromptedUpdateVersionCode);
    }

    @Test
    public void evaluatePromptDecision_requiresRemoteNewer() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = UpdateCoordinator.State.initial(0L, false, 0);

        UpdateCoordinator.PromptDecision decision = coordinator.evaluatePromptDecision(
                state,
                100,
                "1.0.0",
                101,
                "1.1.0");

        assertFalse(decision.shouldPrompt);
        assertEquals(UpdateCoordinator.PromptReason.REMOTE_NOT_NEWER, decision.reason);
    }

    @Test
    public void evaluatePromptDecision_deduplicatesPromptByVersionCode() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = UpdateCoordinator.State.initial(0L, false, 42);

        UpdateCoordinator.PromptDecision decision = coordinator.evaluatePromptDecision(
                state,
                42,
                "1.2.0",
                42,
                "1.1.0");

        assertFalse(decision.shouldPrompt);
        assertEquals(UpdateCoordinator.PromptReason.ALREADY_PROMPTED, decision.reason);
    }

    @Test
    public void evaluatePromptDecision_allowsWhenNewerAndNotPrompted() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = UpdateCoordinator.State.initial(0L, false, 9);

        UpdateCoordinator.PromptDecision decision = coordinator.evaluatePromptDecision(
                state,
                10,
                "1.2.0",
                9,
                "1.1.0");

        assertTrue(decision.shouldPrompt);
        assertEquals(UpdateCoordinator.PromptReason.SHOULD_PROMPT, decision.reason);
    }

    @Test
    public void parseSemVer_supportsLeadingVAndSuffix() {
        int[] parsed = UpdateCoordinator.parseSemVer("v2.10.3-beta.1");

        assertNotNull(parsed);
        assertArrayEquals(new int[]{2, 10, 3}, parsed);
    }

    @Test
    public void compareSemVer_invalidInputFallsBackToEqual() {
        int compared = UpdateCoordinator.compareSemVer("nightly", "1.0.0");

        assertEquals(0, compared);
    }

    @Test
    public void requestDownloadStart_rejectsInvalidOrNonHttpsUrl() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = UpdateCoordinator.State.empty();

        UpdateCoordinator.DownloadDecision empty = coordinator.requestDownloadStart(state, "   ");
        assertFalse(empty.started);
        assertEquals(UpdateCoordinator.DownloadStartReason.EMPTY_URL, empty.reason);

        UpdateCoordinator.DownloadDecision invalid = coordinator.requestDownloadStart(state, "https://exa mple");
        assertFalse(invalid.started);
        assertEquals(UpdateCoordinator.DownloadStartReason.INVALID_URL, invalid.reason);

        UpdateCoordinator.DownloadDecision insecure = coordinator.requestDownloadStart(state, "http://example.com/app.apk");
        assertFalse(insecure.started);
        assertEquals(UpdateCoordinator.DownloadStartReason.HTTPS_REQUIRED, insecure.reason);
    }

    @Test
    public void requestDownloadStart_whenAcceptedSetsDownloadState() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State state = UpdateCoordinator.State.empty();

        UpdateCoordinator.DownloadDecision started = coordinator.requestDownloadStart(
                state,
                " https://example.com/app.apk ");

        assertTrue(started.started);
        assertEquals(UpdateCoordinator.DownloadStartReason.STARTED, started.reason);
        assertEquals("https://example.com/app.apk", started.normalizedUrl);
        assertTrue(started.nextState.downloadInProgress);
        assertFalse(started.nextState.downloadCancelRequested);
    }

    @Test
    public void cancelAndFinishDownload_transitionStateAsExpected() {
        UpdateCoordinator coordinator = new UpdateCoordinator();
        UpdateCoordinator.State started = coordinator.requestDownloadStart(
                UpdateCoordinator.State.empty(),
                "https://example.com/app.apk").nextState;

        UpdateCoordinator.State cancelRequested = coordinator.requestDownloadCancel(started);
        assertTrue(cancelRequested.downloadInProgress);
        assertTrue(cancelRequested.downloadCancelRequested);

        UpdateCoordinator.State finished = coordinator.markDownloadFinished(cancelRequested);
        assertFalse(finished.downloadInProgress);
        assertFalse(finished.downloadCancelRequested);
    }
}
