package com.dpis.module.applist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Process-wide icon supply for virtualized app rows.
 *
 * <p>Rows request a visible range, rather than resolving a drawable from each individual
 * composition. The bounded worker pool prevents PackageManager calls from competing with
 * scrolling while the cache makes revisiting a range immediate.</p>
 */
public final class InstalledAppIconResolver {
    public interface Callback {
        void onIconsResolved(Map<String, Drawable> icons);
    }

    private static final int CACHE_SIZE = 192;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Drawable> CACHE = new LruCache<>(CACHE_SIZE);
    // A package can be requested again after a configuration change while its first lookup is
    // still running. Keep every active consumer so the result is delivered to the current UI,
    // not only to the callback that happened to start the lookup.
    private static final Map<String, Set<Callback>> WAITING_CALLBACKS = new LinkedHashMap<>();

    private InstalledAppIconResolver() {}

    public static void request(Context context, Collection<String> packageNames, Callback callback) {
        if (context == null || packageNames == null || packageNames.isEmpty() || callback == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        Map<String, Drawable> cached = new LinkedHashMap<>();
        Set<String> toResolve = new LinkedHashSet<>();
        synchronized (CACHE) {
            for (String packageName : packageNames) {
                if (packageName == null || packageName.isEmpty()) continue;
                Drawable icon = CACHE.get(packageName);
                if (icon != null) cached.put(packageName, icon);
                else {
                    Set<Callback> waiters = WAITING_CALLBACKS.get(packageName);
                    if (waiters == null) {
                        waiters = new LinkedHashSet<>();
                        WAITING_CALLBACKS.put(packageName, waiters);
                        toResolve.add(packageName);
                    }
                    waiters.add(callback);
                }
            }
        }
        if (!cached.isEmpty()) MAIN_HANDLER.post(() -> callback.onIconsResolved(cached));
        if (toResolve.isEmpty()) return;
        EXECUTOR.execute(() -> resolve(applicationContext, toResolve));
    }

    private static void resolve(Context context, Set<String> packageNames) {
        Map<String, Drawable> resolved = new LinkedHashMap<>();
        PackageManager packageManager = context.getPackageManager();
        for (String packageName : packageNames) {
            try {
                Drawable icon = packageManager.getApplicationIcon(packageName);
                if (icon != null) resolved.put(packageName, icon);
            } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
                // Configured but removed packages intentionally remain without an icon.
            }
        }
        Map<Callback, Map<String, Drawable>> deliveries = new LinkedHashMap<>();
        synchronized (CACHE) {
            for (Map.Entry<String, Drawable> entry : resolved.entrySet()) {
                CACHE.put(entry.getKey(), entry.getValue());
            }
            for (String packageName : packageNames) {
                Set<Callback> waiters = WAITING_CALLBACKS.remove(packageName);
                Drawable icon = resolved.get(packageName);
                if (icon == null || waiters == null) continue;
                for (Callback waiter : waiters) {
                    deliveries.computeIfAbsent(waiter, ignored -> new LinkedHashMap<>())
                            .put(packageName, icon);
                }
            }
        }
        for (Map.Entry<Callback, Map<String, Drawable>> delivery : deliveries.entrySet()) {
            Callback waitingCallback = delivery.getKey();
            Map<String, Drawable> icons = delivery.getValue();
            MAIN_HANDLER.post(() -> waitingCallback.onIconsResolved(icons));
        }
    }
}
