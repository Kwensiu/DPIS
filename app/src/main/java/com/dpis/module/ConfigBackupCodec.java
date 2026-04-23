package com.dpis.module;

import org.json.JSONArray;
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
    private static final int SCHEMA_VERSION = 1;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_CREATED_AT_EPOCH_MS = "createdAtEpochMs";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_APP_VERSION_CODE = "appVersionCode";
    private static final String KEY_APP_VERSION_NAME = "appVersionName";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "value";

    private static final String TYPE_STRING = "string";
    private static final String TYPE_STRING_SET = "string_set";
    private static final String TYPE_INT = "int";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_BOOLEAN = "boolean";

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
        List<String> keys = new ArrayList<>(entries.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            JSONObject encoded = encodeValue(entries.get(key));
            if (encoded != null) {
                encodedEntries.put(key, encoded);
            }
        }
        root.put(KEY_ENTRIES, encodedEntries);
        return root.toString(2);
    }

    static Map<String, Object> decode(String rawJson) throws JSONException {
        JSONObject root = new JSONObject(rawJson);
        int schemaVersion = root.optInt(KEY_SCHEMA_VERSION, -1);
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported backup schema version: " + schemaVersion);
        }
        JSONObject encodedEntries = root.optJSONObject(KEY_ENTRIES);
        if (encodedEntries == null) {
            throw new IllegalArgumentException("Missing entries section");
        }
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
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
            entries.put(key, decodeValue(encodedValue));
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject encodeValue(Object value) throws JSONException {
        if (value == null) {
            return null;
        }
        JSONObject encoded = new JSONObject();
        if (value instanceof String typed) {
            encoded.put(KEY_TYPE, TYPE_STRING);
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Integer typed) {
            encoded.put(KEY_TYPE, TYPE_INT);
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Long typed) {
            encoded.put(KEY_TYPE, TYPE_LONG);
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Float typed) {
            encoded.put(KEY_TYPE, TYPE_FLOAT);
            encoded.put(KEY_VALUE, typed);
            return encoded;
        }
        if (value instanceof Boolean typed) {
            encoded.put(KEY_TYPE, TYPE_BOOLEAN);
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
            JSONArray array = new JSONArray();
            for (String item : values) {
                array.put(item);
            }
            encoded.put(KEY_TYPE, TYPE_STRING_SET);
            encoded.put(KEY_VALUE, array);
            return encoded;
        }
        return null;
    }

    private static Object decodeValue(JSONObject encoded) throws JSONException {
        String type = encoded.optString(KEY_TYPE, "");
        return switch (type) {
            case TYPE_STRING -> encoded.optString(KEY_VALUE, "");
            case TYPE_INT -> encoded.getInt(KEY_VALUE);
            case TYPE_LONG -> encoded.getLong(KEY_VALUE);
            case TYPE_FLOAT -> (float) encoded.getDouble(KEY_VALUE);
            case TYPE_BOOLEAN -> encoded.getBoolean(KEY_VALUE);
            case TYPE_STRING_SET -> decodeStringSet(encoded.getJSONArray(KEY_VALUE));
            default -> throw new IllegalArgumentException("Unsupported backup value type: " + type);
        };
    }

    private static Set<String> decodeStringSet(JSONArray array) throws JSONException {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }
}
