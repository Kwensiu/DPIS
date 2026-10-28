package com.dpis.module.config

import android.os.SystemClock
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDecision
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.function.Supplier

data class ConfigLookupDiagnostics(
    val identity: PackageIdentity,
    val snapshotOwnerUserId: Int,
    val snapshotOwnerMatches: Boolean,
    val snapshotPackageHit: Boolean,
    val fallbackAttempted: Boolean,
    val fallbackHit: Boolean,
    val config: PerAppDisplayConfig?,
    val reason: String,
) {
    val targetViewportSpec = config?.targetViewportSpec
    val targetViewportMode = config?.targetViewportMode
}

class PerAppDisplayConfigSource(
    private val snapshotProvider: SnapshotProvider?,
    private val packageFallbackProvider: PackageFallbackProvider?,
) {
    fun interface SnapshotProvider {
        fun get(): ConfigSnapshot?
    }

    fun interface PackageFallbackProvider {
        fun get(packageName: String, userId: Int): PackageConfigSnapshot?
    }


    constructor(store: DpisConfigStore?) : this(ConfigSnapshotLoader.fromStore(store))
    constructor(snapshot: ConfigSnapshot?) : this(SnapshotProvider { snapshot })
    constructor(snapshotProvider: SnapshotProvider?) : this(snapshotProvider, null)

    fun get(packageName: String?, userId: Int): PerAppDisplayConfig? {
        if (packageName.isNullOrBlank() || userId < 0) return null
        return get(PackageIdentity(packageName, userId))
    }

    fun get(identity: PackageIdentity): PerAppDisplayConfig? =
        inspect(identity).config

    fun inspect(identity: PackageIdentity): ConfigLookupDiagnostics {
        val packageName = identity.packageName
        val userId = identity.userId
        val snapshot = getSnapshot()
        val snapshotOwnerMatches = snapshot.ownerUserId == userId
        val snapshotPackageHit = snapshot.getPackage(packageName) != null
        var packageConfig = if (snapshotOwnerMatches) {
            snapshot.getPackage(packageName, userId)
        } else {
            null
        }
        if (packageConfig != null && !packageConfig.dpisEnabled) {
            return ConfigLookupDiagnostics(
                identity,
                snapshot.ownerUserId,
                snapshotOwnerMatches,
                snapshotPackageHit,
                false,
                false,
                null,
                "snapshot_disabled",
            )
        }
        val fallbackAttempted = packageFallbackProvider != null
        var fallbackHit = false
        getFallbackPackageConfig(packageName, userId)?.let {
            fallbackHit = true
            packageConfig = it
        }
        if (packageConfig == null) {
            return ConfigLookupDiagnostics(
                identity,
                snapshot.ownerUserId,
                snapshotOwnerMatches,
                snapshotPackageHit,
                fallbackAttempted,
                fallbackHit,
                null,
                if (!snapshotOwnerMatches && snapshotPackageHit) {
                    "snapshot_owner_mismatch"
                } else {
                    "package_missing"
                },
            )
        }
        if (!packageConfig.dpisEnabled) {
            return ConfigLookupDiagnostics(
                identity,
                snapshot.ownerUserId,
                snapshotOwnerMatches,
                snapshotPackageHit,
                fallbackAttempted,
                fallbackHit,
                null,
                "fallback_disabled",
            )
        }

        val runtimeTargetSpec = ViewportPropertyBridge.readTargetSpec(packageName, userId)
        var targetViewportSpec = if (runtimeTargetSpec.isEnabled()) {
            runtimeTargetSpec
        } else {
            packageConfig.targetViewportSpec
        }
        val viewportMode = ViewportApplyMode.normalize(packageConfig.targetViewportMode)
        // Explicit compat is not delivered to system_server.
        //
        // On 2026-10-12, compat ratio and width modes stretched the right and
        // bottom edges of video covers. The stretch stopped when modern
        // system_server applied the compat viewport at launch from the physical
        // display's max bounds, so the process started at the target density.
        // Writing Display density, aligning the process bitmap density, and
        // scaling fullscreen height from max bounds inside the app did not.
        //
        // That launch write is not kept. Compat is for a process that does not
        // hook the system framework. The fix works only when system_server is
        // hooked, so keeping it would make compat depend on the hook it exists
        // to avoid. System and AUTO already launch through system_server when
        // that hook is enabled.
        if (viewportMode == ViewportApplyMode.COMPAT || viewportMode == ViewportApplyMode.OFF) {
            targetViewportSpec = ViewportTargetSpec.off()
        }
        val fontConfigured = FontApplyMode.isEnabled(packageConfig.targetFontMode) &&
            packageConfig.targetFontScalePercent != null
        if (!targetViewportSpec.isEnabled() && !fontConfigured) {
            return ConfigLookupDiagnostics(
                identity,
                snapshot.ownerUserId,
                snapshotOwnerMatches,
                snapshotPackageHit,
                fallbackAttempted,
                fallbackHit,
                null,
                "no_effective_fields",
            )
        }

        val hyperOsNativeFlutterEnabled = FontHookDomainDecision
            .isHyperOsNativeFlutterEnabled(snapshot, packageConfig)
        val config = PerAppDisplayConfig(
            packageName,
            targetViewportSpec,
            viewportMode,
            packageConfig.targetFontScalePercent,
            packageConfig.targetFontMode,
            hyperOsNativeFlutterEnabled,
            packageConfig.hookDomainOverride,
        )
        return ConfigLookupDiagnostics(
            identity,
            snapshot.ownerUserId,
            snapshotOwnerMatches,
            snapshotPackageHit,
            fallbackAttempted,
            fallbackHit,
            config,
            "matched",
        )
    }

    fun getConfiguredPackages(): MutableSet<String?> = getSnapshot().getConfiguredPackages().toMutableSet()
    fun isSystemServerHooksEnabled(): Boolean = getSnapshot().isSystemServerHooksEnabled()

    private fun getSnapshot(): ConfigSnapshot =
        snapshotProvider?.get() ?: ConfigSnapshot.empty()

    private fun getFallbackPackageConfig(
        packageName: String?,
        userId: Int
    ): PackageConfigSnapshot? =
        if (packageFallbackProvider == null || packageName.isNullOrBlank()) {
            null
        } else {
            packageFallbackProvider.get(packageName, userId)
        }

    companion object {
        @JvmStatic
        fun withLegacyRuntimePropertyFallback(
            snapshotProvider: SnapshotProvider?,
        ) = PerAppDisplayConfigSource(
            snapshotProvider,
            PackageFallbackProvider { packageName, userId ->
                ConfigSnapshotLoader.fromStore(
                    DpisConfigStore(
                        RuntimePropertyConfigPreferences(packageName, userId = userId),
                        userId,
                    ),
                ).getPackage(packageName, userId)
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
