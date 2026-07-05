package com.dpis.module.templates;

import android.content.SharedPreferences;

public final class TemplateConfigPreferences {
    private static final int MIN_FONT_SCALE_PERCENT = 50;
    private static final int MAX_FONT_SCALE_PERCENT = 300;

    private static final String KEY_VIEWPORT_TARGET_TYPE = "viewport.target_type";
    private static final String KEY_VIEWPORT_WIDTH_DP = "viewport.width_dp";
    private static final String KEY_VIEWPORT_SCALE_PERMILLE = "viewport.scale_permille";
    private static final String KEY_VIEWPORT_SCALE_MILLI_PERCENT = "viewport.scale_milli_percent";
    private static final String KEY_VIEWPORT_WIDTH_DRAFT_DP = "viewport.width_draft_dp";
    private static final String KEY_VIEWPORT_SCALE_DRAFT_PERMILLE =
            "viewport.scale_draft_permille";
    private static final String KEY_VIEWPORT_SCALE_DRAFT_MILLI_PERCENT =
            "viewport.scale_draft_milli_percent";
    private static final String KEY_VIEWPORT_MODE = "viewport.mode";
    private static final String KEY_FONT_SCALE_PERCENT = "font.scale_percent";
    private static final String KEY_FONT_MODE = "font.mode";
    private static final String KEY_TYPEFACE_ID = "font.typeface_id";
    private static final String KEY_FONT_HOOK_DOMAINS = "font.hook_domains";

    private TemplateConfigPreferences() {
    }

    public static TemplateConfigValue read(SharedPreferences preferences, String prefix) {
        String targetType = TemplateConfigValue.normalizeViewportTargetType(
                getString(preferences, prefix + KEY_VIEWPORT_TARGET_TYPE,
                        TemplateConfigValue.VIEWPORT_TARGET_OFF));
        Integer scaleMilliPercent = null;
        Integer widthDp = null;
        if (TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE.equals(targetType)) {
            scaleMilliPercent = getInt(preferences, prefix + KEY_VIEWPORT_SCALE_MILLI_PERCENT);
            if (scaleMilliPercent == null) {
                // Legacy fallback
                Integer scalePermille = getInt(preferences, prefix + KEY_VIEWPORT_SCALE_PERMILLE);
                scaleMilliPercent = scalePermille != null
                        ? TemplateConfigValue.fromLegacyScalePermille(scalePermille) : null;
            }
            scaleMilliPercent = normalizeViewportScaleMilliPercent(scaleMilliPercent);
        } else if (TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP.equals(targetType)) {
            widthDp = normalizeViewportWidthDp(getInt(preferences, prefix + KEY_VIEWPORT_WIDTH_DP));
        }

        return new TemplateConfigValue(
                targetType,
                scaleMilliPercent,
                widthDp,
                normalizeViewportScaleMilliPercent(
                        readScaleMilliPercentDraft(preferences, prefix)),
                normalizeViewportWidthDp(
                        getInt(preferences, prefix + KEY_VIEWPORT_WIDTH_DRAFT_DP)),
                getString(preferences, prefix + KEY_VIEWPORT_MODE,
                        TemplateConfigValue.VIEWPORT_MODE_OFF),
                normalizeFontScalePercent(getInt(preferences, prefix + KEY_FONT_SCALE_PERCENT)),
                getString(preferences, prefix + KEY_FONT_MODE, TemplateConfigValue.FONT_MODE_OFF),
                getString(preferences, prefix + KEY_TYPEFACE_ID, null),
                getString(preferences, prefix + KEY_FONT_HOOK_DOMAINS, null));
    }

    public static void write(SharedPreferences.Editor editor, String prefix, TemplateConfigValue value) {
        clear(editor, prefix);
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        if (!TemplateConfigValue.VIEWPORT_TARGET_OFF.equals(normalized.viewportTargetType)) {
            editor.putString(prefix + KEY_VIEWPORT_TARGET_TYPE, normalized.viewportTargetType);
        }
        if (normalized.hasViewportTargetValue()) {
            if (normalized.isRelativeScaleViewport()) {
                int scaleMilliPercent = normalized.viewportScaleMilliPercent;
                editor.putInt(prefix + KEY_VIEWPORT_SCALE_MILLI_PERCENT, scaleMilliPercent);
                // Double-write legacy for downgrade compatibility
                editor.putInt(prefix + KEY_VIEWPORT_SCALE_PERMILLE,
                        TemplateConfigValue.toLegacyScalePermille(scaleMilliPercent));
            } else {
                editor.putInt(prefix + KEY_VIEWPORT_WIDTH_DP,
                        normalized.viewportWidthDp);
            }
        }
        if (normalized.viewportScaleMilliPercentDraft != null) {
            editor.putInt(prefix + KEY_VIEWPORT_SCALE_DRAFT_MILLI_PERCENT,
                    normalized.viewportScaleMilliPercentDraft);
            // Double-write legacy for downgrade compatibility
            editor.putInt(prefix + KEY_VIEWPORT_SCALE_DRAFT_PERMILLE,
                    TemplateConfigValue.toLegacyScalePermille(
                            normalized.viewportScaleMilliPercentDraft));
        }
        if (normalized.viewportWidthDpDraft != null) {
            editor.putInt(prefix + KEY_VIEWPORT_WIDTH_DRAFT_DP,
                    normalized.viewportWidthDpDraft);
        }
        if (TemplateConfigValue.isViewportApplyModeEnabled(normalized.viewportApplyMode)) {
            editor.putString(prefix + KEY_VIEWPORT_MODE, normalized.viewportApplyMode);
        }
        if (normalizeFontScalePercent(normalized.fontScalePercent) != null) {
            editor.putInt(prefix + KEY_FONT_SCALE_PERCENT, normalized.fontScalePercent);
        }
        if (TemplateConfigValue.isFontApplyModeEnabled(normalized.fontApplyMode)) {
            editor.putString(prefix + KEY_FONT_MODE, normalized.fontApplyMode);
        }
        if (normalized.typefaceId != null) {
            editor.putString(prefix + KEY_TYPEFACE_ID, normalized.typefaceId);
        }
        if (normalized.fontHookDomainsRaw != null) {
            editor.putString(prefix + KEY_FONT_HOOK_DOMAINS, normalized.fontHookDomainsRaw);
        }
    }

    public static void clear(SharedPreferences.Editor editor, String prefix) {
        editor.remove(prefix + KEY_VIEWPORT_TARGET_TYPE)
                .remove(prefix + KEY_VIEWPORT_WIDTH_DP)
                .remove(prefix + KEY_VIEWPORT_SCALE_PERMILLE)
                .remove(prefix + KEY_VIEWPORT_SCALE_MILLI_PERCENT)
                .remove(prefix + KEY_VIEWPORT_WIDTH_DRAFT_DP)
                .remove(prefix + KEY_VIEWPORT_SCALE_DRAFT_PERMILLE)
                .remove(prefix + KEY_VIEWPORT_SCALE_DRAFT_MILLI_PERCENT)
                .remove(prefix + KEY_VIEWPORT_MODE)
                .remove(prefix + KEY_FONT_SCALE_PERCENT)
                .remove(prefix + KEY_FONT_MODE)
                .remove(prefix + KEY_TYPEFACE_ID)
                .remove(prefix + KEY_FONT_HOOK_DOMAINS);
    }

    private static Integer getInt(SharedPreferences preferences, String key) {
        if (!preferences.contains(key)) {
            return null;
        }
        try {
            return preferences.getInt(key, 0);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private static String getString(SharedPreferences preferences, String key, String defaultValue) {
        if (!preferences.contains(key)) {
            return defaultValue;
        }
        try {
            return preferences.getString(key, defaultValue);
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    private static Integer normalizeFontScalePercent(Integer percent) {
        if (percent == null
                || percent < MIN_FONT_SCALE_PERCENT
                || percent > MAX_FONT_SCALE_PERCENT) {
            return null;
        }
        return percent;
    }

    private static Integer readScaleMilliPercentDraft(SharedPreferences preferences, String prefix) {
        Integer value = getInt(preferences, prefix + KEY_VIEWPORT_SCALE_DRAFT_MILLI_PERCENT);
        if (value != null) {
            return value;
        }
        // Legacy fallback
        Integer legacyValue = getInt(preferences, prefix + KEY_VIEWPORT_SCALE_DRAFT_PERMILLE);
        return legacyValue != null ? TemplateConfigValue.fromLegacyScalePermille(legacyValue) : null;
    }

    private static Integer normalizeViewportScaleMilliPercent(Integer scaleMilliPercent) {
        if (scaleMilliPercent == null
                || scaleMilliPercent < TemplateConfigValue.MIN_VIEWPORT_SCALE_MILLI_PERCENT
                || scaleMilliPercent > TemplateConfigValue.MAX_VIEWPORT_SCALE_MILLI_PERCENT) {
            return null;
        }
        return scaleMilliPercent;
    }

    private static Integer normalizeViewportWidthDp(Integer widthDp) {
        if (widthDp == null || widthDp <= 0) {
            return null;
        }
        return widthDp;
    }
}
