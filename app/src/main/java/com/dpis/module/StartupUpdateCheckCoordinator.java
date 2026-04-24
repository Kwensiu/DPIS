package com.dpis.module;

final class StartupUpdateCheckCoordinator {
    interface Host {
        boolean isActivityAlive();

        String getManifestUrl();

        void executeBackground(Runnable runnable);

        void runOnUiThread(Runnable runnable);

        UpdateCoordinator.State buildUpdateCoordinatorState();

        void applyStartupCheckState(UpdateCoordinator.State state);

        int getLocalVersionCode();

        String getLocalVersionName();

        void launchStartupUpdateDialog(StartupUpdateManifest manifest);
    }

    private final Host host;
    private final UpdateCoordinator updateCoordinator;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    StartupUpdateCheckCoordinator(Host host,
            UpdateCoordinator updateCoordinator,
            int connectTimeoutMs,
            int readTimeoutMs) {
        this.host = host;
        this.updateCoordinator = updateCoordinator;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    void maybeCheckForUpdatesOnStartup() {
        if (!host.isActivityAlive()) {
            return;
        }
        UpdateCoordinator.State state = host.buildUpdateCoordinatorState();
        if (state.startupCheckInProgress) {
            return;
        }
        UpdateCoordinator.State checkingState = updateCoordinator.markStartupCheckStarted(state);
        host.applyStartupCheckState(checkingState);

        final String manifestUrl = host.getManifestUrl();
        host.executeBackground(() -> {
            boolean requestSucceeded = false;
            try {
                StartupUpdateManifest manifest = UpdateManifestFetcher.fetch(
                        manifestUrl,
                        connectTimeoutMs,
                        readTimeoutMs);
                requestSucceeded = true;
                UpdateCoordinator.PromptDecision promptDecision = updateCoordinator.evaluatePromptDecision(
                        host.buildUpdateCoordinatorState(),
                        manifest.versionCode,
                        manifest.versionName,
                        host.getLocalVersionCode(),
                        host.getLocalVersionName());
                if (!promptDecision.shouldPrompt) {
                    return;
                }
                host.runOnUiThread(() -> host.launchStartupUpdateDialog(manifest));
            } catch (Exception ignored) {
                // Ignore startup update check failures silently.
            } finally {
                UpdateCoordinator.State nextState = updateCoordinator.markStartupCheckFinished(
                        host.buildUpdateCoordinatorState(),
                        System.currentTimeMillis(),
                        requestSucceeded);
                host.applyStartupCheckState(nextState);
            }
        });
    }
}
