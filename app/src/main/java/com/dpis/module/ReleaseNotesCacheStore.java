package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

final class ReleaseNotesCacheStore {
    static final String PREFS_NAME = "dpis.release_notes_cache";
    static final long DEFAULT_TTL_MS = 24L * 60L * 60L * 1000L;

    private static final String KEY_BODY_PREFIX = "body.";
    private static final String KEY_TIMESTAMP_PREFIX = "timestamp.";
    private static final String KEY_PRESENT_PREFIX = "present.";

    private final SharedPreferences prefs;
    private final long ttlMs;

    ReleaseNotesCacheStore(Context context) {
        this(context, DEFAULT_TTL_MS);
    }

    ReleaseNotesCacheStore(Context context, long ttlMs) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.ttlMs = Math.max(0L, ttlMs);
    }

    ReleaseNotesCacheStore(SharedPreferences prefs, long ttlMs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs == null");
        }
        this.prefs = prefs;
        this.ttlMs = Math.max(0L, ttlMs);
    }

    String getValidBody(String versionName, long nowMs) {
        String key = normalizeVersionName(versionName);
        if (key.isEmpty()) {
            return null;
        }
        if (!prefs.getBoolean(KEY_PRESENT_PREFIX + key, false)) {
            return null;
        }
        long cachedAtMs = prefs.getLong(KEY_TIMESTAMP_PREFIX + key, 0L);
        if (cachedAtMs <= 0L || nowMs < cachedAtMs || nowMs - cachedAtMs > ttlMs) {
            return null;
        }
        return prefs.getString(KEY_BODY_PREFIX + key, "");
    }

    void put(String versionName, String body, long nowMs) {
        String key = normalizeVersionName(versionName);
        if (key.isEmpty() || body == null) {
            return;
        }
        prefs.edit()
                .putString(KEY_BODY_PREFIX + key, body.trim())
                .putLong(KEY_TIMESTAMP_PREFIX + key, nowMs)
                .putBoolean(KEY_PRESENT_PREFIX + key, true)
                .apply();
    }

    private static String normalizeVersionName(String versionName) {
        if (versionName == null) {
            return "";
        }
        String normalized = versionName.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
