package com.dpis.module;

import java.util.Locale;

final class WechatTargetFieldPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.wechat.targetfield.";

    private WechatTargetFieldPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + suffixForPackage(packageName);
    }

    private static String suffixForPackage(String packageName) {
        return String.format(Locale.US, "%08x", packageName.hashCode());
    }
}
