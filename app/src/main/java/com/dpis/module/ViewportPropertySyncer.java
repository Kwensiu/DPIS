package com.dpis.module;

import java.util.LinkedHashSet;

final class ViewportPropertySyncer {
    private ViewportPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, int widthDp) {
        publishTargetAsync(packageName, widthDp, ViewportApplyMode.SYSTEM_EMULATION);
    }

    static void publishTargetAsync(String packageName, int widthDp, String mode) {
        publishTargetAsync(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode);
    }

    static void publishTargetAsync(String packageName, ViewportTargetSpec targetSpec, String mode) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        if (targetSpec == null || !targetSpec.isEnabled()) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildCompatConfigCommand(packageName, targetSpec, mode)),
                "DPIS-viewport-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(() -> runRootCommand(buildClearCommand(packageName)),
                "DPIS-viewport-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    static void syncConfiguredTargetsAsync(DpiConfigStore store) {
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
                ViewportTargetSpec targetSpec = store.getTargetViewportSpec(packageName);
                String mode = store.getTargetViewportApplyMode(packageName);
                if (command.length() > 0) {
                    command.append("; ");
                }
                command.append(buildCompatConfigCommand(
                        packageName,
                        store.isTargetDpisEnabled(packageName) ? targetSpec : ViewportTargetSpec.off(),
                        mode));
            }
            if (command.length() > 0) {
                runRootCommand(command.toString());
            }
        }, "DPIS-viewport-property-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    static String buildSetCommandForTest(String property, int widthDp) {
        return buildSetCommand(property, widthDp);
    }

    static String buildCompatConfigCommandForTest(String packageName, int widthDp, String mode) {
        return buildCompatConfigCommand(packageName, widthDp, mode);
    }

    static String buildCompatConfigCommandForTest(String packageName,
                                                  ViewportTargetSpec targetSpec,
                                                  String mode) {
        return buildCompatConfigCommand(packageName, targetSpec, mode);
    }

    private static String buildCompatConfigCommand(String packageName, int widthDp, String mode) {
        return buildCompatConfigCommand(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode);
    }

    private static String buildCompatConfigCommand(String packageName,
                                                   ViewportTargetSpec targetSpec,
                                                   String mode) {
        ViewportPropertyProjection.Encoded enc = ViewportPropertyProjection.encode(targetSpec, mode);
        return buildSetCommandPair(
                        ViewportPropertyBridge.propertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentPropertyNameForPackage(packageName),
                        enc.systemEmulationValue)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.targetTypePropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentTargetTypePropertyNameForPackage(packageName),
                        enc.targetType)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.scalePropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentScalePropertyNameForPackage(packageName),
                        enc.scalePermille)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.compatConfigPropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentCompatConfigPropertyNameForPackage(packageName),
                        enc.compatConfigValue)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.compatModePropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentCompatModePropertyNameForPackage(packageName),
                        enc.compatMode);
    }

    private static String buildClearCommand(String packageName) {
        return buildCompatConfigCommand(packageName, 0, ViewportApplyMode.OFF);
    }

    private static String buildSetCommand(String property, int widthDp) {
        return "setprop " + shellQuote(property) + " " + shellQuote(String.valueOf(widthDp));
    }

    private static String buildSetCommand(String property, String value) {
        return "setprop " + shellQuote(property) + " " + shellQuote(value);
    }

    private static String buildSetCommandPair(String property, String persistentProperty, int widthDp) {
        return buildSetCommandPair(property, persistentProperty, String.valueOf(widthDp));
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
