package com.dpis.module.updates;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class ReleaseNotesCacheStore {
    public static final String PREFS_NAME = "dpis.release_notes_cache";
    public static final long DEFAULT_TTL_MS = 24L * 60L * 60L * 1000L;

    private static final String KEY_BODY_PREFIX = "body.";
    private static final String KEY_TIMESTAMP_PREFIX = "timestamp.";
    private static final String KEY_PRESENT_PREFIX = "present.";

    private final SharedPreferences prefs;
    private final long ttlMs;

    public ReleaseNotesCacheStore(Context context) {
        this(context, DEFAULT_TTL_MS);
    }

    public ReleaseNotesCacheStore(Context context, long ttlMs) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.prefs = Objects.requireNonNull(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                "context shared preferences");
        this.ttlMs = Math.max(0L, ttlMs);
    }

    public ReleaseNotesCacheStore(SharedPreferences prefs, long ttlMs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs == null");
        }
        this.prefs = Objects.requireNonNull(prefs, "prefs");
        this.ttlMs = Math.max(0L, ttlMs);
    }

    public String getValidBody(String versionName, long nowMs) {
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

    public void put(String versionName, String body, long nowMs) {
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

    public void clear() {
        prefs.edit().clear().commit();
    }

    public long estimateCacheBytes() {
        long total = 0L;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            total += utf8Bytes(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof String stringValue) {
                total += utf8Bytes(stringValue);
            } else if (value instanceof Boolean) {
                total += 1L;
            } else if (value instanceof Number) {
                total += Long.BYTES;
            }
        }
        return total;
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

    private static int utf8Bytes(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }
}
