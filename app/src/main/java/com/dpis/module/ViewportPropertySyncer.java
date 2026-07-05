package com.dpis.module;

import com.dpis.module.runtime.RootCommandRunner;

import java.util.LinkedHashSet;

public final class ViewportPropertySyncer {
    private ViewportPropertySyncer() {
    }

    public static void publishTargetAsync(String packageName, int widthDp) {
        publishTargetAsync(packageName, widthDp, ViewportApplyMode.SYSTEM_EMULATION);
    }

    public static void publishTargetAsync(String packageName, int widthDp, String mode) {
        publishTargetAsync(packageName, ViewportTargetSpec.absoluteDp(widthDp), mode);
    }

    public static void publishTargetAsync(String packageName, ViewportTargetSpec targetSpec, String mode) {
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

    public static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(() -> runRootCommand(buildClearCommand(packageName)),
                "DPIS-viewport-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    public static void syncConfiguredTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        Thread syncThread = new Thread(() -> syncConfiguredTargets(store),
                "DPIS-viewport-property-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    public static void syncConfiguredTargets(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        String command = buildConfiguredTargetsCommand(store);
        if (!command.isEmpty()) {
            runRootCommand(command);
        }
    }

    public static void syncTarget(String packageName, DpisConfigStore store) {
        if (packageName == null || packageName.isBlank() || store == null) {
            return;
        }
        ViewportTargetSpec targetSpec = store.getTargetViewportSpec(packageName);
        String mode = store.getTargetViewportApplyMode(packageName);
        runRootCommand(buildCompatConfigCommand(
                packageName,
                store.isTargetDpisEnabled(packageName) ? targetSpec : ViewportTargetSpec.off(),
                mode));
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
                        enc.scaleMilliPercent)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.compatConfigPropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentCompatConfigPropertyNameForPackage(packageName),
                        enc.compatConfigValue)
                + "; " + buildSetCommandPair(
                        ViewportPropertyBridge.compatModePropertyNameForPackage(packageName),
                        ViewportPropertyBridge.persistentCompatModePropertyNameForPackage(packageName),
                        enc.compatMode);
    }

    private static String buildConfiguredTargetsCommand(DpisConfigStore store) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(store.getConfiguredPackages());
        if (packages.isEmpty()) {
            return "";
        }
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
        return command.toString();
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
