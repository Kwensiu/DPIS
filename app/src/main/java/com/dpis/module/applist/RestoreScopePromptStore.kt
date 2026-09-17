package com.dpis.module.applist

import android.content.Context
import android.content.SharedPreferences

/** One-shot prompt after a successful configuration restore. Survives process relaunch. */
class RestoreScopePromptStore(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    fun isPending(): Boolean = preferences.getBoolean(KEY_PENDING, false)

    fun markPending() {
        preferences.edit().putBoolean(KEY_PENDING, true).apply()
    }

    /** Replaces any prior restore intent so each successful import stands alone. */
    fun replacePendingScope(packageNames: Iterable<String>) {
        val scope = packageNames.toSet()
        preferences.edit()
            .putStringSet(KEY_SCOPE_PACKAGES, scope)
            .putBoolean(KEY_PENDING, scope.isNotEmpty())
            .apply()
    }

    fun scopePackages(): Set<String> = preferences.getStringSet(KEY_SCOPE_PACKAGES, emptySet()).orEmpty()

    fun clear() {
        preferences.edit().remove(KEY_SCOPE_PACKAGES).putBoolean(KEY_PENDING, false).apply()
    }

    companion object {
        private const val PREFS_NAME = "restore_scope_prompt"
        private const val KEY_PENDING = "pending_after_restore"
        private const val KEY_SCOPE_PACKAGES = "scope_packages"
    }
}
