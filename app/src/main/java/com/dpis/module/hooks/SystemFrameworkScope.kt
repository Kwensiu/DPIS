package com.dpis.module.hooks

/** Shared semantics for LSPosed/Xposed system-framework scope entries. */
object SystemFrameworkScope {
    const val SYSTEM_SCOPE_MODERN = "system"
    const val SYSTEM_SCOPE_ANDROID_ALIAS = "android"

    /** System-framework entries enable system_server hooks, not user app targets. */
    @JvmStatic
    fun isFrameworkScopePackage(packageName: String?): Boolean =
        packageName == SYSTEM_SCOPE_MODERN || packageName == SYSTEM_SCOPE_ANDROID_ALIAS

    /** Accept both the modern and legacy framework scope aliases. */
    @JvmStatic
    fun containsSystemScope(scopePackages: Collection<String>?): Boolean =
        scopePackages?.contains(SYSTEM_SCOPE_MODERN) == true ||
                scopePackages?.contains(SYSTEM_SCOPE_ANDROID_ALIAS) == true
}
