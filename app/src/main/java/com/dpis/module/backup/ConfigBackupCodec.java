package com.dpis.module.backup;

import com.dpis.module.BuildConfig;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ConfigBackupCodec {
    private static final int SCHEMA_VERSION = 3;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_CREATED_AT_EPOCH_MS = "createdAtEpochMs";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_APP_VERSION_CODE = "appVersionCode";
    private static final String KEY_APP_VERSION_NAME = "appVersionName";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_PACKAGE_CONFIGS = "packageConfigs";
    private static final String KEY_RESOLUTION_CONFIGS = "resolutionConfigs";
    private static final String KEY_GLOBAL = "global";
    private static final String KEY_DEFAULT_PREFILL = "defaultPrefill";
    private static final String KEY_TEMPLATES = "templates";
    private static final String KEY_TEMPLATE_META = "_meta";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "value";
    private static final String[] PACKAGE_CONFIG_FIELD_KEYS = {
            "viewport.width_dp",
            "viewport.target_type",
            "viewport.scale_permille",
            "viewport.scale_milli_percent",
            "viewport.mode",
            "font.scale_percent",
            "font.typeface_id",
            "font.mode",
            "font.hook_domains",
            "target.dpis_enabled",
            "app.wechat_dpi"
    };
    private static final String[] RESOLUTION_CONFIG_FIELD_KEYS = {
            "width_px",
            "height_px",
            "mode"
    };

    private ConfigBackupCodec() {
    }

    public static String encode(Map<String, Object> entries) throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
        root.put(KEY_CREATED_AT_EPOCH_MS, System.currentTimeMillis());
        root.put(KEY_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
        root.put(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
        root.put(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME);

        JSONObject encodedPackageConfigs = new JSONObject();
        JSONObject encodedResolutionConfigs = new JSONObject();
        JSONObject encodedGlobal = new JSONObject();
        JSONObject encodedDefaultPrefill = new JSONObject();
        JSONObject encodedTemplates = new JSONObject();
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
            if (putPackageOwnedConfigEntry(
                    encodedResolutionConfigs,
                    "resolution.",
                    RESOLUTION_CONFIG_FIELD_KEYS,
                    key,
                    value)) {
                continue;
            }
            if (putDefaultPrefillEntry(encodedDefaultPrefill, key, value)) {
                continue;
            }
            if (putTemplateEntry(encodedTemplates, key, value)) {
                continue;
            }
            Object encoded = encodeDirectValue(value);
            if (encoded != null) {
                putNestedValue(encodedGlobal, key, encoded);
            }
        }
        if (encodedPackageConfigs.length() > 0) {
            root.put(KEY_PACKAGE_CONFIGS, encodedPackageConfigs);
        }
        if (encodedResolutionConfigs.length() > 0) {
            root.put(KEY_RESOLUTION_CONFIGS, encodedResolutionConfigs);
        }
        if (encodedGlobal.length() > 0) {
            root.put(KEY_GLOBAL, encodedGlobal);
        }
        if (encodedDefaultPrefill.length() > 0) {
            root.put(KEY_DEFAULT_PREFILL, encodedDefaultPrefill);
        }
        if (encodedTemplates.length() > 0) {
            root.put(KEY_TEMPLATES, encodedTemplates);
        }
        return root.toString(2);
    }

    public static Map<String, Object> decode(String rawJson) throws JSONException {
        JSONObject root = new JSONObject(rawJson);
        int schemaVersion = root.optInt(KEY_SCHEMA_VERSION, -1);
        if (schemaVersion == 1) {
            return decodeSchemaV1(root);
        }
        if (schemaVersion == 2) {
            return decodeSchemaV2(root);
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported backup schema version: " + schemaVersion);
        }
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        JSONObject encodedPackageConfigs = root.optJSONObject(KEY_PACKAGE_CONFIGS);
        if (encodedPackageConfigs != null) {
            decodePackageConfigsInto(entries, encodedPackageConfigs);
        }
        JSONObject encodedResolutionConfigs = root.optJSONObject(KEY_RESOLUTION_CONFIGS);
        if (encodedResolutionConfigs != null) {
            decodePackageOwnedConfigsInto(entries, "resolution.", encodedResolutionConfigs);
        }
        JSONObject encodedGlobal = root.optJSONObject(KEY_GLOBAL);
        if (encodedGlobal != null) {
            decodeDirectSectionInto(entries, "", encodedGlobal);
        }
        JSONObject encodedDefaultPrefill = root.optJSONObject(KEY_DEFAULT_PREFILL);
        if (encodedDefaultPrefill != null) {
            decodeDirectSectionInto(entries, "default_config.", encodedDefaultPrefill);
        }
        JSONObject encodedTemplates = root.optJSONObject(KEY_TEMPLATES);
        if (encodedTemplates != null) {
            decodeTemplatesInto(entries, encodedTemplates);
        }
        if (encodedPackageConfigs == null
                && encodedResolutionConfigs == null
                && encodedGlobal == null
                && encodedDefaultPrefill == null
                && encodedTemplates == null) {
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

    private static Map<String, Object> decodeSchemaV2(JSONObject root) throws JSONException {
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
        String domain = fieldKey.substring(0, fieldKey.indexOf('.'));
        String field = fieldKey.substring(domain.length() + 1);
        JSONObject domainEntries = packageEntries.optJSONObject(domain);
        if (domainEntries == null) {
            domainEntries = new JSONObject();
            packageEntries.put(domain, domainEntries);
        }
        Object encoded = encodeDirectValue(value);
        if (encoded != null) {
            putNestedValue(domainEntries, field, encoded);
        }
        return true;
    }

    private static boolean putPackageOwnedConfigEntry(
            JSONObject encodedPackages,
            String prefix,
            String[] fieldKeys,
            String key,
            Object value) throws JSONException {
        if (!key.startsWith(prefix)) {
            return false;
        }
        String remainder = key.substring(prefix.length());
        String fieldKey = fieldKeyFromRemainder(remainder, fieldKeys);
        if (fieldKey == null) {
            return false;
        }
        String packageName = remainder.substring(0, remainder.length() - fieldKey.length() - 1);
        if (packageName.isEmpty()) {
            return false;
        }
        Object encoded = encodeDirectValue(value);
        if (encoded == null) {
            return true;
        }
        JSONObject packageEntries = encodedPackages.optJSONObject(packageName);
        if (packageEntries == null) {
            packageEntries = new JSONObject();
            encodedPackages.put(packageName, packageEntries);
        }
        putNestedValue(packageEntries, fieldKey, encoded);
        return true;
    }

    private static boolean putDefaultPrefillEntry(
            JSONObject encodedDefaultPrefill,
            String key,
            Object value) throws JSONException {
        String prefix = "default_config.";
        if (!key.startsWith(prefix)) {
            return false;
        }
        Object encoded = encodeDirectValue(value);
        if (encoded != null) {
            putNestedValue(encodedDefaultPrefill, key.substring(prefix.length()), encoded);
        }
        return true;
    }

    private static boolean putTemplateEntry(
            JSONObject encodedTemplates,
            String key,
            Object value) throws JSONException {
        String prefix = "template.";
        if (!key.startsWith(prefix)) {
            return false;
        }
        Object encoded = encodeDirectValue(value);
        if (encoded == null) {
            return true;
        }
        String remainder = key.substring(prefix.length());
        if ("ids".equals(remainder) || "order".equals(remainder)) {
            JSONObject meta = encodedTemplates.optJSONObject(KEY_TEMPLATE_META);
            if (meta == null) {
                meta = new JSONObject();
                encodedTemplates.put(KEY_TEMPLATE_META, meta);
            }
            meta.put(remainder, encoded);
            return true;
        }
        int dot = remainder.indexOf('.');
        if (dot <= 0 || dot == remainder.length() - 1) {
            return true;
        }
        String templateId = remainder.substring(0, dot);
        String templateKey = remainder.substring(dot + 1);
        JSONObject template = encodedTemplates.optJSONObject(templateId);
        if (template == null) {
            template = new JSONObject();
            encodedTemplates.put(templateId, template);
        }
        putNestedValue(template, templateKey, encoded);
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

    private static String fieldKeyFromRemainder(String remainder, String[] fieldKeys) {
        if (remainder == null || remainder.isEmpty()) {
            return null;
        }
        for (String fieldKey : fieldKeys) {
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
                String domain = fieldKeys.next();
                if (domain == null || domain.isEmpty()) {
                    continue;
                }
                Object domainValue = packageEntries.get(domain);
                if (domainValue instanceof JSONObject domainEntries) {
                    decodeDirectSectionInto(entries,
                            "package_config." + packageName + "." + domain + ".",
                            domainEntries);
                } else {
                    entries.put("package_config." + packageName + "." + domain,
                            decodeDirectValue(domainValue));
                }
            }
        }
    }

    private static void decodeTemplatesInto(
            Map<String, Object> entries,
            JSONObject encodedTemplates) throws JSONException {
        Iterator<String> templateIds = encodedTemplates.keys();
        while (templateIds.hasNext()) {
            String templateId = templateIds.next();
            if (templateId == null || templateId.isEmpty()) {
                continue;
            }
            JSONObject template = encodedTemplates.optJSONObject(templateId);
            if (template == null) {
                throw new IllegalArgumentException(
                        "Invalid template payload for template: " + templateId);
            }
            if (KEY_TEMPLATE_META.equals(templateId)) {
                decodeDirectSectionInto(entries, "template.", template);
            } else {
                decodeDirectSectionInto(entries, "template." + templateId + ".", template);
            }
        }
    }

    private static void decodePackageOwnedConfigsInto(
            Map<String, Object> entries,
            String prefix,
            JSONObject encodedPackages) throws JSONException {
        Iterator<String> packageNames = encodedPackages.keys();
        while (packageNames.hasNext()) {
            String packageName = packageNames.next();
            if (packageName == null || packageName.isEmpty()) {
                continue;
            }
            JSONObject packageEntries = encodedPackages.optJSONObject(packageName);
            if (packageEntries == null) {
                throw new IllegalArgumentException(
                        "Invalid package-owned config payload for package: " + packageName);
            }
            decodeDirectSectionInto(entries, prefix + packageName + ".", packageEntries);
        }
    }

    private static void decodeDirectSectionInto(
            Map<String, Object> entries,
            String prefix,
            JSONObject section) throws JSONException {
        decodeDirectSectionInto(entries, prefix, "", section);
    }

    private static void decodeDirectSectionInto(
            Map<String, Object> entries,
            String prefix,
            String path,
            JSONObject section) throws JSONException {
        Iterator<String> keys = section.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object value = section.get(key);
            String nextPath = path.isEmpty() ? key : path + "." + key;
            if (value instanceof JSONObject nested) {
                decodeDirectSectionInto(entries, prefix, nextPath, nested);
            } else {
                entries.put(prefix + nextPath, decodeDirectValue(value));
            }
        }
    }

    private static void putNestedValue(JSONObject section, String path, Object value)
            throws JSONException {
        int dot = path.indexOf('.');
        if (dot < 0) {
            section.put(path, value);
            return;
        }
        String head = path.substring(0, dot);
        String tail = path.substring(dot + 1);
        JSONObject nested = section.optJSONObject(head);
        if (nested == null) {
            nested = new JSONObject();
            section.put(head, nested);
        }
        putNestedValue(nested, tail, value);
    }

    private static Object encodeDirectValue(Object value) throws JSONException {
        if (value instanceof Set<?> typed) {
            List<String> values = new ArrayList<>();
            for (Object item : typed) {
                if (item instanceof String text) {
                    values.add(text);
                }
            }
            Collections.sort(values);
            return new JSONArray(values);
        }
        if (value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Boolean) {
            return value;
        }
        return null;
    }

    private static Object decodeDirectValue(Object value) throws JSONException {
        if (value instanceof JSONArray array) {
            return decodeStringSet(array);
        }
        return value;
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
