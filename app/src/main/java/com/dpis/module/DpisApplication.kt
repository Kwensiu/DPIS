package com.dpis.module

import android.app.Application
import android.content.Context
import com.dpis.module.appconfig.XposedConfigReadiness
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.RuntimeConfigSnapshotPublisher
import com.dpis.module.diagnostics.DpisAppLogStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.DpisLog.AppLogSink
import com.dpis.module.fonts.TypefaceCatalogCache.preload
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.delivery.RuntimePropertyInstallCleanup
import com.dpis.module.runtime.delivery.RuntimePropertyRecoveryCoordinator
import com.dpis.module.runtime.hyperos.HyperOsNativeProxyAssetExporter
import com.dpis.module.updates.presentation.UpdatePackageInstaller
import com.google.android.material.color.DynamicColors
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.libxposed.service.XposedServiceHelper.OnServiceListener
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

class DpisApplication : Application(), OnServiceListener {
    interface ServiceStateListener {
        fun onServiceStateChanged()
    }

    var xposedSelfLoadedByLegacyConstructorHook: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        val appLogStore = DpisAppLogStore(this)
        DpisLog.setAppLogSink(AppLogSink { level: String?, message: String? ->
            appLogStore.record(
                level,
                message
            )
        })
        DpisLog.i("app process started")
        RootAccessProbe.warmUpAsync()
        DynamicColors.applyToActivitiesIfAvailable(this)
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(
            this,
            HyperOsNativeProxyAssetExporter.Logger { msg: String?, throwable: Throwable? ->
                DpisLog.e(
                    msg,
                    throwable
                )
            })
        // Migrate the private catalog before remote config mirroring can touch the old XML.
        ConfigStoreFactory.createLocalFontLibraryStore(this)
        preload(this)
        configStore = ConfigStoreFactory.createLocalModuleConfigStore(this)
        migrateLocalConfigStore(configStore)
        // A reinstall creates a fresh preference namespace but does not remove system properties
        // left by the previous installation. Clear only those per-package mirrors once, before
        // the current store is replayed by the normal recovery coordinator.
        val initializedStore: DpisConfigStore? = configStore
        if (initializedStore == null) {
            DpisLog.e(
                "app config store initialization returned null",
                IllegalStateException("config store unavailable")
            )
            RuntimePropertyInstallCleanup.initializeAsync(this)
        } else {
            DpisLog.setLoggingEnabled(initializedStore.isGlobalLogEnabled())
            RuntimePropertyInstallCleanup.initializeAsync(this, Runnable {
                RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(initializedStore)
            })
        }
        XposedServiceHelper.registerListener(this)
        UpdatePackageInstaller.clearStaleUpdateCache(this, UPDATE_CACHE_STARTUP_MAX_AGE_MS)
    }

    override fun onServiceBind(service: XposedService) {
        val localStore = ConfigStoreFactory.createLocalModuleConfigStore(this)
        migrateLocalConfigStore(localStore)
        configStore = localStore
        xposedService = service
        DpisLog.setLoggingEnabled(localStore.isGlobalLogEnabled())
        publishRuntimeConfig(localStore, service)
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(localStore)
        notifyServiceStateChanged()
    }

    override fun onServiceDied(service: XposedService) {
        configStore = ConfigStoreFactory.createLocalModuleConfigStore(this)
        DpisLog.setLoggingEnabled(configStore!!.isGlobalLogEnabled())
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)
        xposedService = null
        notifyServiceStateChanged()
    }

    companion object {
        private val UPDATE_CACHE_STARTUP_MAX_AGE_MS = 24 * 60 * 60 * 1000L

        init {
            RuntimeConfigDelivery.setLocalSnapshotReloader(Runnable { reloadConfigStore() })
        }

        private val SERVICE_STATE_LISTENERS: MutableSet<ServiceStateListener> =
            CopyOnWriteArraySet<ServiceStateListener>()

        @Volatile
        private var instance: DpisApplication? = null

        @Volatile
        private var configStore: DpisConfigStore? = null

        @Volatile
        @JvmStatic
        var xposedService: XposedService? = null
            private set

        @Volatile
        private var xposedSelfLoaded = false

        @JvmStatic
        fun getConfigStore(): DpisConfigStore {
            return configStore!!
        }

        fun markXposedSelfLoaded() {
            xposedSelfLoaded = true
            notifyServiceStateChanged()
        }

        @JvmStatic
        fun isXposedSelfLoaded(): Boolean {
            val application: DpisApplication? = instance
            return xposedSelfLoaded
                    || (application != null && application.xposedSelfLoadedByLegacyConstructorHook)
        }

        @JvmStatic
        fun clearXposedSelfLoadedForTest() {
            xposedSelfLoaded = false
            val application: DpisApplication? = instance
            if (application != null) {
                application.xposedSelfLoadedByLegacyConstructorHook = false
            }
        }

        @JvmStatic
        fun isXposedConfigReady(): Boolean {
            val flavor = BuildConfig.FLAVOR
            val remoteConfigAvailable =
                flavor == XposedConfigReadiness.MODERN_FLAVOR &&
                        ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(xposedService) != null
            return XposedConfigReadiness.isReady(
                flavor,
                remoteConfigAvailable,
                flavor == XposedConfigReadiness.LEGACY_FLAVOR && isXposedSelfLoaded(),
            )
        }

        @JvmStatic
        fun getActiveHookConfigStore(context: Context?): DpisConfigStore? {
            val sharedStore: DpisConfigStore? = configStore
            if (sharedStore != null) {
                return sharedStore
            }
            if (context == null) {
                return null
            }
            return ConfigStoreFactory.createLocalModuleConfigStore(context)
        }

        fun reloadConfigStore() {
            val application: DpisApplication? = instance
            if (application == null) {
                return
            }
            val service: XposedService? = xposedService
            val localStore = ConfigStoreFactory.createLocalModuleConfigStore(application)
            migrateLocalConfigStore(localStore)
            configStore = localStore
            DpisLog.setLoggingEnabled(localStore.isGlobalLogEnabled())
            publishRuntimeConfig(localStore, service)
            RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(localStore)
            notifyServiceStateChanged()
        }

        @JvmStatic
        fun addServiceStateListener(listener: ServiceStateListener, notifyImmediately: Boolean) {
            SERVICE_STATE_LISTENERS.add(listener)
            if (notifyImmediately) {
                listener.onServiceStateChanged()
            }
        }

        @JvmStatic
        fun removeServiceStateListener(listener: ServiceStateListener?) {
            SERVICE_STATE_LISTENERS.remove(listener)
        }

        private fun notifyServiceStateChanged() {
            for (listener in SERVICE_STATE_LISTENERS) {
                listener.onServiceStateChanged()
            }
        }

        private fun migrateLocalConfigStore(store: DpisConfigStore?) {
            if (store == null) {
                return
            }
            store.migrateLegacyWechatDpi()
            store.migrateLegacyPackageConfigToAggregated()
            store.migratePackageConfigToUserScoped()
        }

        private fun publishRuntimeConfig(from: DpisConfigStore, service: XposedService?) {
            if (service == null) return
            val runtimeStore = ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service)
            if (runtimeStore == null) {
                DpisLog.w("Xposed runtime preferences unavailable; local config remains active")
                return
            }
            if (RuntimeConfigSnapshotPublisher.publish(from, runtimeStore)) {
                DpisLog.i(
                    "runtime config projection: source=local, " +
                            "destination=libxposed_remote, result=published",
                )
            } else {
                DpisLog.w("Xposed runtime config publish failed; local config remains active")
            }
        }
    }
}
