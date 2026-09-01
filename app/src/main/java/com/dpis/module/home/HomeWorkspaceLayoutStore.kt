package com.dpis.module.home

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Owns local-only Home presentation preferences; these never affect module runtime behavior. */
class HomeWorkspaceLayoutStore internal constructor(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    fun load(): HomeWorkspaceLayout {
        return HomeWorkspaceLayout(parseHiddenItems(preferences.getStringSet(KEY_HIDDEN_ITEMS, emptySet())))
    }

    fun save(layout: HomeWorkspaceLayout) {
        preferences.edit {
            putStringSet(KEY_HIDDEN_ITEMS, layout.hiddenItems.mapTo(mutableSetOf()) { it.name })
        }
    }

    private companion object {
        const val PREFS_NAME = "dpis.home_workspace"
        const val KEY_HIDDEN_ITEMS = "hidden_items"
    }
}

internal fun parseHiddenItems(names: Set<String>?): Set<HomeWorkspaceLayout.Item> = names
    .orEmpty()
    .mapNotNull { itemName -> HomeWorkspaceLayout.Item.entries.firstOrNull { it.name == itemName } }
    .toSet()
