package com.dpis.module;

import java.io.IOException;

final class ViewportPropertySyncer {
    private ViewportPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, int widthDp) {
        if (packageName == null || packageName.isBlank() || widthDp <= 0) {
            return;
        }
        String property = ViewportPropertyBridge.propertyNameForPackage(packageName);
        Thread publisherThread = new Thread(() -> setPropertyWithRoot(property, widthDp),
                "DPIS-viewport-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        String property = ViewportPropertyBridge.propertyNameForPackage(packageName);
        Thread cleanerThread = new Thread(() -> setPropertyWithRoot(property, 0),
                "DPIS-viewport-property-cleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    static String buildSetCommandForTest(String property, int widthDp) {
        return buildSetCommand(property, widthDp);
    }

    private static void setPropertyWithRoot(String property, int widthDp) {
        runRootCommand(buildSetCommand(property, widthDp));
    }

    private static String buildSetCommand(String property, int widthDp) {
        return "setprop " + shellQuote(property) + " " + shellQuote(String.valueOf(widthDp));
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
