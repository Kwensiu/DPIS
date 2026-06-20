package com.dpis.module;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ConfigBackupCodec {
    private static final int SCHEMA_VERSION = 2;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_CREATED_AT_EPOCH_MS = "createdAtEpochMs";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_APP_VERSION_CODE = "appVersionCode";
    private static final String KEY_APP_VERSION_NAME = "appVersionName";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_PACKAGE_CONFIGS = "packageConfigs";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "value";
    private static final String[] PACKAGE_CONFIG_FIELD_KEYS = {
            "viewport.width_dp",
            "viewport.target_type",
            "viewport.scale_permille",
            "viewport.mode",
            "font.scale_percent",
            "font.typeface_id",
            "font.mode",
            "font.hook_domains",
            "target.dpis_enabled",
            "app.wechat_dpi"
    };

    private ConfigBackupCodec() {
    }

    static String encode(Map<String, Object> entries) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
        root.put(KEY_CREATED_AT_EPOCH_MS, System.currentTimeMillis());
        root.put(KEY_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
        root.put(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
        root.put(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME);

        JSONObject encodedEntries = new JSONObject();
        JSONObject encodedPackageConfigs = new JSONObject();
        List<String> keys = new ArrayList<>(entries.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object value = entries.get(key);
            if (putPackageConfigEntry(encodedPackageConfigs, key, value)) {
                continue;
            }
            JSONObject encoded = encodeValue(value);
            if (encoded != null) {
                encodedEntries.put(key, encoded);
            }
        }
        root.put(KEY_ENTRIES, encodedEntries);
        if (encodedPackageConfigs.length() > 0) {
            root.put(KEY_PACKAGE_CONFIGS, encodedPackageConfigs);
        }
        return root.toString(2);
    }

    static Map<String, Object> decode(String rawJson) throws JSONException {
        JSONObject root = new JSONObject(rawJson);
        int schemaVersion = root.optInt(KEY_SCHEMA_VERSION, -1);
        if (schemaVersion == 1) {
            return decodeSchemaV1(root);
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported backup schema version: " + schemaVersion);
        }
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        JSONObject encodedPackageConfigs = root.optJSONObject(KEY_PACKAGE_CONFIGS);
        if (encodedPackageConfigs != null) {
            decodePackageConfigsInto(entries, encodedPackageConfigs);
        }
        JSONObject encodedEntries = root.optJSONObject(KEY_ENTRIES);
        if (encodedEntries != null) {
            decodeEntrySectionInto(entries, encodedEntries);
        }
        if (encodedEntries == null && encodedPackageConfigs == null) {
            throw new IllegalArgumentException("Missing entries section");
        }
        return entries;
    }

    private static Map<String, Object> decodeSchemaV1(JSONObject root) throws JSONException {
        JSONObject encodedEntries = root.optJSONObject(KEY_ENTRIES);
        if (encodedEntries == null) {
            throw new IllegalArgumentException("Missing entries section");
        }
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        decodeEntrySectionInto(entries, encodedEntries);
        return entries;
    }

    private static void decodeEntrySectionInto(Map<String, Object> entries, JSONObject encodedEntries)
            throws JSONException {
        Iterator<String> keys = encodedEntries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key == null || key.isEmpty()) {
                continue;
            }
            JSONObject encodedValue = encodedEntries.optJSONObject(key);
            if (encodedValue == null) {
                throw new IllegalArgumentException("Invalid entry payload for key: " + key);
            }
            entries.put(key, decodeEntryValue(encodedValue));
        }
    }

    private static boolean putPackageConfigEntry(
            JSONObject encodedPackageConfigs,
            String key,
            Object value) throws JSONException {
        String prefix = "package_config.";
        if (!key.startsWith(prefix)) {
            return false;
        }
        String remainder = key.substring(prefix.length());
        String fieldKey = packageConfigFieldKeyFromRemainder(remainder);
        if (fieldKey == null) {
            return false;
        }
        String packageName = remainder.substring(0, remainder.length() - fieldKey.length() - 1);
        if (packageName.isEmpty() || fieldKey.isEmpty()) {
            return false;
        }
        JSONObject packageEntries = encodedPackageConfigs.optJSONObject(packageName);
        if (packageEntries == null) {
            packageEntries = new JSONObject();
            encodedPackageConfigs.put(packageName, packageEntries);
        }
        packageEntries.put(fieldKey, value);
        return true;
    }

    private static String packageConfigFieldKeyFromRemainder(String remainder) {
        if (remainder == null || remainder.isEmpty()) {
            return null;
        }
        for (String fieldKey : PACKAGE_CONFIG_FIELD_KEYS) {
            if (remainder.endsWith("." + fieldKey)) {
                return fieldKey;
            }
        }
        return null;
    }

    private static void decodePackageConfigsInto(
            Map<String, Object> entries,
            JSONObject encodedPackageConfigs) throws JSONException {
        Iterator<String> packageNames = encodedPackageConfigs.keys();
        while (packageNames.hasNext()) {
            String packageName = packageNames.next();
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }
            JSONObject packageEntries = encodedPackageConfigs.optJSONObject(packageName);
            if (packageEntries == null) {
                throw new IllegalArgumentException(
                        "Invalid package config payload for package: " + packageName);
            }
            Iterator<String> fieldKeys = packageEntries.keys();
            while (fieldKeys.hasNext()) {
                String fieldKey = fieldKeys.next();
                if (fieldKey == null || fieldKey.isEmpty()) {
                    continue;
                }
                entries.put("package_config." + packageName + "." + fieldKey, packageEntries.get(fieldKey));
            }
        }
    }

    private static JSONObject encodeValue(Object value) throws JSONException {
        if (value == null) {
            return null;
        }
        JSONObject encoded = new JSONObject();
        if (value instanceof String typed) {
            encoded.put(KEY_TYPE, "string");
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Integer typed) {
            encoded.put(KEY_TYPE, "int");
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Long typed) {
            encoded.put(KEY_TYPE, "long");
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Float typed) {
            encoded.put(KEY_TYPE, "float");
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Boolean typed) {
            encoded.put(KEY_TYPE, "boolean");
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Set<?> typed) {
            List<String> values = new ArrayList<>();
            for (Object item : typed) {
                if (item instanceof String text) {
                    values.add(text);
                }
            }
            Collections.sort(values);
            encoded.put(KEY_TYPE, "string_set");
            encoded.put(KEY_VALUE, values);
            return encoded;
        }
        return null;
    }

    private static Object decodeEntryValue(JSONObject encoded) throws JSONException {
        String type = encoded.optString(KEY_TYPE, "");
        return switch (type) {
            case "string" -> encoded.optString(KEY_VALUE, "");
            case "int" -> encoded.getInt(KEY_VALUE);
            case "long" -> encoded.getLong(KEY_VALUE);
            case "float" -> (float) encoded.getDouble(KEY_VALUE);
            case "boolean" -> encoded.getBoolean(KEY_VALUE);
            case "string_set" -> decodeStringSet(encoded.getJSONArray(KEY_VALUE));
            default -> throw new IllegalArgumentException("Unsupported backup value type: " + type);
        };
    }

    private static Set<String> decodeStringSet(org.json.JSONArray array) throws JSONException {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }
}
