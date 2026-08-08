package com.dpis.module.fonts.hookdomain;

import java.util.Set;

/** Builds the shell commands used to publish per-package font hook-domain overrides. */
final class FontHookDomainPropertyCommands {
    private FontHookDomainPropertyCommands() {
    }

    static String buildPublish(String packageName, Set<String> enabledKnownDomains) {
        String value = FontHookDomainPropertyBridge.encodeOverrideValue(
                packageName, enabledKnownDomains);
        return buildSetCommand(FontHookDomainPropertyBridge.propertyNameForPackage(packageName), value)
                + "; " + buildSetCommand(
                        FontHookDomainPropertyBridge.persistentPropertyNameForPackage(packageName),
                        value);
    }

    static String buildClear(String packageName) {
        return buildSetCommand(FontHookDomainPropertyBridge.propertyNameForPackage(packageName), "0")
                + "; " + buildSetCommand(
                        FontHookDomainPropertyBridge.persistentPropertyNameForPackage(packageName),
                        "0");
    }

    private static String buildSetCommand(String property, String value) {
        return "setprop " + shellQuote(property) + " " + shellQuote(value);
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
