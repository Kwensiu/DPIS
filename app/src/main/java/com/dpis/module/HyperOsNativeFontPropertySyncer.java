package com.dpis.module;

import java.util.LinkedHashSet;

final class HyperOsNativeFontPropertySyncer {
    private HyperOsNativeFontPropertySyncer() {
    }

    static void publishForceFontTargetAsync(String packageName, int fontScalePercent) {
        if (packageName == null || packageName.isBlank() || fontScalePercent <= 0) {
            return;
        }
        // Native proxies prefer forcefont so value-only changes can bypass stale
        // system_server RustProcess env values after a same-version module update.
        String fontProperty = HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName);
        Thread publisherThread = new Thread(() -> publishPropertyWithRoot(fontProperty, fontScalePercent),
                "DPIS-hyperos-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearFontTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        HyperOsFlutterFontBridge.clearTarget(packageName);
        String fontProperty = HyperOsFlutterFontBridge.propertyNameForPackage(packageName);
        String forceFontProperty = HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName);
        String rustBinaryProperty = HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(packageName);
        Thread cleanerThread = new Thread(() -> clearPropertiesWithRoot(
                        fontProperty, forceFontProperty, rustBinaryProperty),
                "DPIS-hyperos-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    static void clearConfiguredFontTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(store.getConfiguredPackages());
        if (packages.isEmpty()) {
            return;
        }
        Thread cleanerThread = new Thread(() -> clearFontTargets(store, packages),
                "DPIS-hyperos-configured-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    static void syncConfiguredFontTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        for (String packageName : store.getConfiguredPackages()) {
            Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
            String fontMode = store.getTargetFontApplyMode(packageName);
            if (store.isTargetDpisEnabled(packageName)
                    && fontScalePercent != null
                    && fontScalePercent > 0
                    && shouldPublishForceFontOnRecovery(store, packageName, fontMode)) {
                publishForceFontTargetAsync(packageName, fontScalePercent);
            }
        }
    }


    private static void publishPropertyWithRoot(String fontProperty, int fontScalePercent) {
        runRootCommand(buildPublishCommand(fontProperty, fontScalePercent));
    }

    private static void clearPropertiesWithRoot(String fontProperty,
                                                String forceFontProperty,
                                                String rustBinaryProperty) {
        String command = "setprop " + shellQuote(fontProperty) + " 0; "
                + "setprop " + shellQuote(forceFontProperty) + " 0; "
                + "setprop " + shellQuote(rustBinaryProperty) + " 0";
        runRootCommand(command);
    }

    private static void clearFontTargets(DpisConfigStore store, LinkedHashSet<String> packages) {
        StringBuilder command = new StringBuilder();
        for (String packageName : packages) {
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            // forcefont.* is shared by HyperOS native font replacement and
            // legacy field-rewrite config. Disabling only the HyperOS
            // native path must not erase an active legacy field rewrite.
            boolean preserveForceFont = shouldPreserveCompatForceFont(store, packageName);
            if (!preserveForceFont) {
                HyperOsFlutterFontBridge.clearTarget(packageName);
            } else {
                HyperOsFlutterFontBridge.clearNativeTarget(packageName);
            }
            appendClearCommand(command,
                    HyperOsFlutterFontBridge.propertyNameForPackage(packageName));
            if (!preserveForceFont) {
                appendClearCommand(command,
                        HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName));
            }
            appendClearCommand(command,
                    HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(packageName));
        }
        if (command.length() > 0) {
            runRootCommand(command.toString());
        }
    }

    private static void appendClearCommand(StringBuilder command, String property) {
        if (command.length() > 0) {
            command.append("; ");
        }
        command.append("setprop ").append(shellQuote(property)).append(" 0");
    }

    static String buildPublishCommandForTest(String fontProperty, int fontScalePercent) {
        return buildPublishCommand(fontProperty, fontScalePercent);
    }

    static boolean shouldPreserveCompatForceFontForTest(DpisConfigStore store, String packageName) {
        return shouldPreserveCompatForceFont(store, packageName);
    }

    static boolean shouldPublishForceFontOnRecoveryForTest(DpisConfigStore store,
                                                           String packageName,
                                                           String fontMode) {
        return shouldPublishForceFontOnRecovery(store, packageName, fontMode);
    }

    private static boolean shouldPublishForceFontOnRecovery(DpisConfigStore store,
                                                           String packageName,
                                                           String fontMode) {
        if (store == null) {
            return false;
        }
        String normalizedMode = FontApplyMode.normalize(fontMode);
        if (FontApplyMode.FIELD_REWRITE.equals(normalizedMode)) {
            return true;
        }
        return FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName)
                && FontApplyMode.isEnabled(normalizedMode);
    }

    private static boolean shouldPreserveCompatForceFont(DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isBlank()
                || !store.isTargetDpisEnabled(packageName)) {
            return false;
        }
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return fontScalePercent != null
                && fontScalePercent > 0
                && FontApplyMode.FIELD_REWRITE.equals(store.getTargetFontApplyMode(packageName));
    }

    private static String buildPublishCommand(String fontProperty, int fontScalePercent) {
        return "setprop " + shellQuote(fontProperty) + " "
                + shellQuote(String.valueOf(fontScalePercent));
    }

    private static void runRootCommand(String command) {
        RootCommandRunner.run(command);
    }
    static String shellQuoteForTest(String value) {
        return shellQuote(value);
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
