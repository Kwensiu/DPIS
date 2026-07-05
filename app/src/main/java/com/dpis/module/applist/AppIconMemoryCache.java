package com.dpis.module.applist;

import android.graphics.drawable.Drawable;
import android.util.LruCache;

public final class AppIconMemoryCache {
    private final LruCache<String, Drawable> cache;

    public AppIconMemoryCache(int maxEntries) {
        cache = new LruCache<>(Math.max(1, maxEntries));
    }

    public Drawable get(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        synchronized (cache) {
            return cache.get(packageName);
        }
    }

    public void put(String packageName, Drawable icon) {
        if (packageName == null || packageName.isEmpty() || icon == null) {
            return;
        }
        synchronized (cache) {
            cache.put(packageName, icon);
        }
    }
}
