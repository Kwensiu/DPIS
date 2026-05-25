package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SystemPropertyConfigPreferences implements SharedPreferences {
    private static final long SNAPSHOT_TTL_MILLIS = 2_000L;
    private final String packageName;
    private volatile Map<String, Object> cachedSnapshot;
    private volatile long cachedAtMillis;

    SystemPropertyConfigPreferences(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> snapshot = cachedSnapshot;
        long now = System.currentTimeMillis();
        if (snapshot != null && (now - cachedAtMillis) < SNAPSHOT_TTL_MILLIS) {
            return snapshot;
        }
        // Compat100 publishes the current per-app values through system properties so
        // legacy app processes do not need to read DPIS private files from hook hot paths.
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        ViewportTargetSpec viewportTargetSpec = ViewportPropertyBridge.readTargetSpec(packageName);
        Integer widthDp = viewportTargetSpec.isAbsoluteDp()
                ? viewportTargetSpec.absoluteWidthDp()
                : null;
        String viewportMode = ViewportPropertyBridge.readCompatMode(packageName);
        if (widthDp == null || widthDp <= 0 || !ViewportApplyMode.isEnabled(viewportMode)) {
            widthDp = ViewportPropertyBridge.readTargetWidthDp(packageName);
            if (viewportTargetSpec.isRelativeScale() && ViewportApplyMode.isEnabled(viewportMode)) {
                widthDp = null;
            } else {
                viewportMode = ViewportApplyMode.SYSTEM;
            }
        }
        Integer fontScalePercent = HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName);
        String fontMode = HyperOsFlutterFontBridge.readCompatFontMode(packageName);
        Integer forceFontScalePercent = null;
        if (fontScalePercent == null || fontScalePercent <= 0) {
            forceFontScalePercent = HyperOsFlutterFontBridge.readForceFontScalePercent(packageName);
            fontScalePercent = forceFontScalePercent;
        }
        fontMode = resolveCompatFontMode(fontScalePercent, fontMode, forceFontScalePercent);
        String typefaceId = HyperOsFlutterFontBridge.readTypefaceId(packageName);
        if (viewportTargetSpec.isEnabled() && ViewportApplyMode.isEnabled(viewportMode)) {
            values.put(viewportTargetTypeKey(), viewportTargetSpec.type());
            if (viewportTargetSpec.isRelativeScale()) {
                values.put(viewportScalePermilleKey(), viewportTargetSpec.scalePermille());
            }
            if (widthDp != null && widthDp > 0) {
                values.put(viewportWidthKey(), widthDp);
            }
            values.put(viewportModeKey(), viewportMode);
        }
        values.put(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED,
                RuntimeDebugPropertyBridge.readGlobalLogEnabled());
        values.put(DpiConfigStore.KEY_FONT_DEBUG_OVERLAY_ENABLED,
                RuntimeDebugPropertyBridge.readFontDebugOverlayEnabled());
        if (fontScalePercent != null && fontScalePercent > 0) {
            values.put(fontScaleKey(), fontScalePercent);
            values.put(fontModeKey(), FontApplyMode.normalize(fontMode));
        }
        if (typefaceId != null && !typefaceId.isBlank()) {
            values.put(typefaceIdKey(), typefaceId);
        }
        if (!values.isEmpty()) {
            HookDomainOverride override = FontHookDomainPropertyBridge.readOverride(packageName);
            if (override.customPathEnabled) {
                values.put(hookDomainsKey(), String.join(",",
                        FontHookDomainRegistry.orderedCustomizableSubset(
                                override.enabledKnownDomains)));
            }
            values.put(DpiConfigStore.KEY_TARGET_PACKAGES,
                    new LinkedHashSet<>(Collections.singleton(packageName)));
        }
        cachedSnapshot = Collections.unmodifiableMap(values);
        cachedAtMillis = now;
        return cachedSnapshot;
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = getAll().get(key);
        return value instanceof String typed ? typed : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = getAll().get(key);
        return value instanceof Set<?> ? new LinkedHashSet<>((Set<String>) value) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = getAll().get(key);
        return value instanceof Integer typed ? typed : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = getAll().get(key);
        return value instanceof Long typed ? typed : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = getAll().get(key);
        return value instanceof Float typed ? typed : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = getAll().get(key);
        return value instanceof Boolean typed ? typed : defValue;
    }

    @Override
    public boolean contains(String key) {
        return getAll().containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("SystemPropertyConfigPreferences is read-only");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    static String resolveCompatFontModeForTest(Integer compatFontScalePercent,
                                               String rawMode,
                                               Integer forceFontScalePercent) {
        return resolveCompatFontMode(compatFontScalePercent, rawMode, forceFontScalePercent);
    }

    private static String resolveCompatFontMode(Integer fontScalePercent,
                                                String rawMode,
                                                Integer forceFontScalePercent) {
        String mode = FontApplyMode.normalize(rawMode);
        if (FontApplyMode.isEnabled(mode)) {
            return mode;
        }
        if (fontScalePercent == null || fontScalePercent <= 0) {
            return FontApplyMode.OFF;
        }
        return forceFontScalePercent != null && forceFontScalePercent > 0
                ? FontApplyMode.FIELD_REWRITE
                : FontApplyMode.SYSTEM_EMULATION;
    }

    private String viewportWidthKey() {
        return "viewport." + packageName + ".width_dp";
    }

    private String viewportTargetTypeKey() {
        return "viewport." + packageName + ".target_type";
    }

    private String viewportScalePermilleKey() {
        return "viewport." + packageName + ".scale_permille";
    }

    private String viewportModeKey() {
        return "viewport." + packageName + ".mode";
    }

    private String fontScaleKey() {
        return "font." + packageName + ".scale_percent";
    }

    private String fontModeKey() {
        return "font." + packageName + ".mode";
    }

    private String hookDomainsKey() {
        return "font." + packageName + ".hook_domains";
    }

    private String typefaceIdKey() {
        return "font." + packageName + ".typeface_id";
    }
}
