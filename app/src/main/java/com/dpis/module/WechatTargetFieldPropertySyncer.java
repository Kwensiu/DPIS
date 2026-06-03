package com.dpis.module;

import java.util.LinkedHashSet;

final class WechatTargetFieldPropertySyncer {

    private WechatTargetFieldPropertySyncer() {
    }

    static void publishTargetAsync(String packageName, Integer targetField) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return;
        }
        Thread publisherThread = new Thread(
                () -> RootCommandRunner.run(buildTargetCommand(packageName, targetField)),
                "DPIS-wechat-target-field-publisher");
        publisherThread.setDaemon(true);
        publisherThread.start();
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
                if (!WechatTargetFieldConfig.appliesTo(packageName)) {
                    continue;
                }
                Integer targetField = store.isTargetDpisEnabled(packageName)
                        ? store.getWechatTargetField(packageName)
                        : null;
                if (command.length() > 0) {
                    command.append("; ");
                }
                command.append(buildTargetCommand(packageName, targetField));
            }
            if (command.length() > 0) {
                RootCommandRunner.run(command.toString());
            }
        }, "DPIS-wechat-target-field-syncer");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    static String buildTargetCommandForTest(String packageName, Integer targetField) {
        return buildTargetCommand(packageName, targetField);
    }

    private static String buildTargetCommand(String packageName, Integer targetField) {
        Integer normalized = WechatTargetFieldConfig.normalize(targetField);
        String value = normalized != null ? String.valueOf(normalized) : "0";
        return buildSetCommandPair(
                WechatTargetFieldPropertyBridge.propertyNameForPackage(packageName),
                WechatTargetFieldPropertyBridge.persistentPropertyNameForPackage(packageName),
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
