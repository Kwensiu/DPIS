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

    interface Clock {
        long currentTimeMillis();
    }

    interface ManifestFetcher {
        StartupUpdateManifest fetch(String url, int connectTimeoutMs, int readTimeoutMs)
                throws Exception;
    }

    private final Host host;
    private final UpdateCoordinator updateCoordinator;
    private final Clock clock;
    private final ManifestFetcher manifestFetcher;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    StartupUpdateCheckCoordinator(Host host,
            UpdateCoordinator updateCoordinator,
            int connectTimeoutMs,
            int readTimeoutMs) {
        this(host, updateCoordinator, System::currentTimeMillis,
                UpdateManifestFetcher::fetch, connectTimeoutMs, readTimeoutMs);
    }

    StartupUpdateCheckCoordinator(Host host,
            UpdateCoordinator updateCoordinator,
            Clock clock,
            ManifestFetcher manifestFetcher,
            int connectTimeoutMs,
            int readTimeoutMs) {
        this.host = host;
        this.updateCoordinator = updateCoordinator;
        this.clock = clock;
        this.manifestFetcher = manifestFetcher;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    void maybeCheckForUpdatesOnStartup() {
        if (!host.isActivityAlive()) {
            return;
        }
        UpdateCoordinator.State state = host.buildUpdateCoordinatorState();
        UpdateCoordinator.StartupCheckGate gate =
                updateCoordinator.evaluateStartupCheck(state, clock.currentTimeMillis());
        if (!gate.shouldStart) {
            return;
        }
        UpdateCoordinator.State checkingState = updateCoordinator.markStartupCheckStarted(state);
        host.applyStartupCheckState(checkingState);

        final String manifestUrl = host.getManifestUrl();
        host.executeBackground(() -> {
            boolean requestSucceeded = false;
            try {
                StartupUpdateManifest manifest = manifestFetcher.fetch(
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
                // Non-fatal: startup update check failures are silently swallowed.
            } finally {
                UpdateCoordinator.State nextState = updateCoordinator.markStartupCheckFinished(
                        host.buildUpdateCoordinatorState(),
                        clock.currentTimeMillis(),
                        requestSucceeded);
                host.applyStartupCheckState(nextState);
            }
        });
    }
}
