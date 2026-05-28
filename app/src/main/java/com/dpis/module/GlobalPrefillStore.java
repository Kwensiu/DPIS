package com.dpis.module;

import android.content.SharedPreferences;

final class GlobalPrefillStore {
    private static final String PREFIX = "default_config.";

    private final SharedPreferences preferences;

    GlobalPrefillStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    TemplateConfigValue read() {
        return TemplateConfigPreferences.read(preferences, PREFIX);
    }

    boolean write(TemplateConfigValue value) {
        SharedPreferences.Editor editor = preferences.edit();
        TemplateConfigPreferences.write(editor, PREFIX, value);
        return editor.commit();
    }

    boolean clear() {
        SharedPreferences.Editor editor = preferences.edit();
        TemplateConfigPreferences.clear(editor, PREFIX);
        return editor.commit();
    }
}
