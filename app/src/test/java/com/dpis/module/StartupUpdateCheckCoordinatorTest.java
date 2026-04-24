package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StartupUpdateCheckCoordinatorTest {
    @Test
    public void maybeCheck_ignoresIntervalWindowAndStartsOnFreshState() {
        FakeHost host = new FakeHost(new UpdateCoordinator.State(
                System.currentTimeMillis(),
                false,
                0,
                false,
                false,
                false));
        StartupUpdateCheckCoordinator coordinator = new StartupUpdateCheckCoordinator(
                host,
                new UpdateCoordinator(),
                1_000,
                1_000);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(1, host.backgroundExecutionCount);
    }

    @Test
    public void maybeCheck_blocksWhenAlreadyInProgress() {
        FakeHost host = new FakeHost(new UpdateCoordinator.State(
                0L,
                false,
                0,
                true,
                false,
                false));
        StartupUpdateCheckCoordinator coordinator = new StartupUpdateCheckCoordinator(
                host,
                new UpdateCoordinator(),
                1_000,
                1_000);

        coordinator.maybeCheckForUpdatesOnStartup();

        assertEquals(0, host.backgroundExecutionCount);
    }

    private static final class FakeHost implements StartupUpdateCheckCoordinator.Host {
        private UpdateCoordinator.State state;
        int backgroundExecutionCount;

        FakeHost(UpdateCoordinator.State state) {
            this.state = state;
        }

        @Override
        public boolean isActivityAlive() {
            return true;
        }

        @Override
        public String getManifestUrl() {
            return "https://example.com/manifest.json";
        }

        @Override
        public void executeBackground(Runnable runnable) {
            backgroundExecutionCount++;
        }

        @Override
        public void runOnUiThread(Runnable runnable) {
        }

        @Override
        public UpdateCoordinator.State buildUpdateCoordinatorState() {
            return state;
        }

        @Override
        public void applyStartupCheckState(UpdateCoordinator.State state) {
            this.state = state;
        }

        @Override
        public int getLocalVersionCode() {
            return 1;
        }

        @Override
        public String getLocalVersionName() {
            return "1.0.0";
        }

        @Override
        public void launchStartupUpdateDialog(StartupUpdateManifest manifest) {
        }
    }
}
