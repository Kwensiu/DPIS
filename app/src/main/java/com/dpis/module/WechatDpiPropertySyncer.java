package com.dpis.module;

import com.dpis.module.appconfig.WechatDpiConfig;
import com.dpis.module.runtime.WechatDpiPropertyBridge;

import com.dpis.module.runtime.RootCommandRunner;

final class WechatDpiPropertySyncer {
    private WechatDpiPropertySyncer() {
    }

    static void publishDpiAsync(String packageName, Integer dpi) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> RootCommandRunner.run(buildDpiCommand(packageName, dpi)),
                "DPIS-wechat-dpi-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    static void syncConfiguredTargetsAsync(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        String command = buildSyncCommand(store);
        if (command.isEmpty()) {
            return;
        }
        Thread syncThread = new Thread(() -> RootCommandRunner.run(command),
                "DPIS-wechat-dpi-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    static String buildSyncCommandForTest(DpisConfigStore store) {
        return buildSyncCommand(store);
    }

    static String buildDpiCommandForTest(String packageName, Integer dpi) {
        return buildDpiCommand(packageName, dpi);
    }

    private static String buildSyncCommand(DpisConfigStore store) {
        if (store == null) {
            return "";
        }
        Integer dpi = store.isTargetDpisEnabled(WechatDpiConfig.PACKAGE_NAME)
                ? store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME)
                : null;
        return buildDpiCommand(WechatDpiConfig.PACKAGE_NAME, dpi);
    }

    private static String buildDpiCommand(String packageName, Integer dpi) {
        Integer normalized = WechatDpiConfig.normalize(dpi);
        String value = normalized != null ? String.valueOf(normalized) : "0";
        return buildSetCommandPair(
                WechatDpiPropertyBridge.propertyNameForPackage(packageName),
                WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName),
                value);
    }

    private static String buildSetCommand(String property, String value) {
        return "setprop " + shellQuote(property) + " " + shellQuote(value);
    }

    private static String buildSetCommandPair(String property, String persistentProperty,
            String value) {
        return buildSetCommand(property, value)
                + "; "
                + buildSetCommand(persistentProperty, value);
    }

    private static String shellQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
