package com.dpis.module.templates;

import android.content.SharedPreferences;

public final class GlobalPrefillStore {
    private static final String PREFIX = "default_config.";

    private final SharedPreferences preferences;

    public GlobalPrefillStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public TemplateConfigValue read() {
        return TemplateCustomSemantics.customValue(
                TemplateConfigPreferences.read(preferences, PREFIX));
    }

    public boolean write(TemplateConfigValue value) {
        SharedPreferences.Editor editor = preferences.edit();
        TemplateConfigPreferences.write(editor, PREFIX,
                TemplateCustomSemantics.customValue(value));
        return editor.commit();
    }

    public boolean clear() {
        SharedPreferences.Editor editor = preferences.edit();
        TemplateConfigPreferences.clear(editor, PREFIX);
        return editor.commit();
    }
}
