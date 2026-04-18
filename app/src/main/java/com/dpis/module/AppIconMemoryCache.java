package com.dpis.module;

import android.graphics.drawable.Drawable;
import android.util.LruCache;

final class AppIconMemoryCache {
    private final LruCache<String, Drawable> cache;

    AppIconMemoryCache(int maxEntries) {
        cache = new LruCache<>(Math.max(1, maxEntries));
    }

    Drawable get(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        synchronized (cache) {
            return cache.get(packageName);
        }
    }

    void put(String packageName, Drawable icon) {
        if (packageName == null || packageName.isEmpty() || icon == null) {
            return;
        }
        synchronized (cache) {
            cache.put(packageName, icon);
        }
    }
}
