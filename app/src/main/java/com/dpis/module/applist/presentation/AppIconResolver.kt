package com.dpis.module.applist.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Supplies icons for visible app rows without blocking composition on PackageManager calls. */
object AppIconResolver {
    interface Callback {
        fun onIconsResolved(icons: Map<String, Drawable>)
    }

    private const val CACHE_SIZE = 192
    private val executor: ExecutorService = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cache = LruCache<String, Drawable>(CACHE_SIZE)

    // Keep every active consumer so a configuration change still receives an in-flight result.
    private val waitingCallbacks = LinkedHashMap<String, MutableSet<Callback>>()

    @JvmStatic
    fun request(
        context: Context?,
        packageNames: Collection<String?>?,
        callback: Callback?,
    ) {
        if (context == null || callback == null) return
        val requestedPackages = packageNames ?: return
        if (requestedPackages.isEmpty()) return
        val applicationContext = context.applicationContext
        val cached = LinkedHashMap<String, Drawable>()
        val toResolve = LinkedHashSet<String>()
        synchronized(cache) {
            for (candidate in requestedPackages) {
                val packageName = candidate ?: continue
                if (packageName.isEmpty()) continue
                val icon = cache.get(packageName)
                if (icon != null) {
                    cached[packageName] = icon
                    continue
                }
                val waiters = waitingCallbacks.getOrPut(packageName) {
                    toResolve.add(packageName)
                    LinkedHashSet()
                }
                waiters.add(callback)
            }
        }
        if (cached.isNotEmpty()) {
            mainHandler.post { callback.onIconsResolved(cached) }
        }
        if (toResolve.isNotEmpty()) {
            executor.execute { resolve(applicationContext, toResolve) }
        }
    }

    private fun resolve(context: Context, packageNames: Set<String>) {
        val resolved = LinkedHashMap<String, Drawable>()
        val packageManager = context.packageManager
        for (packageName in packageNames) {
            try {
                resolved[packageName] = packageManager.getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                // Configured but removed packages intentionally remain without an icon.
            } catch (_: RuntimeException) {
                // PackageManager can fail transiently while the package list changes.
            }
        }
        val deliveries = LinkedHashMap<Callback, MutableMap<String, Drawable>>()
        synchronized(cache) {
            resolved.forEach { (packageName, icon) -> cache.put(packageName, icon) }
            for (packageName in packageNames) {
                val waiters = waitingCallbacks.remove(packageName) ?: continue
                val icon = resolved[packageName] ?: continue
                for (waiter in waiters) {
                    deliveries.getOrPut(waiter) { LinkedHashMap() }[packageName] = icon
                }
            }
        }
        deliveries.forEach { (callback, icons) ->
            mainHandler.post { callback.onIconsResolved(icons) }
        }
    }
}
