package com.dpis.module;

import java.io.IOException;

final class HyperOsNativeFontPropertySyncer {
    private HyperOsNativeFontPropertySyncer() {
    }

    static void publishForceFontTargetAsync(String packageName, int fontScalePercent) {
        if (packageName == null || packageName.isBlank() || fontScalePercent <= 0) {
            return;
        }
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
    static void syncConfiguredFontTargetsAsync(DpiConfigStore store) {
        if (store == null || !store.isHyperOsFlutterFontHookEnabled()) {
            return;
        }
        for (String packageName : store.getConfiguredPackages()) {
            Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
            String fontMode = store.getTargetFontApplyMode(packageName);
            if (store.isTargetDpisEnabled(packageName)
                    && fontScalePercent != null
                    && fontScalePercent > 0
                    && FontApplyMode.isEnabled(fontMode)) {
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

    static String buildPublishCommandForTest(String fontProperty, int fontScalePercent) {
        return buildPublishCommand(fontProperty, fontScalePercent);
    }

    private static String buildPublishCommand(String fontProperty, int fontScalePercent) {
        return "setprop " + shellQuote(fontProperty) + " "
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
