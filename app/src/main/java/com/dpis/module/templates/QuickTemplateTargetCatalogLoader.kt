package com.dpis.module.templates

import com.dpis.module.DpisLog
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.applist.InstalledAppCatalogItem
import com.dpis.module.config.PackageConfigRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Loads and projects installed applications for the target picker off the UI thread. */
class QuickTemplateTargetCatalogLoader(
    private val catalogCoordinator: InstalledAppCatalogCoordinator,
    private val packageConfigRepository: PackageConfigRepository,
    private val listener: Listener,
) {
    interface Listener {
        fun onLoaded(items: List<QuickTemplateTargetsBinder.TargetAppItem>)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var requestId = 0
    private var disposed = false

    fun load() {
        if (disposed) return
        val currentRequest = ++requestId
        executor.execute {
            val items = try {
                buildItems()
            } catch (throwable: Throwable) {
                DpisLog.e("quick template target list load failed", throwable)
                emptyList()
            }
            if (!disposed && currentRequest == requestId) {
                listener.onLoaded(items)
            }
        }
    }

    fun dispose() {
        disposed = true
        requestId++
        executor.shutdownNow()
    }

    private fun buildItems(): List<QuickTemplateTargetsBinder.TargetAppItem> =
        catalogCoordinator.loadInstalledAppCatalogWithIcons(false).map { item: InstalledAppCatalogItem ->
            QuickTemplateTargetsBinder.TargetAppItem(
                item.label,
                item.packageName,
                packageConfigRepository.hasRealPackageConfig(item.packageName),
                item.systemApp,
                item.icon,
            )
        }
}
