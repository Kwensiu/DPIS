package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StartupUpdateCheckCoordinatorTest {

    private static final long SUCCESS_INTERVAL =
            UpdateCoordinator.DEFAULT_STARTUP_CHECK_INTERVAL_MS;
    private static final long FAILURE_INTERVAL =
            UpdateCoordinator.DEFAULT_FAILURE_RETRY_INTERVAL_MS;

    @Test
    public void blocksInsideSuccessfulCheckInterval() {
        long lastCheck = 1000L;
        long now = lastCheck + SUCCESS_INTERVAL - 1;
        FakeHost host = hostWithState(lastCheck, false, 0, false);
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, now, null);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(0, host.backgroundExecutionCount);
    }

    @Test
    public void blocksInsideFailureRetryInterval() {
        long lastCheck = 1000L;
        long now = lastCheck + FAILURE_INTERVAL - 1;
        FakeHost host = hostWithState(lastCheck, true, 0, false);
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, now, null);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(0, host.backgroundExecutionCount);
    }

    @Test
    public void blocksWhenAlreadyInProgress() {
        FakeHost host = hostWithState(0L, false, 0, true);
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, 1000L, null);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(0, host.backgroundExecutionCount);
    }

    @Test
    public void allowedSuccess_marksFinishedAndPrompts() {
        long now = SUCCESS_INTERVAL + 1;
        FakeHost host = hostWithState(0L, false, 0, false);
        host.localVersionCode = 1;
        host.localVersionName = "1.0.0";
        StartupUpdateManifest remoteManifest = new StartupUpdateManifest("2.0.0", 2, "", "", "");
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, now, remoteManifest);

        coordinator.maybeCheckForUpdatesOnStartup();
        runBackground(host);

        assertFalse(host.state.startupCheckInProgress);
        assertFalse(host.state.lastUpdateCheckFailed);
        assertEquals(now, host.state.lastUpdateCheckTimestampMs);
        assertEquals(1, host.dialogLaunchCount);
    }

    @Test
    public void fetchFailure_marksFailedAndDoesNotPrompt() {
        long now = SUCCESS_INTERVAL + 1;
        FakeHost host = hostWithState(0L, false, 0, false);
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, now, null);

        coordinator.maybeCheckForUpdatesOnStartup();
        runBackground(host);

        assertFalse(host.state.startupCheckInProgress);
        assertTrue(host.state.lastUpdateCheckFailed);
        assertEquals(now, host.state.lastUpdateCheckTimestampMs);
        assertEquals(0, host.dialogLaunchCount);
    }

    @Test
    public void remoteNotNewer_marksSuccessAndDoesNotPrompt() {
        long now = SUCCESS_INTERVAL + 1;
        FakeHost host = hostWithState(0L, false, 0, false);
        host.localVersionCode = 5;
        host.localVersionName = "5.0.0";
        StartupUpdateManifest remoteManifest = new StartupUpdateManifest("5.0.0", 5, "", "", "");
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, now, remoteManifest);

        coordinator.maybeCheckForUpdatesOnStartup();
        runBackground(host);

        assertFalse(host.state.startupCheckInProgress);
        assertFalse(host.state.lastUpdateCheckFailed);
        assertEquals(now, host.state.lastUpdateCheckTimestampMs);
        assertEquals(0, host.dialogLaunchCount);
    }

    @Test
    public void activityNotAlive_doesNothing() {
        FakeHost host = hostWithState(0L, false, 0, false);
        host.alive = false;
        StartupUpdateCheckCoordinator coordinator = buildCoordinator(host, SUCCESS_INTERVAL + 1, null);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(0, host.backgroundExecutionCount);
    }

    private static StartupUpdateCheckCoordinator buildCoordinator(
            FakeHost host, long clockValue, StartupUpdateManifest manifest) {
        return new StartupUpdateCheckCoordinator(
                host,
                new UpdateCoordinator(),
                () -> clockValue,
                manifest != null
                        ? (url, ct, rt) -> manifest
                        : (url, ct, rt) -> { throw new RuntimeException("fetch failed"); },
                1_000,
                1_000);
    }

    private static FakeHost hostWithState(long timestamp, boolean failed, int prompted, boolean inProgress) {
        return new FakeHost(new UpdateCoordinator.State(timestamp, failed, prompted, inProgress, false, false));
    }

    private static void runBackground(FakeHost host) {
        if (host.lastBackgroundRunnable != null) {
            host.lastBackgroundRunnable.run();
        }
    }

    private static final class FakeHost implements StartupUpdateCheckCoordinator.Host {
        UpdateCoordinator.State state;
        int backgroundExecutionCount;
        int dialogLaunchCount;
        Runnable lastBackgroundRunnable;
        boolean alive = true;
        int localVersionCode = 1;
        String localVersionName = "1.0.0";

        FakeHost(UpdateCoordinator.State state) {
            this.state = state;
        }

        @Override public boolean isActivityAlive() { return alive; }
        @Override public String getManifestUrl() { return "https://example.com/manifest.json"; }

        @Override
        public void executeBackground(Runnable runnable) {
            backgroundExecutionCount++;
            lastBackgroundRunnable = runnable;
        }

        @Override public void runOnUiThread(Runnable runnable) { runnable.run(); }
        @Override public UpdateCoordinator.State buildUpdateCoordinatorState() { return state; }
        @Override public void applyStartupCheckState(UpdateCoordinator.State state) { this.state = state; }
        @Override public int getLocalVersionCode() { return localVersionCode; }
        @Override public String getLocalVersionName() { return localVersionName; }

        @Override
        public void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
            dialogLaunchCount++;
        }
    }
}
