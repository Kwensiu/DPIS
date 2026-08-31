package com.dpis.module.templates

import android.content.SharedPreferences

/** Persists the global editor prefill without ever creating a configured package entry. */
class GlobalPrefillStore(
    private val preferences: SharedPreferences,
) {
    fun read(): TemplateConfigValue = TemplateCustomSemantics.customValue(
        TemplateConfigPreferences.read(preferences, PREFIX),
    )

    fun write(value: TemplateConfigValue?): Boolean = preferences.edit().run {
        TemplateConfigPreferences.write(this, PREFIX, TemplateCustomSemantics.customValue(value))
        commit()
    }

    fun clear(): Boolean = preferences.edit().run {
        TemplateConfigPreferences.clear(this, PREFIX)
        commit()
    }

    private companion object {
        const val PREFIX = "default_config."
    }
}
