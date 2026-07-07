package com.dpis.module.runtime.appprocess;

import java.util.LinkedHashSet;
import java.util.Set;

public final class WebApkCarrierResolver {
    static final String WEBAPK_PACKAGE_PREFIX = "org.chromium.webapk.";
    public static final String WEBAPK_PACKAGE_EXTRA =
            "org.chromium.chrome.browser.webapk_package_name";
    private static final String WEBAPK_URI_PREFIX = "webapp://webapk-";

    private WebApkCarrierResolver() {
    }

    public static boolean isWebApkOwnerPackage(String packageName) {
        return isLikelyPackageName(packageName)
                && packageName.startsWith(WEBAPK_PACKAGE_PREFIX);
    }

    public static Set<String> collectOwnerPackagesFromText(String text, int maxCount) {
        Set<String> owners = new LinkedHashSet<>();
        collectByPrefix(text, WEBAPK_PACKAGE_PREFIX, owners, maxCount);
        collectFromWebAppUri(text, owners, maxCount);
        return owners;
    }

    public static String ownerPackageFromText(String text) {
        for (String owner : collectOwnerPackagesFromText(text, 1)) {
            return owner;
        }
        return null;
    }

    private static void collectFromWebAppUri(String text, Set<String> output, int maxCount) {
        if (text == null || output == null || output.size() >= maxCount) {
            return;
        }
        int index = text.indexOf(WEBAPK_URI_PREFIX);
        while (index >= 0 && output.size() < maxCount) {
            int start = index + WEBAPK_URI_PREFIX.length();
            String candidate = readPackageToken(text, start);
            if (isWebApkOwnerPackage(candidate)) {
                output.add(candidate);
            }
            index = text.indexOf(WEBAPK_URI_PREFIX, start);
        }
    }

    private static void collectByPrefix(String text,
                                        String prefix,
                                        Set<String> output,
                                        int maxCount) {
        if (text == null || prefix == null || output == null || maxCount <= 0) {
            return;
        }
        int index = text.indexOf(prefix);
        while (index >= 0 && output.size() < maxCount) {
            String candidate = readPackageToken(text, index);
            if (isWebApkOwnerPackage(candidate)) {
                output.add(candidate);
            }
            index = text.indexOf(prefix, index + prefix.length());
        }
    }

    private static String readPackageToken(String text, int start) {
        if (text == null || start < 0 || start >= text.length()) {
            return null;
        }
        int end = start;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                end++;
                continue;
            }
            break;
        }
        return end > start ? text.substring(start, end) : null;
    }

    private static boolean isLikelyPackageName(String value) {
        if (value == null || value.isBlank() || value.length() > 256
                || !value.contains(".")) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) {
                return false;
            }
        }
        return Character.isLowerCase(value.charAt(0));
    }
}
