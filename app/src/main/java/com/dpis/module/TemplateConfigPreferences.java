package com.dpis.module;

import android.content.SharedPreferences;

final class TemplateConfigPreferences {
    private static final int MIN_FONT_SCALE_PERCENT = 50;
    private static final int MAX_FONT_SCALE_PERCENT = 300;

    private static final String KEY_VIEWPORT_TARGET_TYPE = "viewport.target_type";
    private static final String KEY_VIEWPORT_WIDTH_DP = "viewport.width_dp";
    private static final String KEY_VIEWPORT_SCALE_PERMILLE = "viewport.scale_permille";
    private static final String KEY_VIEWPORT_MODE = "viewport.mode";
    private static final String KEY_FONT_SCALE_PERCENT = "font.scale_percent";
    private static final String KEY_FONT_MODE = "font.mode";
    private static final String KEY_TYPEFACE_ID = "font.typeface_id";
    private static final String KEY_FONT_HOOK_DOMAINS = "font.hook_domains";

    private TemplateConfigPreferences() {
    }

    static TemplateConfigValue read(SharedPreferences preferences, String prefix) {
        String targetType = ViewportTargetType.normalize(
                getString(preferences, prefix + KEY_VIEWPORT_TARGET_TYPE, ViewportTargetType.OFF));
        ViewportTargetSpec viewportTargetSpec = ViewportTargetSpec.off();
        if (ViewportTargetType.RELATIVE_SCALE.equals(targetType)) {
            Integer scalePermille = getInt(preferences, prefix + KEY_VIEWPORT_SCALE_PERMILLE);
            viewportTargetSpec = scalePermille != null
                    ? ViewportTargetSpec.relativeScale(scalePermille)
                    : ViewportTargetSpec.off();
        } else if (ViewportTargetType.ABSOLUTE_DP.equals(targetType)) {
            Integer widthDp = getInt(preferences, prefix + KEY_VIEWPORT_WIDTH_DP);
            viewportTargetSpec = widthDp != null
                    ? ViewportTargetSpec.absoluteDp(widthDp)
                    : ViewportTargetSpec.off();
        }

        return new TemplateConfigValue(
                viewportTargetSpec,
                getString(preferences, prefix + KEY_VIEWPORT_MODE, ViewportApplyMode.OFF),
                normalizeFontScalePercent(getInt(preferences, prefix + KEY_FONT_SCALE_PERCENT)),
                getString(preferences, prefix + KEY_FONT_MODE, FontApplyMode.OFF),
                getString(preferences, prefix + KEY_TYPEFACE_ID, null),
                getString(preferences, prefix + KEY_FONT_HOOK_DOMAINS, null));
    }

    static void write(SharedPreferences.Editor editor, String prefix, TemplateConfigValue value) {
        clear(editor, prefix);
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        if (normalized.viewportTargetSpec.isEnabled()) {
            editor.putString(prefix + KEY_VIEWPORT_TARGET_TYPE, normalized.viewportTargetSpec.type());
            if (normalized.viewportTargetSpec.isRelativeScale()) {
                editor.putInt(prefix + KEY_VIEWPORT_SCALE_PERMILLE,
                        normalized.viewportTargetSpec.scalePermille());
            } else {
                editor.putInt(prefix + KEY_VIEWPORT_WIDTH_DP,
                        normalized.viewportTargetSpec.absoluteWidthDp());
            }
        }
        if (ViewportApplyMode.isEnabled(normalized.viewportApplyMode)) {
            editor.putString(prefix + KEY_VIEWPORT_MODE, normalized.viewportApplyMode);
        }
        if (normalizeFontScalePercent(normalized.fontScalePercent) != null) {
            editor.putInt(prefix + KEY_FONT_SCALE_PERCENT, normalized.fontScalePercent);
        }
        if (FontApplyMode.isEnabled(normalized.fontApplyMode)) {
            editor.putString(prefix + KEY_FONT_MODE, normalized.fontApplyMode);
        }
        if (normalized.typefaceId != null) {
            editor.putString(prefix + KEY_TYPEFACE_ID, normalized.typefaceId);
        }
        if (normalized.fontHookDomainsRaw != null) {
            editor.putString(prefix + KEY_FONT_HOOK_DOMAINS, normalized.fontHookDomainsRaw);
        }
    }

    static void clear(SharedPreferences.Editor editor, String prefix) {
        editor.remove(prefix + KEY_VIEWPORT_TARGET_TYPE)
                .remove(prefix + KEY_VIEWPORT_WIDTH_DP)
                .remove(prefix + KEY_VIEWPORT_SCALE_PERMILLE)
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
}
