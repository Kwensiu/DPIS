package com.dpis.module.backup

import com.dpis.module.BuildConfig
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object ConfigBackupCodec {
    private const val SCHEMA_VERSION = 3
    private val MAX_JSON_CHARS = 4 * 1024 * 1024
    private const val MAX_SECTION_ENTRIES = 10000

    private const val KEY_SCHEMA_VERSION = "schemaVersion"
    private const val KEY_CREATED_AT_EPOCH_MS = "createdAtEpochMs"
    private const val KEY_PACKAGE_NAME = "packageName"
    private const val KEY_APP_VERSION_CODE = "appVersionCode"
    private const val KEY_APP_VERSION_NAME = "appVersionName"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_PACKAGE_CONFIGS = "packageConfigs"
    private const val KEY_RESOLUTION_CONFIGS = "resolutionConfigs"
    private const val KEY_GLOBAL = "global"
    private const val KEY_DEFAULT_PREFILL = "defaultPrefill"
    private const val KEY_TEMPLATES = "templates"
    private const val KEY_TEMPLATE_META = "_meta"
    private const val KEY_TYPE = "type"
    private const val KEY_VALUE = "value"
    private val PACKAGE_CONFIG_FIELD_KEYS = arrayOf<String?>(
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
    )
    private val RESOLUTION_CONFIG_FIELD_KEYS = arrayOf<String?>(
        "width_px",
        "height_px",
        "mode"
    )

    @Throws(JSONException::class)
    @JvmStatic
    fun decodeDocument(rawJson: String): BackupDocument {
        val root = JSONObject(rawJson)
        val schema = root.optInt(KEY_SCHEMA_VERSION, -1)
        val entries = decode(rawJson)
        val metadata = BackupMetadata(
            schema, root.optLong(KEY_CREATED_AT_EPOCH_MS, 0L),
            root.optString(KEY_PACKAGE_NAME, ""),
            root.optLong(KEY_APP_VERSION_CODE, 0L),
            root.optString(KEY_APP_VERSION_NAME, "")
        )
        @Suppress("UNCHECKED_CAST")
        return BackupDocument(metadata, entries as Map<String, Any?>)
    }

    @Throws(JSONException::class)
    @JvmStatic
    fun encode(entries: Map<String, Any?>): String {
        val root = JSONObject()
        root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
        root.put(KEY_CREATED_AT_EPOCH_MS, System.currentTimeMillis())
        root.put(KEY_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
        root.put(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE)
        root.put(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)

        val encodedPackageConfigs = JSONObject()
        val encodedResolutionConfigs = JSONObject()
        val encodedGlobal = JSONObject()
        val encodedDefaultPrefill = JSONObject()
        val encodedTemplates = JSONObject()
        val keys = entries.keys.sorted()
        for (key in keys) {
            if (!BackupKeyPolicy.isImportable(key)) {
                continue
            }
            val value = entries.get(key)
            if (putPackageConfigEntry(encodedPackageConfigs, key, value)) {
                continue
            }
            if (putPackageOwnedConfigEntry(
                    encodedResolutionConfigs,
                    "resolution.",
                    RESOLUTION_CONFIG_FIELD_KEYS,
                    key,
                    value
                )
            ) {
                continue
            }
            if (putDefaultPrefillEntry(encodedDefaultPrefill, key, value)) {
                continue
            }
            if (putTemplateEntry(encodedTemplates, key, value)) {
                continue
            }
            val encoded = encodeDirectValue(value)
            putNestedValue(encodedGlobal, key, encoded)
        }
        if (encodedPackageConfigs.length() > 0) {
            root.put(KEY_PACKAGE_CONFIGS, encodedPackageConfigs)
        }
        if (encodedResolutionConfigs.length() > 0) {
            root.put(KEY_RESOLUTION_CONFIGS, encodedResolutionConfigs)
        }
        if (encodedGlobal.length() > 0) {
            root.put(KEY_GLOBAL, encodedGlobal)
        }
        if (encodedDefaultPrefill.length() > 0) {
            root.put(KEY_DEFAULT_PREFILL, encodedDefaultPrefill)
        }
        if (encodedTemplates.length() > 0) {
            root.put(KEY_TEMPLATES, encodedTemplates)
        }
        return root.toString(2)
    }

    @Throws(JSONException::class)
    @JvmStatic
    fun decode(rawJson: String): MutableMap<String?, Any?> {
        require(rawJson.length <= MAX_JSON_CHARS) { "Backup exceeds size limit" }
        val root = JSONObject(rawJson)
        val schemaVersion = root.optInt(KEY_SCHEMA_VERSION, -1)
        if (schemaVersion == SCHEMA_VERSION) {
            val packageName = root.optString(KEY_PACKAGE_NAME, "")
            require(BuildConfig.APPLICATION_ID == packageName) { "Backup belongs to another application" }
            require(
                !(!root.has(KEY_CREATED_AT_EPOCH_MS) || !root.has(KEY_APP_VERSION_CODE) || !root.has(
                    KEY_APP_VERSION_NAME
                ))
            ) { "Missing backup metadata" }
        }
        if (schemaVersion == 1) {
            return decodeSchemaV1(root)
        }
        if (schemaVersion == 2) {
            return decodeSchemaV2(root)
        }
        require(schemaVersion == SCHEMA_VERSION) { "Unsupported backup schema version: " + schemaVersion }
        val entries = LinkedHashMap<String?, Any?>()
        val encodedPackageConfigs = root.optJSONObject(KEY_PACKAGE_CONFIGS)
        if (encodedPackageConfigs != null) {
            decodePackageConfigsInto(entries, encodedPackageConfigs)
        }
        val encodedResolutionConfigs = root.optJSONObject(KEY_RESOLUTION_CONFIGS)
        if (encodedResolutionConfigs != null) {
            decodePackageOwnedConfigsInto(entries, "resolution.", encodedResolutionConfigs)
        }
        val encodedGlobal = root.optJSONObject(KEY_GLOBAL)
        if (encodedGlobal != null) {
            decodeDirectSectionInto(entries, "", encodedGlobal)
        }
        val encodedDefaultPrefill = root.optJSONObject(KEY_DEFAULT_PREFILL)
        if (encodedDefaultPrefill != null) {
            decodeDirectSectionInto(entries, "default_config.", encodedDefaultPrefill)
        }
        val encodedTemplates = root.optJSONObject(KEY_TEMPLATES)
        if (encodedTemplates != null) {
            decodeTemplatesInto(entries, encodedTemplates)
        }
        require(!(encodedPackageConfigs == null && encodedResolutionConfigs == null && encodedGlobal == null && encodedDefaultPrefill == null && encodedTemplates == null)) { "Missing entries section" }
        return entries
    }

    @Throws(JSONException::class)
    private fun decodeSchemaV1(root: JSONObject): MutableMap<String?, Any?> {
        val encodedEntries = root.optJSONObject(KEY_ENTRIES)
        requireNotNull(encodedEntries) { "Missing entries section" }
        val entries = LinkedHashMap<String?, Any?>()
        decodeEntrySectionInto(entries, encodedEntries)
        return entries
    }

    @Throws(JSONException::class)
    private fun decodeSchemaV2(root: JSONObject): MutableMap<String?, Any?> {
        val entries = LinkedHashMap<String?, Any?>()
        val encodedPackageConfigs = root.optJSONObject(KEY_PACKAGE_CONFIGS)
        if (encodedPackageConfigs != null) {
            decodePackageConfigsInto(entries, encodedPackageConfigs)
        }
        val encodedEntries = root.optJSONObject(KEY_ENTRIES)
        if (encodedEntries != null) {
            decodeEntrySectionInto(entries, encodedEntries)
        }
        require(!(encodedEntries == null && encodedPackageConfigs == null)) { "Missing entries section" }
        return entries
    }

    @Throws(JSONException::class)
    private fun decodeEntrySectionInto(
        entries: MutableMap<String?, Any?>,
        encodedEntries: JSONObject
    ) {
        val keys = encodedEntries.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == null || key.isEmpty()) {
                continue
            }
            val encodedValue = encodedEntries.optJSONObject(key)
            requireNotNull(encodedValue) { "Invalid entry payload for key: " + key }
            entries.put(key, decodeEntryValue(encodedValue))
        }
    }

    @Throws(JSONException::class)
    private fun putPackageConfigEntry(
        encodedPackageConfigs: JSONObject,
        key: String,
        value: Any?
    ): Boolean {
        val prefix = "package_config."
        if (!key.startsWith(prefix)) {
            return false
        }
        val remainder = key.substring(prefix.length)
        val fieldKey = packageConfigFieldKeyFromRemainder(remainder)
        if (fieldKey == null) {
            return false
        }
        val packageName = remainder.substring(0, remainder.length - fieldKey.length - 1)
        if (packageName.isEmpty() || fieldKey.isEmpty()) {
            return false
        }
        var packageEntries = encodedPackageConfigs.optJSONObject(packageName)
        if (packageEntries == null) {
            packageEntries = JSONObject()
            encodedPackageConfigs.put(packageName, packageEntries)
        }
        val domain = fieldKey.substring(0, fieldKey.indexOf('.'))
        val field = fieldKey.substring(domain.length + 1)
        var domainEntries = packageEntries.optJSONObject(domain)
        if (domainEntries == null) {
            domainEntries = JSONObject()
            packageEntries.put(domain, domainEntries)
        }
        val encoded = encodeDirectValue(value)
        putNestedValue(domainEntries, field, encoded)
        return true
    }

    @Throws(JSONException::class)
    private fun putPackageOwnedConfigEntry(
        encodedPackages: JSONObject,
        prefix: String,
        fieldKeys: Array<String?>,
        key: String,
        value: Any?
    ): Boolean {
        if (!key.startsWith(prefix)) {
            return false
        }
        val remainder = key.substring(prefix.length)
        val fieldKey = fieldKeyFromRemainder(remainder, fieldKeys)
        if (fieldKey == null) {
            return false
        }
        val packageName = remainder.substring(0, remainder.length - fieldKey.length - 1)
        if (packageName.isEmpty()) {
            return false
        }
        val encoded = encodeDirectValue(value)
        var packageEntries = encodedPackages.optJSONObject(packageName)
        if (packageEntries == null) {
            packageEntries = JSONObject()
            encodedPackages.put(packageName, packageEntries)
        }
        putNestedValue(packageEntries, fieldKey, encoded)
        return true
    }

    @Throws(JSONException::class)
    private fun putDefaultPrefillEntry(
        encodedDefaultPrefill: JSONObject,
        key: String,
        value: Any?
    ): Boolean {
        val prefix = "default_config."
        if (!key.startsWith(prefix)) {
            return false
        }
        val encoded = encodeDirectValue(value)
        putNestedValue(encodedDefaultPrefill, key.substring(prefix.length), encoded)
        return true
    }

    @Throws(JSONException::class)
    private fun putTemplateEntry(
        encodedTemplates: JSONObject,
        key: String,
        value: Any?
    ): Boolean {
        val prefix = "template."
        if (!key.startsWith(prefix)) {
            return false
        }
        val encoded = encodeDirectValue(value)
        val remainder = key.substring(prefix.length)
        if ("ids" == remainder || "order" == remainder) {
            var meta = encodedTemplates.optJSONObject(KEY_TEMPLATE_META)
            if (meta == null) {
                meta = JSONObject()
                encodedTemplates.put(KEY_TEMPLATE_META, meta)
            }
            meta.put(remainder, encoded)
            return true
        }
        val dot = remainder.indexOf('.')
        if (dot <= 0 || dot == remainder.length - 1) {
            return true
        }
        val templateId = remainder.substring(0, dot)
        val templateKey = remainder.substring(dot + 1)
        var template = encodedTemplates.optJSONObject(templateId)
        if (template == null) {
            template = JSONObject()
            encodedTemplates.put(templateId, template)
        }
        putNestedValue(template, templateKey, encoded)
        return true
    }

    private fun packageConfigFieldKeyFromRemainder(remainder: String?): String? {
        if (remainder == null || remainder.isEmpty()) {
            return null
        }
        for (fieldKey in PACKAGE_CONFIG_FIELD_KEYS) {
            if (remainder.endsWith("." + fieldKey)) {
                return fieldKey
            }
        }
        return null
    }

    private fun fieldKeyFromRemainder(remainder: String?, fieldKeys: Array<String?>): String? {
        if (remainder == null || remainder.isEmpty()) {
            return null
        }
        for (fieldKey in fieldKeys) {
            if (remainder.endsWith("." + fieldKey)) {
                return fieldKey
            }
        }
        return null
    }

    @Throws(JSONException::class)
    private fun decodePackageConfigsInto(
        entries: MutableMap<String?, Any?>,
        encodedPackageConfigs: JSONObject
    ) {
        val packageNames = encodedPackageConfigs.keys()
        while (packageNames.hasNext()) {
            val packageName = packageNames.next()
            if (packageName == null || packageName.isEmpty()) {
                continue
            }
            val packageEntries = encodedPackageConfigs.optJSONObject(packageName)
            requireNotNull(packageEntries) { "Invalid package config payload for package: " + packageName }
            val fieldKeys = packageEntries.keys()
            while (fieldKeys.hasNext()) {
                val domain = fieldKeys.next()
                if (domain == null || domain.isEmpty()) {
                    continue
                }
                val domainValue = packageEntries.get(domain)
                if (domainValue is JSONObject) {
                    decodeDirectSectionInto(
                        entries,
                        "package_config." + packageName + "." + domain + ".",
                        domainValue
                    )
                } else {
                    entries.put(
                        "package_config." + packageName + "." + domain,
                        decodeDirectValue(domainValue)
                    )
                }
            }
        }
    }

    @Throws(JSONException::class)
    private fun decodeTemplatesInto(
        entries: MutableMap<String?, Any?>,
        encodedTemplates: JSONObject
    ) {
        val templateIds = encodedTemplates.keys()
        while (templateIds.hasNext()) {
            val templateId = templateIds.next()
            if (templateId == null || templateId.isEmpty()) {
                continue
            }
            val template = encodedTemplates.optJSONObject(templateId)
            requireNotNull(template) { "Invalid template payload for template: " + templateId }
            if (KEY_TEMPLATE_META == templateId) {
                decodeDirectSectionInto(entries, "template.", template)
            } else {
                decodeDirectSectionInto(entries, "template." + templateId + ".", template)
            }
        }
    }

    @Throws(JSONException::class)
    private fun decodePackageOwnedConfigsInto(
        entries: MutableMap<String?, Any?>,
        prefix: String?,
        encodedPackages: JSONObject
    ) {
        val packageNames = encodedPackages.keys()
        while (packageNames.hasNext()) {
            val packageName = packageNames.next()
            if (packageName == null || packageName.isEmpty()) {
                continue
            }
            val packageEntries = encodedPackages.optJSONObject(packageName)
            requireNotNull(packageEntries) { "Invalid package-owned config payload for package: " + packageName }
            decodeDirectSectionInto(entries, prefix + packageName + ".", packageEntries)
        }
    }

    @Throws(JSONException::class)
    private fun decodeDirectSectionInto(
        entries: MutableMap<String?, Any?>,
        prefix: String?,
        section: JSONObject
    ) {
        decodeDirectSectionInto(entries, prefix, "", section)
    }

    @Throws(JSONException::class)
    private fun decodeDirectSectionInto(
        entries: MutableMap<String?, Any?>,
        prefix: String?,
        path: String,
        section: JSONObject
    ) {
        val keys = section.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == null || key.isEmpty()) {
                continue
            }
            val value = section.get(key)
            val nextPath = if (path.isEmpty()) key else path + "." + key
            if (value is JSONObject) {
                decodeDirectSectionInto(entries, prefix, nextPath, value)
            } else {
                entries.put(prefix + nextPath, decodeDirectValue(value))
            }
        }
    }

    @Throws(JSONException::class)
    private fun putNestedValue(section: JSONObject, path: String, value: Any?) {
        val dot = path.indexOf('.')
        if (dot < 0) {
            section.put(path, value)
            return
        }
        val head = path.substring(0, dot)
        val tail = path.substring(dot + 1)
        var nested = section.optJSONObject(head)
        if (nested == null) {
            nested = JSONObject()
            section.put(head, nested)
        }
        putNestedValue(nested, tail, value)
    }

    @Throws(JSONException::class)
    private fun encodeDirectValue(value: Any?): Any {
        if (value is Set<*>) {
            val values = mutableListOf<String>()
            for (item in value) {
                if (item is String) {
                    values.add(item)
                }
            }
            values.sort()
            return JSONArray(values)
        }
        if (value is String
            || value is Int
            || value is Long
            || value is Float
            || value is Boolean
        ) {
            return value
        }
        throw JSONException(
            "Unsupported backup value type: "
                    + (if (value == null) "null" else value.javaClass.name)
        )
    }

    @Throws(JSONException::class)
    private fun decodeDirectValue(value: Any?): Any? {
        if (value is JSONArray) {
            return decodeStringSet(value)
        }
        return value
    }

    @Throws(JSONException::class)
    private fun decodeEntryValue(encoded: JSONObject): Any {
        return when (val type = encoded.optString(KEY_TYPE, "")) {
            "string" -> encoded.optString(KEY_VALUE, "")
            "int" -> encoded.getInt(KEY_VALUE)
            "long" -> encoded.getLong(KEY_VALUE)
            "float" -> encoded.getDouble(KEY_VALUE).toFloat()
            "boolean" -> encoded.getBoolean(KEY_VALUE)
            "string_set" -> decodeStringSet(encoded.getJSONArray(KEY_VALUE))
            else -> throw IllegalArgumentException("Unsupported backup value type: " + type)
        }
    }

    @Throws(JSONException::class)
    private fun decodeStringSet(array: JSONArray): MutableSet<String?> {
        val values = LinkedHashSet<String?>()
        for (i in 0..<array.length()) {
            values.add(array.getString(i))
        }
        return values
    }
}
