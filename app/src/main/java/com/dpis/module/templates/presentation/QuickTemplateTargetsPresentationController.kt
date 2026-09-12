package com.dpis.module.templates.presentation

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.templates.QuickTemplateTargetSelectionPolicy
import java.util.LinkedHashSet
import java.util.concurrent.Executors

/**
 * Presentation state for quick-template target selection, shared by portrait and landscape UI.
 * Selection is intentionally draft-only until [save] succeeds.
 */
class QuickTemplateTargetsPresentationController(private val context: Context) {
    fun interface Listener {
        fun onStateChanged(state: State)
    }

    class TargetApp(
        @JvmField val label: String,
        @JvmField val packageName: String,
        @JvmField val configured: Boolean,
        @JvmField val systemApp: Boolean,
        @JvmField val icon: Drawable?,
        @JvmField val selected: Boolean,
        @JvmField val firstInstallTime: Long,
        @JvmField val lastUpdateTime: Long,
    ) {
        fun sortable() = QuickTemplateTargetSelectionPolicy.SortableTarget(
            label,
            packageName,
            configured,
            firstInstallTime,
            lastUpdateTime,
        )
    }

    class State(
        @JvmField val templateId: String?,
        @JvmField val templateName: String?,
        @JvmField val apps: List<TargetApp>,
        @JvmField val selectedCount: Int,
        @JvmField val hasUnsavedChanges: Boolean,
        @JvmField val query: String,
        @JvmField val showSystemApps: Boolean,
        @JvmField val showUserApps: Boolean,
        @JvmField val showAllApps: Boolean,
        @JvmField val showConfiguredApps: Boolean,
        @JvmField val sortMode: Int,
        @JvmField val reverseOrder: Boolean,
        @JvmField val loading: Boolean,
        @JvmField val missingTemplate: Boolean,
    )

    class SaveResult(
        @JvmField val success: Boolean,
        @JvmField val messageResId: Int,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val templates = QuickTemplateStore(context)
    private val filters = context.getSharedPreferences(FILTER_PREFS_NAME, Context.MODE_PRIVATE)
    private val packageConfigs = PackageConfigRepository(DpisApplication.getActiveHookConfigStore(context))
    private val loader = Executors.newSingleThreadExecutor()
    private val catalog = InstalledAppCatalogCoordinator(
        object : InstalledAppCatalogCoordinator.Host {
            override fun getPackageManager() = context.packageManager
            override fun getSelfPackageName() = context.packageName
        },
        60_000L,
    )
    private val listeners = LinkedHashSet<Listener>()
    private val allApps = ArrayList<RawTargetApp>()
    private val selectedPackages = LinkedHashSet<String>()
    // Kept stable for the session so changing a checkbox does not move list rows mid-scroll.
    private val savedSelectedPackages = LinkedHashSet<String>()

    private var templateId: String? = null
    private var templateName: String? = null
    private var query = ""
    private var showSystemApps = filters.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
    private var showUserApps = filters.getBoolean(KEY_SHOW_USER_APPS, false)
    private var showAllApps = filters.getBoolean(KEY_SHOW_ALL_APPS, true)
    private var showConfiguredApps = filters.getBoolean(KEY_SHOW_CONFIGURED_APPS, true)
    private var sortMode = filters.getInt(KEY_SORT_MODE, SORT_NAME)
    private var reverseOrder = filters.getBoolean(KEY_REVERSE_ORDER, false)
    private var loading = false
    private var missingTemplate = false
    private var requestId = 0
    private var disposed = false

    fun addListener(listener: Listener?) {
        if (listener != null) listeners.add(listener)
    }

    fun removeListener(listener: Listener?) {
        listeners.remove(listener)
    }

    fun load(id: String?) {
        templateId = id
        val template = templates.read(id)
        if (template == null) {
            missingTemplate = true
            templateName = null
            selectedPackages.clear()
            savedSelectedPackages.clear()
            loading = false
            publish()
            return
        }
        missingTemplate = false
        templateName = template.name
        selectedPackages.clear()
        selectedPackages.addAll(template.selectedPackages.filterNotNull())
        savedSelectedPackages.clear()
        savedSelectedPackages.addAll(template.selectedPackages.filterNotNull())
        reloadApps()
    }

    fun setQuery(value: String?) {
        query = value.orEmpty()
        publish()
    }

    fun setFilters(
        showAll: Boolean,
        showSystem: Boolean,
        showUser: Boolean,
        showConfigured: Boolean,
        sortMode: Int,
        reverseOrder: Boolean,
    ) {
        showAllApps = showAll
        showSystemApps = showSystem
        showUserApps = showUser
        showConfiguredApps = showConfigured
        this.sortMode = sortMode
        this.reverseOrder = reverseOrder
        filters.edit()
            .putBoolean(KEY_SHOW_SYSTEM_APPS, showSystem)
            .putBoolean(KEY_SHOW_USER_APPS, showUser)
            .putBoolean(KEY_SHOW_ALL_APPS, showAll)
            .putBoolean(KEY_SHOW_CONFIGURED_APPS, showConfigured)
            .putInt(KEY_SORT_MODE, sortMode)
            .putBoolean(KEY_REVERSE_ORDER, reverseOrder)
            .apply()
        publish()
    }

    fun toggleSelection(packageName: String, selected: Boolean) {
        if (selected) selectedPackages.add(packageName) else selectedPackages.remove(packageName)
        publish()
    }

    fun save(): SaveResult {
        val id = templateId
        val saved = id != null && templates.setSelectedPackages(id, LinkedHashSet<String?>(selectedPackages))
        if (saved) {
            savedSelectedPackages.clear()
            savedSelectedPackages.addAll(selectedPackages)
        }
        return SaveResult(
            saved,
            if (saved) R.string.quick_template_targets_save_success
            else R.string.quick_template_targets_save_failed,
        )
    }

    fun dispose() {
        disposed = true
        loader.shutdownNow()
        catalog.shutdown()
    }

    private fun reloadApps() {
        if (disposed || missingTemplate) return
        val currentRequest = ++requestId
        loading = true
        publish()
        loader.execute {
            var loaded: List<RawTargetApp>? = null
            try {
                val next = ArrayList<RawTargetApp>()
                for (item in catalog.loadInstalledAppCatalogWithIcons(false)) {
                    next.add(
                        RawTargetApp(
                            item.label,
                            item.packageName,
                            packageConfigs.hasRealPackageConfig(item.packageName),
                            item.systemApp,
                            item.icon,
                            item.firstInstallTime,
                            item.lastUpdateTime,
                        ),
                    )
                }
                loaded = next
            } catch (throwable: Throwable) {
                // Loading failure is a presentation state, not a reason to strand the page in
                // its progress state. Keep the last complete list and converge loading below.
                DpisLog.e("quick template target presentation load failed", throwable)
            }
            val finalLoaded = loaded
            mainHandler.post {
                if (disposed || currentRequest != requestId) return@post
                if (finalLoaded != null) {
                    allApps.clear()
                    allApps.addAll(finalLoaded)
                    pruneSelection()
                }
                loading = false
                publish()
            }
        }
    }

    private fun pruneSelection() {
        val installed = LinkedHashSet<String>()
        for (item in allApps) installed.add(item.packageName)
        QuickTemplateTargetSelectionPolicy.retainInstalled(selectedPackages, installed)
        QuickTemplateTargetSelectionPolicy.retainInstalled(savedSelectedPackages, installed)
    }

    private fun publish() {
        val visible = ArrayList<TargetApp>()
        for (item in allApps) {
            if (!QuickTemplateTargetSelectionPolicy.matches(
                    item.label,
                    item.packageName,
                    item.configured,
                    item.systemApp,
                    query,
                    showAllApps,
                    showSystemApps,
                    showUserApps,
                    showConfiguredApps,
                )
            ) {
                continue
            }
            visible.add(
                TargetApp(
                    item.label,
                    item.packageName,
                    item.configured,
                    item.systemApp,
                    item.icon,
                    selectedPackages.contains(item.packageName),
                    item.firstInstallTime,
                    item.lastUpdateTime,
                ),
            )
        }
        // Keep the session's persisted targets visible first, followed by configured apps.
        // List.sort is stable, so catalog order remains unchanged within each priority group.
        visible.sortWith { left, right ->
            QuickTemplateTargetSelectionPolicy.compare(
                left.sortable(),
                right.sortable(),
                savedSelectedPackages,
                sortMode,
                reverseOrder,
            )
        }
        val state = State(
            templateId,
            templateName,
            visible,
            selectedPackages.size,
            QuickTemplateTargetSelectionPolicy.hasUnsavedChanges(selectedPackages, savedSelectedPackages),
            query,
            showSystemApps,
            showUserApps,
            showAllApps,
            showConfiguredApps,
            sortMode,
            reverseOrder,
            loading,
            missingTemplate,
        )
        for (listener in LinkedHashSet(listeners)) listener.onStateChanged(state)
    }

    private class RawTargetApp(
        val label: String,
        val packageName: String,
        val configured: Boolean,
        val systemApp: Boolean,
        val icon: Drawable?,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
    )

    companion object {
        private const val FILTER_PREFS_NAME = "quick_template_target_filters"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_SHOW_USER_APPS = "show_user_apps"
        private const val KEY_SHOW_ALL_APPS = "show_all_apps"
        private const val KEY_SHOW_CONFIGURED_APPS = "show_configured_apps"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_REVERSE_ORDER = "reverse_order"

        const val SORT_NAME = QuickTemplateTargetSelectionPolicy.SORT_NAME
        const val SORT_UPDATED = QuickTemplateTargetSelectionPolicy.SORT_UPDATED
        const val SORT_INSTALLED = QuickTemplateTargetSelectionPolicy.SORT_INSTALLED
    }
}
