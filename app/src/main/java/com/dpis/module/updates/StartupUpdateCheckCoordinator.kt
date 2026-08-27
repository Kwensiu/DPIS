package com.dpis.module.updates

class StartupUpdateCheckCoordinator(
    private val host: Host,
    private val updateCoordinator: UpdateCoordinator,
    private val clock: Clock,
    private val manifestFetcher: ManifestFetcher,
    private val connectTimeoutMs: Int,
    private val readTimeoutMs: Int
) {
    interface Host {
        fun isActivityAlive(): Boolean
        fun getManifestUrl(): String
        fun executeBackground(runnable: Runnable)
        fun runOnUiThread(runnable: Runnable)
        fun buildUpdateCoordinatorState(): UpdateCoordinator.State
        fun applyStartupCheckState(state: UpdateCoordinator.State)
        fun getLocalVersionCode(): Int
        fun getLocalVersionName(): String
        fun onStartupUpdateCheckStarted()
        fun onStartupUpdateAvailable(manifest: StartupUpdateManifest)
        fun onStartupUpdateUpToDate()
        fun onStartupUpdateCheckFailed()
    }

    fun interface Clock {
        fun currentTimeMillis(): Long
    }

    fun interface ManifestFetcher {
        @Throws(Exception::class)
        fun fetch(url: String, connectTimeoutMs: Int, readTimeoutMs: Int): StartupUpdateManifest
    }

    constructor(
        host: Host,
        updateCoordinator: UpdateCoordinator,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ) : this(
        host,
        updateCoordinator,
        Clock { System.currentTimeMillis() },
        ManifestFetcher { url, connect, read -> UpdateManifestFetcher.fetch(url, connect, read) },
        connectTimeoutMs,
        readTimeoutMs
    )

    fun maybeCheckForUpdatesOnStartup() = checkForUpdates(true)
    fun checkForUpdatesNow() = checkForUpdates(true)

    private fun checkForUpdates(ignoreGate: Boolean) {
        if (!host.isActivityAlive()) return
        val state = host.buildUpdateCoordinatorState()
        val gate = updateCoordinator.evaluateStartupCheck(state, clock.currentTimeMillis())
        if (gate.reason == UpdateCoordinator.StartupCheckReason.CHECK_IN_PROGRESS) {
            host.runOnUiThread(Runnable { host.onStartupUpdateCheckStarted() })
            return
        }
        if (!ignoreGate && !gate.shouldStart) {
            if (state.lastUpdateCheckFailed) {
                host.runOnUiThread(Runnable { host.onStartupUpdateCheckFailed() })
            } else {
                host.runOnUiThread(Runnable { host.onStartupUpdateUpToDate() })
            }
            return
        }

        host.applyStartupCheckState(updateCoordinator.markStartupCheckStarted(state))
        host.runOnUiThread(Runnable { host.onStartupUpdateCheckStarted() })
        host.executeBackground(Runnable {
            var requestSucceeded = false
            try {
                val manifest = manifestFetcher.fetch(
                    host.getManifestUrl(), connectTimeoutMs, readTimeoutMs)
                requestSucceeded = true
                val remoteNewer = UpdateCoordinator.isRemoteVersionNewer(
                    manifest.versionCode,
                    manifest.versionName,
                    host.getLocalVersionCode(),
                    host.getLocalVersionName()
                )
                if (!remoteNewer) {
                    host.runOnUiThread(Runnable { host.onStartupUpdateUpToDate() })
                } else {
                    host.runOnUiThread(Runnable { host.onStartupUpdateAvailable(manifest) })
                }
            } catch (_: Exception) {
                host.runOnUiThread(Runnable { host.onStartupUpdateCheckFailed() })
            } finally {
                host.applyStartupCheckState(
                    updateCoordinator.markStartupCheckFinished(
                        host.buildUpdateCoordinatorState(),
                        clock.currentTimeMillis(),
                        requestSucceeded
                    )
                )
            }
        })
    }
}
