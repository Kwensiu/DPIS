package com.dpis.module.fonts

import android.content.Context
import android.graphics.Typeface
import com.dpis.module.ConfigStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide UI catalogue for font selection.
 *
 * Font files are stable until the font library changes, so page entry must not rescan and parse
 * every face. Mutations invalidate this cache and start one background rebuild.
 */
object TypefaceCatalogCache {
    data class Entry(
        val id: String,
        val displayName: String,
        val preview: Typeface?
    )

    data class Catalog(
        val systemEntries: List<Entry>,
        val importedEntries: List<Entry>
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var cached: Catalog? = null
    private var loading: Deferred<Catalog>? = null

    @JvmStatic
    fun preload(context: Context) {
        val applicationContext = context.applicationContext
        scope.launch {
            ensureLoaded(applicationContext)
        }
    }

    @JvmStatic
    fun invalidate(context: Context) {
        val applicationContext = context.applicationContext
        cached = null
        loading?.cancel()
        loading = null
        scope.launch {
            ensureLoaded(applicationContext)
        }
    }

    fun cached(): Catalog? = cached

    suspend fun get(context: Context): Catalog {
        cached?.let { return it }
        return ensureLoaded(context.applicationContext)
    }

    private suspend fun ensureLoaded(context: Context): Catalog {
        val job = lock.withLock {
            cached?.let { return@withLock null }
            loading?.takeIf { it.isActive } ?: scope.async {
                loadCatalog(context)
            }.also { loading = it }
        }
        if (job == null) {
            return cached ?: error("Typeface catalogue was cleared during loading")
        }
        val result = job.await()
        lock.withLock {
            cached = result
            if (loading === job) {
                loading = null
            }
        }
        return result
    }

    private suspend fun loadCatalog(context: Context): Catalog {
        return coroutineScope {
            val systemEntries = async {
                SystemFontRegistry.listRecommendedFonts().map { entry ->
                    Entry(
                        id = entry.id(),
                        displayName = entry.displayName(),
                        preview = SystemFontRegistry.loadTypeface(entry.id())
                    )
                }
            }
            val importedEntries = async {
                val store = ConfigStoreFactory.createLocalUiFontLibraryStore(context, null)
                store.listFonts().map { entry ->
                    Entry(
                        id = entry.id,
                        displayName = entry.displayName,
                        preview = store.resolveFontFile(entry.id)?.let { file ->
                            FontTypefaceLoader.load(file, entry.ttcIndex)
                        }
                    )
                }
            }
            Catalog(
                systemEntries = systemEntries.await(),
                importedEntries = importedEntries.await()
            )
        }
    }
}
