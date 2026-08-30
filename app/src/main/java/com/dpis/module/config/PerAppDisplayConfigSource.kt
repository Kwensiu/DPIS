package com.dpis.module.config

import android.os.SystemClock
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDecision
import com.dpis.module.runtime.systemserver.PerAppDisplayConfig
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.function.Supplier

class PerAppDisplayConfigSource(
    private val snapshotProvider: SnapshotProvider?,
    private val packageFallbackProvider: PackageFallbackProvider?,
) {
    fun interface SnapshotProvider {
        fun get(): ConfigSnapshot?
    }

    fun interface PackageFallbackProvider {
        fun get(packageName: String): PackageConfigSnapshot?
    }

    constructor(store: DpisConfigStore?) : this(ConfigSnapshotLoader.fromStore(store))
    constructor(snapshot: ConfigSnapshot?) : this(SnapshotProvider { snapshot })
    constructor(snapshotProvider: SnapshotProvider?) : this(snapshotProvider, null)

    fun get(packageName: String?): PerAppDisplayConfig? {
        val snapshot = getSnapshot()
        var packageConfig = snapshot.getPackage(packageName)
        if (packageConfig != null && !packageConfig.dpisEnabled) return null
        getFallbackPackageConfig(packageName)?.let { packageConfig = it }
        if (packageConfig == null || !packageConfig.dpisEnabled) return null

        val runtimeTargetSpec = ViewportPropertyBridge.readTargetSpec(packageName)
        var targetViewportSpec =
            if (runtimeTargetSpec.isEnabled()) runtimeTargetSpec else packageConfig.targetViewportSpec
        val viewportMode = ViewportApplyMode.normalize(packageConfig.targetViewportMode)
        if (viewportMode == ViewportApplyMode.COMPAT || viewportMode == ViewportApplyMode.OFF) {
            targetViewportSpec = ViewportTargetSpec.off()
        }
        val fontConfigured = FontApplyMode.isEnabled(packageConfig.targetFontMode) &&
            packageConfig.targetFontScalePercent != null
        if (!targetViewportSpec.isEnabled() && !fontConfigured) return null

        val hyperOsNativeFlutterEnabled = FontHookDomainDecision
            .isHyperOsNativeFlutterEnabled(snapshot, packageConfig)
        return PerAppDisplayConfig(
            packageName,
            targetViewportSpec,
            viewportMode,
            packageConfig.targetFontScalePercent,
            packageConfig.targetFontMode,
            hyperOsNativeFlutterEnabled,
            packageConfig.hookDomainOverride,
        )
    }

    fun getConfiguredPackages(): MutableSet<String?> = getSnapshot().getConfiguredPackages().toMutableSet()
    fun isSystemServerHooksEnabled(): Boolean = getSnapshot().isSystemServerHooksEnabled()

    private fun getSnapshot(): ConfigSnapshot =
        snapshotProvider?.get() ?: ConfigSnapshot.empty()

    private fun getFallbackPackageConfig(packageName: String?): PackageConfigSnapshot? =
        if (packageFallbackProvider == null || packageName.isNullOrBlank()) {
            null
        } else {
            packageFallbackProvider.get(packageName)
        }

    companion object {
        @JvmStatic
        fun withLegacyRuntimePropertyFallback(
            snapshotProvider: SnapshotProvider?,
        ) = PerAppDisplayConfigSource(
            snapshotProvider,
            PackageFallbackProvider { packageName ->
                ConfigSnapshotLoader.fromStore(
                    DpisConfigStore(RuntimePropertyConfigPreferences(packageName)),
                ).getPackage(packageName)
            },
        )
    }

    class RefreshingSnapshotProvider @JvmOverloads constructor(
        private val loader: Supplier<ConfigSnapshot>?,
        ttlMillis: Long,
        private val clock: Clock = Clock { SystemClock.elapsedRealtime() },
    ) : SnapshotProvider {
        fun interface Clock {
            fun nowMillis(): Long
        }

        private val ttlMillis = ttlMillis.coerceAtLeast(0L)
        @Volatile private var snapshot: ConfigSnapshot? = null
        @Volatile private var loadedAtMillis = 0L
        @Volatile private var lastRefreshAttemptAtMillis = Long.MIN_VALUE
        @Volatile private var lastFailureLoggedAtMillis = 0L
        @Volatile private var lastFailureMessage: String? = null

        override fun get(): ConfigSnapshot? {
            var now = clock.nowMillis()
            var current = snapshot
            if (!shouldRefresh(current, now)) return current
            synchronized(this) {
                now = clock.nowMillis()
                current = snapshot
                if (!shouldRefresh(current, now)) return current
                return try {
                    lastRefreshAttemptAtMillis = now
                    snapshot = loader?.get() ?: ConfigSnapshot.empty()
                    loadedAtMillis = now
                    lastFailureMessage = null
                    snapshot
                } catch (throwable: Throwable) {
                    logFailureIfNeeded(throwable, now)
                    current ?: ConfigSnapshot.empty()
                }
            }
        }

        private fun shouldRefresh(current: ConfigSnapshot?, now: Long): Boolean =
            if (ttlMillis == 0L) true
            else if (current != null) now - loadedAtMillis >= ttlMillis
            else lastRefreshAttemptAtMillis == Long.MIN_VALUE ||
                now - lastRefreshAttemptAtMillis >= ttlMillis

        private fun logFailureIfNeeded(throwable: Throwable, now: Long) {
            val message = "${throwable.javaClass.name}: ${throwable.message}"
            if (message == lastFailureMessage &&
                now - lastFailureLoggedAtMillis < FAILURE_LOG_MIN_INTERVAL_MILLIS
            ) return
            lastFailureMessage = message
            lastFailureLoggedAtMillis = now
            DpisLog.e("config snapshot refresh failed: $message", throwable)
        }

        companion object {
            private const val FAILURE_LOG_MIN_INTERVAL_MILLIS = 30_000L
        }
    }
}
