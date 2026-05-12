package com.dpis.module;

import java.io.IOException;
import java.util.LinkedHashSet;

final class ViewportPropertySyncer {
    private ViewportPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, int widthDp) {
        publishTargetAsync(packageName, widthDp, ViewportApplyMode.SYSTEM_EMULATION);
    }

    static void publishTargetAsync(String packageName, int widthDp, String mode) {
        if (packageName == null || packageName.isBlank() || widthDp <= 0) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildCompatConfigCommand(packageName, widthDp, mode)),
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
                Integer widthDp = store.getTargetViewportWidthDp(packageName);
                String mode = store.getTargetViewportApplyMode(packageName);
                if (command.length() > 0) {
                    command.append("; ");
                }
                int targetWidthDp = widthDp != null ? widthDp : 0;
                command.append(buildCompatConfigCommand(
                        packageName,
                        store.isTargetDpisEnabled(packageName) ? targetWidthDp : 0,
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

    private static String buildCompatConfigCommand(String packageName, int widthDp, String mode) {
        String normalizedMode = ViewportApplyMode.normalize(mode);
        boolean enabled = widthDp > 0 && ViewportApplyMode.isEnabled(normalizedMode);
        // vp.* drives system emulation. vpcfg/vpmode preserve compat100 config
        // for field_rewrite without accidentally enabling emulation.
        int systemEmulationValue = enabled
                && ViewportApplyMode.SYSTEM_EMULATION.equals(normalizedMode) ? widthDp : 0;
        int compatConfigValue = enabled ? widthDp : 0;
        String compatMode = enabled ? normalizedMode : ViewportApplyMode.OFF;
        return buildSetCommand(ViewportPropertyBridge.propertyNameForPackage(packageName),
                systemEmulationValue)
                + "; " + buildSetCommand(
                        ViewportPropertyBridge.compatConfigPropertyNameForPackage(packageName),
                        compatConfigValue)
                + "; " + buildSetCommand(
                        ViewportPropertyBridge.compatModePropertyNameForPackage(packageName),
                        compatMode);
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

    private static void runRootCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
            process.waitFor();
        } catch (IOException ignored) {
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
