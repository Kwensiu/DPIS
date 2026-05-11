package com.dpis.module;

import java.io.IOException;
import java.util.LinkedHashSet;

final class CompatFontPropertySyncer {
    private CompatFontPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, int fontScalePercent) {
        if (packageName == null || packageName.isBlank() || fontScalePercent <= 0) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> setPropertyWithRoot(packageName, fontScalePercent),
                "DPIS-compat-font-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(
                () -> setPropertyWithRoot(packageName, 0),
                "DPIS-compat-font-property-cleaner");
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
                Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
                String mode = store.getTargetFontApplyMode(packageName);
                int value = store.isTargetDpisEnabled(packageName)
                        && fontScalePercent != null
                        && fontScalePercent > 0
                        && FontApplyMode.SYSTEM_EMULATION.equals(FontApplyMode.normalize(mode))
                        ? fontScalePercent
                        : 0;
                if (command.length() > 0) {
                    command.append("; ");
                }
                command.append(buildSetCommand(
                        HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName),
                        value));
            }
            if (command.length() > 0) {
                runRootCommand(command.toString());
            }
        }, "DPIS-compat-font-property-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    static String buildSetCommandForTest(String property, int fontScalePercent) {
        return buildSetCommand(property, fontScalePercent);
    }

    private static void setPropertyWithRoot(String packageName, int fontScalePercent) {
        runRootCommand(buildSetCommand(
                HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName),
                fontScalePercent));
    }

    private static String buildSetCommand(String property, int fontScalePercent) {
        return "setprop " + shellQuote(property) + " "
                + shellQuote(String.valueOf(fontScalePercent));
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
