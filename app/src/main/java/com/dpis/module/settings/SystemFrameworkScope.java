package com.dpis.module.settings;

import java.util.Collection;

/** Shared semantics for LSPosed/Xposed system-framework scope entries. */
public final class SystemFrameworkScope {
    public static final String SYSTEM_SCOPE_MODERN = "system";
    public static final String SYSTEM_SCOPE_ANDROID_ALIAS = "android";

    private SystemFrameworkScope() {
    }

    /**
     * System-framework scope entries enable DPIS system_server hooks, but they are
     * not user app targets and must not appear in configured-app lists or counts.
     */
    public static boolean isFrameworkScopePackage(String packageName) {
        return SYSTEM_SCOPE_MODERN.equals(packageName)
                || SYSTEM_SCOPE_ANDROID_ALIAS.equals(packageName);
    }

    /**
     * Modern LSPosed reports system_server scope as "system". Some legacy/Xposed
     * surfaces expose the Android framework package name instead, so accept both
     * aliases when resolving whether the system hook is selected.
     */
    public static boolean containsSystemScope(Collection<String> scopePackages) {
        return scopePackages != null
                && (scopePackages.contains(SYSTEM_SCOPE_MODERN)
                || scopePackages.contains(SYSTEM_SCOPE_ANDROID_ALIAS));
    }
}
