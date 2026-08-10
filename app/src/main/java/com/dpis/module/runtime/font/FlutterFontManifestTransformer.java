package com.dpis.module.runtime.font;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies the generic Flutter default-family rule without depending on a
 * target application's package name or manifest formatting.
 */
public final class FlutterFontManifestTransformer {
    private static final String DEFAULT_FAMILY = "Roboto";
    private static final String PLACEHOLDER_ASSET = "dpis/typeface.ttf";
    private static final Pattern FAMILY_PATTERN = Pattern.compile(
            "\"family\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private FlutterFontManifestTransformer() {
    }

    /**
     * Adds a manifest-owned default family only when the app does not already
     * declare that family. A null result means that the input is not a valid
     * Flutter font manifest array and must be left untouched.
     */
    public static String addDefaultFamilyIfMissing(String manifest) {
        if (manifest == null) {
            return null;
        }
        try {
            JSONArray families = new JSONArray(manifest);
            for (int index = 0; index < families.length(); index++) {
                JSONObject family = families.optJSONObject(index);
                if (family != null
                        && DEFAULT_FAMILY.equals(family.optString("family", null))) {
                    return manifest.trim();
                }
            }
            JSONObject replacementFamily = new JSONObject();
            replacementFamily.put("family", DEFAULT_FAMILY);
            JSONArray fonts = new JSONArray();
            JSONObject font = new JSONObject();
            font.put("asset", PLACEHOLDER_ASSET);
            fonts.put(font);
            replacementFamily.put("fonts", fonts);
            families.put(replacementFamily);
            return families.toString();
        } catch (Throwable ignored) {
            // Local JVM unit tests use Android's stub JSON classes. Keep the
            // same validated transformation available there without adding a
            // second JSON dependency to the app.
            return addDefaultFamilyWithValidatedArraySyntax(manifest);
        }
    }

    private static String addDefaultFamilyWithValidatedArraySyntax(String manifest) {
        String trimmed = manifest.trim();
        if (!isTopLevelArray(trimmed)) {
            return null;
        }
        Matcher matcher = FAMILY_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            if (DEFAULT_FAMILY.equals(matcher.group(1))) {
                return trimmed;
            }
        }
        String body = trimmed.substring(0, trimmed.length() - 1).trim();
        String family = "{\"family\":\"" + DEFAULT_FAMILY
                + "\",\"fonts\":[{\"asset\":\"" + PLACEHOLDER_ASSET + "\"}]}";
        return body.equals("[") ? body + family + "]" : body + "," + family + "]";
    }

    private static boolean isTopLevelArray(String value) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            return false;
        }
        int squareDepth = 0;
        int curlyDepth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    quoted = false;
                }
                continue;
            }
            if (current == '"') {
                quoted = true;
            } else if (current == '[') {
                squareDepth++;
            } else if (current == ']') {
                squareDepth--;
                if (squareDepth < 0) {
                    return false;
                }
            } else if (current == '{') {
                curlyDepth++;
            } else if (current == '}') {
                curlyDepth--;
                if (curlyDepth < 0) {
                    return false;
                }
            }
        }
        return !quoted && !escaped && squareDepth == 0 && curlyDepth == 0;
    }
}
