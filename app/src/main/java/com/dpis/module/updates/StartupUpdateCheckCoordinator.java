package com.dpis.module.updates;

public final class StartupUpdateCheckCoordinator {
    public interface Host {
        boolean isActivityAlive();

        String getManifestUrl();

        void executeBackground(Runnable runnable);

        void runOnUiThread(Runnable runnable);

        UpdateCoordinator.State buildUpdateCoordinatorState();

        void applyStartupCheckState(UpdateCoordinator.State state);

        int getLocalVersionCode();

        String getLocalVersionName();

        void onStartupUpdateCheckStarted();

        void onStartupUpdateAvailable(StartupUpdateManifest manifest);

        void onStartupUpdateUpToDate();

        void onStartupUpdateCheckFailed();
    }

    public interface Clock {
        long currentTimeMillis();
    }

    public interface ManifestFetcher {
        StartupUpdateManifest fetch(String url, int connectTimeoutMs, int readTimeoutMs)
                throws Exception;
    }

    private final Host host;
    private final UpdateCoordinator updateCoordinator;
    private final Clock clock;
    private final ManifestFetcher manifestFetcher;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public StartupUpdateCheckCoordinator(Host host,
            UpdateCoordinator updateCoordinator,
            int connectTimeoutMs,
            int readTimeoutMs) {
        this(host, updateCoordinator, System::currentTimeMillis,
                UpdateManifestFetcher::fetch, connectTimeoutMs, readTimeoutMs);
    }

    public StartupUpdateCheckCoordinator(Host host,
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

    public void maybeCheckForUpdatesOnStartup() {
        checkForUpdates(true);
    }

    public void checkForUpdatesNow() {
        checkForUpdates(true);
    }

    private void checkForUpdates(boolean ignoreGate) {
        if (!host.isActivityAlive()) {
            return;
        }
        UpdateCoordinator.State state = host.buildUpdateCoordinatorState();
        UpdateCoordinator.StartupCheckGate gate =
                updateCoordinator.evaluateStartupCheck(state, clock.currentTimeMillis());
        if (gate.reason == UpdateCoordinator.StartupCheckReason.CHECK_IN_PROGRESS) {
            host.runOnUiThread(host::onStartupUpdateCheckStarted);
            return;
        }
        if (!ignoreGate && !gate.shouldStart) {
            if (gate.reason == UpdateCoordinator.StartupCheckReason.CHECK_IN_PROGRESS) {
                host.runOnUiThread(host::onStartupUpdateCheckStarted);
            } else if (state.lastUpdateCheckFailed) {
                host.runOnUiThread(host::onStartupUpdateCheckFailed);
            } else {
                host.runOnUiThread(host::onStartupUpdateUpToDate);
            }
            return;
        }
        UpdateCoordinator.State checkingState = updateCoordinator.markStartupCheckStarted(state);
        host.applyStartupCheckState(checkingState);
        host.runOnUiThread(host::onStartupUpdateCheckStarted);

        final String manifestUrl = host.getManifestUrl();
        host.executeBackground(() -> {
            boolean requestSucceeded = false;
            try {
                StartupUpdateManifest manifest = manifestFetcher.fetch(
                        manifestUrl,
                        connectTimeoutMs,
                        readTimeoutMs);
                requestSucceeded = true;
                boolean remoteNewer = UpdateCoordinator.isRemoteVersionNewer(
                        manifest.versionCode,
                        manifest.versionName,
                        host.getLocalVersionCode(),
                        host.getLocalVersionName());
                if (!remoteNewer) {
                    host.runOnUiThread(host::onStartupUpdateUpToDate);
                    return;
                }
                host.runOnUiThread(() -> host.onStartupUpdateAvailable(manifest));
            } catch (Exception ignored) {
                host.runOnUiThread(host::onStartupUpdateCheckFailed);
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
