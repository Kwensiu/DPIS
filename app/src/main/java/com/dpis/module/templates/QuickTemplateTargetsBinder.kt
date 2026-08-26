package com.dpis.module.templates

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dpis.module.DpisApplication
import com.dpis.module.DpisLog
import com.dpis.module.R
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.TouchFeedbackBinder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import java.util.LinkedHashSet
import java.util.Locale

/** Binds the template target picker; persistence and catalog loading are delegated. */
class QuickTemplateTargetsBinder(
    private val activity: Activity,
    private val rootView: View,
    private val host: Host,
) {
    interface Host {
        fun getPackageManager(): android.content.pm.PackageManager
        fun getSelfPackageName(): String
        fun runOnUiThread(runnable: Runnable)
        fun getIconRefreshAnchor(): View?
        fun onSaved()
        fun onMissingTemplate()
        fun showToast(messageResId: Int)
    }

    private val quickTemplateStore = QuickTemplateStore(activity)
    private val filterState = QuickTemplateTargetFilterState.from(
        activity.getSharedPreferences(FILTER_PREFS_NAME, Activity.MODE_PRIVATE)
    )
    private val packageConfigRepository = PackageConfigRepository(
        DpisApplication.getActiveHookConfigStore(activity)
    )
    private val installedAppCatalogCoordinator = InstalledAppCatalogCoordinator(
        createInstalledAppCatalogHost(), INSTALLED_APP_CATALOG_TTL_MS
    )
    private val targetCatalogLoader = QuickTemplateTargetCatalogLoader(
        installedAppCatalogCoordinator,
        packageConfigRepository,
        object : QuickTemplateTargetCatalogLoader.Listener {
            override fun onLoaded(items: List<TargetAppItem>) {
                host.runOnUiThread(Runnable { onTargetAppsLoaded(items) })
            }
        }
    )
    private val allTargetItems = ArrayList<TargetAppItem>()
    private val selectedPackages = LinkedHashSet<String>()
    private var template: QuickTemplateStore.QuickTemplate? = null
    private var adapter: QuickTemplateTargetAdapter? = null
    private var subtitleView: MaterialTextView? = null
    private var emptyView: MaterialTextView? = null
    private var searchInput: AppCompatEditText? = null
    private var searchClearButton: ImageButton? = null
    private var disposed = false

    fun bind(templateId: String): Boolean {
        val loadedTemplate = quickTemplateStore.read(templateId)
        if (loadedTemplate == null) {
            host.showToast(R.string.quick_template_target_missing)
            host.onMissingTemplate()
            return false
        }
        template = loadedTemplate
        selectedPackages.clear()
        selectedPackages.addAll(loadedTemplate.selectedPackages)
        bindViews()
        bindList()
        targetCatalogLoader.load()
        return true
    }

    fun dispose() {
        disposed = true
        targetCatalogLoader.dispose()
        installedAppCatalogCoordinator.shutdown()
    }

    private fun bindViews() {
        subtitleView = rootView.findViewById(R.id.quick_template_targets_subtitle)
        emptyView = rootView.findViewById(R.id.quick_template_targets_empty)
        searchInput = rootView.findViewById(R.id.quick_template_targets_search_input)
        searchClearButton = rootView.findViewById(R.id.quick_template_targets_search_clear_button)
        val saveButton = rootView.findViewById<View>(R.id.quick_template_targets_save_button)
        val titleView = rootView.findViewById<MaterialTextView>(R.id.quick_template_targets_title)
        titleView.text = activity.getString(R.string.quick_template_targets_title, template?.name)
        refreshSelectedCount()
        TouchFeedbackBinder.bindPressHaptic(saveButton)
        saveButton.setOnClickListener { saveSelection() }
        searchInput?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchClearButton?.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                applyTargetFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        searchClearButton?.setOnClickListener {
            searchInput?.setText("")
            searchInput?.requestFocus()
        }
        rootView.findViewById<ImageButton>(R.id.quick_template_targets_filter_button)?.let { filterButton ->
            TouchFeedbackBinder.bindPressHaptic(filterButton)
            filterButton.setOnClickListener { showFilterSheet() }
        }
    }

    private fun bindList() {
        val list = rootView.findViewById<RecyclerView>(R.id.quick_template_targets_list)
        val targetAdapter = QuickTemplateTargetAdapter(
            selectedPackages,
            ::onSelectionChanged,
            ::onIconLoadRequested
        )
        adapter = targetAdapter
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = targetAdapter
    }

    private fun onTargetAppsLoaded(loaded: List<TargetAppItem>) {
        if (disposed) return
        allTargetItems.clear()
        allTargetItems.addAll(loaded)
        pruneSelectedPackagesToInstalledApps(selectedPackages, allTargetItems)
        refreshSelectedCount()
        applyTargetFilters()
    }

    private fun onIconLoadRequested(packageName: String) =
        installedAppCatalogCoordinator.onIconLoadRequested(packageName)

    private fun applyTargetFilters() {
        val query = textOf(searchInput).trim().lowercase(Locale.ROOT)
        val filtered = allTargetItems.filterTo(ArrayList()) { item ->
            matchesTargetFilters(item, query, filterState.showSystemApps, filterState.hideConfiguredApps,
                selectedPackages.contains(item.packageName))
        }
        filtered.sortWith(compareBy {
            QuickTemplateTargetOrdering.priority(selectedPackages.contains(it.packageName), it.configured)
        })
        adapter?.submit(filtered)
        emptyView?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onSelectionChanged(packageName: String, selected: Boolean) {
        if (selected) selectedPackages.add(packageName) else selectedPackages.remove(packageName)
        refreshSelectedCount()
    }

    private fun refreshSelectedCount() {
        subtitleView?.text = activity.getString(
            R.string.quick_template_targets_selected_count, selectedPackages.size
        )
    }

    private fun saveSelection() {
        val current = template ?: return
        if (quickTemplateStore.setSelectedPackages(current.id, selectedPackages)) {
            host.showToast(R.string.quick_template_targets_save_success)
            host.onSaved()
        } else {
            host.showToast(R.string.quick_template_targets_save_failed)
        }
    }

    private fun showFilterSheet() {
        var root = rootView.findViewById<ViewGroup>(android.R.id.content)
        if (root == null) root = activity.findViewById(android.R.id.content)
        val dialogView = LayoutInflater.from(activity).inflate(
            R.layout.dialog_quick_template_target_filters, root, false
        )
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(dialogView)
        val showSystemSwitch = dialogView.findViewById<MaterialSwitch>(
            R.id.quick_template_targets_filter_show_system_switch
        )
        val hideConfiguredSwitch = dialogView.findViewById<MaterialSwitch>(
            R.id.quick_template_targets_filter_hide_configured_switch
        )
        showSystemSwitch.isChecked = filterState.showSystemApps
        hideConfiguredSwitch.isChecked = filterState.hideConfiguredApps
        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            filterState.update(showSystemSwitch.isChecked, hideConfiguredSwitch.isChecked)
            applyTargetFilters()
        }
        showSystemSwitch.setOnCheckedChangeListener(listener)
        hideConfiguredSwitch.setOnCheckedChangeListener(listener)
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
    }

    private fun createInstalledAppCatalogHost() = object : InstalledAppCatalogCoordinator.Host {
        override fun getPackageManager() = host.getPackageManager()
        override fun getSelfPackageName() = host.getSelfPackageName()
    }

    private fun textOf(view: AppCompatEditText?): String = view?.text?.toString().orEmpty()

    companion object {
        private const val FILTER_PREFS_NAME = "quick_template_target_filters"
        private const val INSTALLED_APP_CATALOG_TTL_MS = 60_000L

        @JvmStatic
        fun matchesTargetFilters(item: TargetAppItem?, normalizedQuery: String?, showSystemApps: Boolean,
                                 hideConfiguredApps: Boolean) =
            matchesTargetFilters(item, normalizedQuery, showSystemApps, hideConfiguredApps, false)

        @JvmStatic
        fun matchesTargetFilters(item: TargetAppItem?, normalizedQuery: String?, showSystemApps: Boolean,
                                 hideConfiguredApps: Boolean, selected: Boolean): Boolean {
            if (item == null || (!showSystemApps && item.systemApp) ||
                (hideConfiguredApps && item.configured && !selected)) return false
            val query = normalizedQuery.orEmpty()
            return query.isEmpty() || item.label.lowercase(Locale.ROOT).contains(query) ||
                item.packageName.lowercase(Locale.ROOT).contains(query)
        }

        @JvmStatic
        fun pruneSelectedPackagesToInstalledApps(selectedPackages: MutableSet<String>?,
                                                 installedItems: List<TargetAppItem>?): LinkedHashSet<String> {
            val installed = installedItems.orEmpty().mapNotNull { it.packageName?.trim()?.takeIf(String::isNotBlank) }.toSet()
            val pruned = LinkedHashSet<String>()
            selectedPackages?.forEach { packageName ->
                val trimmed = packageName.trim()
                if (installed.contains(trimmed)) pruned.add(trimmed)
            }
            selectedPackages?.apply { clear(); addAll(pruned) }
            return pruned
        }
    }

    class TargetAppItem(
        @JvmField val label: String,
        @JvmField val packageName: String,
        @JvmField val configured: Boolean,
        @JvmField val systemApp: Boolean,
        @JvmField val icon: Drawable?
    ) {
        @JvmOverloads
        constructor(label: String?, packageName: String, configured: Boolean) :
            this(label ?: packageName, packageName, configured, false, null)
    }
}
