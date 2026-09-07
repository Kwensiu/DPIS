package com.dpis.module

import android.app.Application
import android.content.Context
import com.dpis.module.DpisLog.AppLogSink
import com.dpis.module.diagnostics.DpisAppLogStore
import com.dpis.module.fonts.HyperOsNativeProxyAssetExporter
import com.dpis.module.fonts.TypefaceCatalogCache.preload
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.RuntimePropertyRecoveryCoordinator
import com.dpis.module.updates.UpdatePackageInstaller
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
        val initializedStore: DpisConfigStore? = configStore
        if (initializedStore == null) {
            DpisLog.e(
                "app config store initialization returned null",
                IllegalStateException("config store unavailable")
            )
        } else {
            DpisLog.setLoggingEnabled(initializedStore.isGlobalLogEnabled())
            RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(initializedStore)
        }
        XposedServiceHelper.registerListener(this)
        UpdatePackageInstaller.clearStaleUpdateCache(this, UPDATE_CACHE_STARTUP_MAX_AGE_MS)
    }

    override fun onServiceBind(service: XposedService) {
        val localStore = ConfigStoreFactory.createLocalModuleConfigStore(this)
        val runtimeDeliveryStore =
            ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service)
        migrateLocalConfigStore(localStore)
        migrateLocalConfigStore(runtimeDeliveryStore)
        configStore = ConfigStoreFactory.createLocalUiModuleConfigStore(this, service)
        DpisLog.setLoggingEnabled(configStore!!.isGlobalLogEnabled())
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)
        xposedService = service
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
        fun getActiveHookConfigStore(context: Context?): DpisConfigStore? {
            val sharedStore: DpisConfigStore? = configStore
            if (sharedStore != null) {
                return sharedStore
            }
            if (context == null) {
                return null
            }
            val service: XposedService? = xposedService
            return if (service != null)
                ConfigStoreFactory.createLocalUiModuleConfigStore(context, service)
            else
                ConfigStoreFactory.createLocalModuleConfigStore(context)
        }

        fun reloadConfigStore() {
            val application: DpisApplication? = instance
            if (application == null) {
                return
            }
            val service: XposedService? = xposedService
            val localStore = ConfigStoreFactory.createLocalModuleConfigStore(application)
            migrateLocalConfigStore(localStore)
            migrateLocalConfigStore(
                ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(
                    service
                )
            )
            val refreshedStore = if (service != null)
                ConfigStoreFactory.createLocalUiModuleConfigStore(application, service)
            else
                ConfigStoreFactory.createLocalModuleConfigStore(application)
            configStore = refreshedStore
            DpisLog.setLoggingEnabled(refreshedStore.isGlobalLogEnabled())
            RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)
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
        }

        @JvmStatic
        private fun publishRuntimeConfig(from: DpisConfigStore?, to: DpisConfigStore?) {
            if (from == null || to == null || from == to) {
                return
            }
            // LSPosed remote preferences are a runtime delivery copy, not a migration
            // source or backup. Publish only runtime-shared config from the local store.
            val snapshot = from.snapshotRuntimeDelivery()
            to.replaceAll(snapshot)
        }
    }
}
