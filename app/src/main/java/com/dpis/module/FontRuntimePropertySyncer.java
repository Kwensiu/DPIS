package com.dpis.module;

import java.io.IOException;
import java.util.LinkedHashSet;

final class FontRuntimePropertySyncer {
    private FontRuntimePropertySyncer() {
    }

    static void publishTargetAsync(String packageName,
                                   int fontScalePercent,
                                   String mode,
                                   boolean hyperOsNativeFontHookEnabled) {
        if (packageName == null || packageName.isBlank() || fontScalePercent <= 0) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildTargetCommand(
                        packageName, fontScalePercent, mode, hyperOsNativeFontHookEnabled)),
                "DPIS-font-runtime-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void clearTargetAsync(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        Thread cleanerThread = new Thread(
                () -> runRootCommand(buildClearTargetCommand(packageName)),
                "DPIS-font-runtime-property-cleaner");
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
                        ? fontScalePercent
                        : 0;
                appendCommand(command, buildTargetCommand(
                        packageName, value, mode, store.isHyperOsFlutterFontHookEnabled()));
            }
            if (command.length() > 0) {
                runRootCommand(command.toString());
            }
        }, "DPIS-font-runtime-property-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    static String buildTargetCommandForTest(String packageName,
                                            int fontScalePercent,
                                            String mode,
                                            boolean hyperOsNativeFontHookEnabled) {
        return buildTargetCommand(packageName, fontScalePercent, mode, hyperOsNativeFontHookEnabled);
    }

    static String buildClearTargetCommandForTest(String packageName) {
        return buildClearTargetCommand(packageName);
    }

    private static String buildTargetCommand(String packageName,
                                             int fontScalePercent,
                                             String mode,
                                             boolean hyperOsNativeFontHookEnabled) {
        String normalizedMode = FontApplyMode.normalize(mode);
        boolean enabled = fontScalePercent > 0 && FontApplyMode.isEnabled(normalizedMode);
        int systemEmulationValue = enabled
                && FontApplyMode.SYSTEM_EMULATION.equals(normalizedMode) ? fontScalePercent : 0;
        // forcefont.* is the live override consumed by HyperOS native proxies and by
        // compat100 field rewrite. It must be written once with the final semantic value.
        int forceFontValue = enabled
                && (FontApplyMode.FIELD_REWRITE.equals(normalizedMode) || hyperOsNativeFontHookEnabled)
                ? fontScalePercent : 0;
        String compatMode = enabled ? normalizedMode : FontApplyMode.OFF;
        return buildSetCommand(HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName),
                systemEmulationValue)
                + "; " + buildSetCommand(
                        HyperOsFlutterFontBridge.persistentCompatFontPropertyNameForPackage(packageName),
                        systemEmulationValue)
                + "; " + buildSetCommand(
                        HyperOsFlutterFontBridge.compatFontModePropertyNameForPackage(packageName),
                        compatMode)
                + "; " + buildSetCommand(
                        HyperOsFlutterFontBridge.persistentCompatFontModePropertyNameForPackage(packageName),
                        compatMode)
                + "; " + buildSetCommand(
                        HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName),
                        forceFontValue)
                + "; " + buildSetCommand(
                        HyperOsFlutterFontBridge.persistentForcePropertyNameForPackage(packageName),
                        forceFontValue);
    }

    private static String buildClearTargetCommand(String packageName) {
        return buildSetCommand(HyperOsFlutterFontBridge.propertyNameForPackage(packageName), 0)
                + "; " + buildSetCommand(HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(packageName), 0)
                + "; " + buildTargetCommand(packageName, 0, FontApplyMode.OFF, false);
    }

    private static void appendCommand(StringBuilder command, String fragment) {
        if (fragment == null || fragment.isEmpty()) {
            return;
        }
        if (command.length() > 0) {
            command.append("; ");
        }
        command.append(fragment);
    }

    private static String buildSetCommand(String property, int value) {
        return buildSetCommand(property, String.valueOf(value));
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
