package com.dpis.module.runtime.font;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.HyperOsFlutterFontBridge;

import com.dpis.module.fonts.FontApplyMode;




import com.dpis.module.runtime.RootCommandRunner;

import java.util.LinkedHashSet;

public final class CompatFontPropertySyncer {
    private CompatFontPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, int fontScalePercent) {
        publishTargetAsync(packageName, fontScalePercent, FontApplyMode.SYSTEM_EMULATION);
    }

    static void publishTargetAsync(String packageName, int fontScalePercent, String mode) {
        if (packageName == null || packageName.isBlank() || fontScalePercent <= 0) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildCompatConfigCommand(packageName, fontScalePercent, mode)),
                "DPIS-compat-font-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(
                () -> runRootCommand(buildClearCommand(packageName)),
                "DPIS-compat-font-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    static void syncConfiguredTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(store.getConfiguredPackages());
        if (packages.isEmpty()) {
            return;
        }
        Thread syncThread = new Thread(() -> {
            StringBuilder command = new StringBuilder();
            for (String packageName : packages) {
                Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
                String mode = store.getTargetFontApplyMode(packageName);
                int value = store.isTargetDpisEnabled(packageName)
                        && fontScalePercent != null
                        && fontScalePercent > 0
                        ? fontScalePercent
                        : 0;
                if (command.length() > 0) {
                    command.append("; ");
                }
                command.append(buildCompatConfigCommand(packageName, value, mode));
            }
            if (command.length() > 0) {
                runRootCommand(command.toString());
            }
        }, "DPIS-compat-font-property-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    public static String buildSetCommandForTest(String property, int fontScalePercent) {
        return buildSetCommand(property, fontScalePercent);
    }

    public static String buildCompatConfigCommandForTest(String packageName, int fontScalePercent, String mode) {
        return buildCompatConfigCommand(packageName, fontScalePercent, mode);
    }

    private static String buildCompatConfigCommand(String packageName, int fontScalePercent, String mode) {
        String normalizedMode = FontApplyMode.normalize(mode);
        boolean enabled = fontScalePercent > 0 && FontApplyMode.isEnabled(normalizedMode);
        // compatfont.* keeps legacy system-emulation behavior. Field rewrite
        // reads forcefont.* plus fontmode.*, so compatfont.* must stay 0.
        int systemEmulationValue = enabled
                && FontApplyMode.SYSTEM_EMULATION.equals(normalizedMode) ? fontScalePercent : 0;
        int forceFontValue = enabled
                && FontApplyMode.FIELD_REWRITE.equals(normalizedMode) ? fontScalePercent : 0;
        String compatMode = enabled ? normalizedMode : FontApplyMode.OFF;
        return buildSetCommandPair(
                        HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName),
                        HyperOsFlutterFontBridge.persistentCompatFontPropertyNameForPackage(packageName),
                        systemEmulationValue)
                + "; " + buildSetCommandPair(
                        HyperOsFlutterFontBridge.compatFontModePropertyNameForPackage(packageName),
                        HyperOsFlutterFontBridge.persistentCompatFontModePropertyNameForPackage(packageName),
                        compatMode)
                + "; " + buildSetCommandPair(
                        HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName),
                        HyperOsFlutterFontBridge.persistentForcePropertyNameForPackage(packageName),
                        forceFontValue);
    }

    private static String buildClearCommand(String packageName) {
        return buildCompatConfigCommand(packageName, 0, FontApplyMode.OFF);
    }

    private static String buildSetCommand(String property, int fontScalePercent) {
        return "setprop " + shellQuote(property) + " "
                + shellQuote(String.valueOf(fontScalePercent));
    }

    private static String buildSetCommand(String property, String value) {
        return "setprop " + shellQuote(property) + " " + shellQuote(value);
    }

    private static String buildSetCommandPair(String property, String persistentProperty, int fontScalePercent) {
        return buildSetCommandPair(property, persistentProperty, String.valueOf(fontScalePercent));
    }

    private static String buildSetCommandPair(String property, String persistentProperty, String value) {
        return buildSetCommand(property, value)
                + "; " + buildSetCommand(persistentProperty, value);
    }

    private static void runRootCommand(String command) {
        RootCommandRunner.run(command);
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
