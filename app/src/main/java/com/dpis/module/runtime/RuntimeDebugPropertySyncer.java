package com.dpis.module.runtime;

public final class RuntimeDebugPropertySyncer {
    private RuntimeDebugPropertySyncer() {
    }

    public static void publishAsync(boolean globalLogEnabled, boolean fontDebugOverlayEnabled) {
        Thread publisherThread = new Thread(
                () -> runRootCommand(buildPublishCommand(globalLogEnabled, fontDebugOverlayEnabled)),
                "DPIS-runtime-debug-property-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static String buildPublishCommandForTest(boolean globalLogEnabled,
                                             boolean fontDebugOverlayEnabled) {
        return buildPublishCommand(globalLogEnabled, fontDebugOverlayEnabled);
    }

    private static String buildPublishCommand(boolean globalLogEnabled,
                                             boolean fontDebugOverlayEnabled) {
        String globalLogValue = globalLogEnabled ? "1" : "0";
        String overlayValue = fontDebugOverlayEnabled ? "1" : "0";
        return buildSetCommand(RuntimeDebugPropertyBridge.globalLogPropertyName(), globalLogValue)
                + "; " + buildSetCommand(
                        RuntimeDebugPropertyBridge.persistentGlobalLogPropertyName(),
                        globalLogValue)
                + "; " + buildSetCommand(
                        RuntimeDebugPropertyBridge.fontDebugOverlayPropertyName(),
                        overlayValue)
                + "; " + buildSetCommand(
                        RuntimeDebugPropertyBridge.persistentFontDebugOverlayPropertyName(),
                        overlayValue);
    }

    private static String buildSetCommand(String property, String value) {
        return "setprop " + shellQuote(property) + " " + shellQuote(value);
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
