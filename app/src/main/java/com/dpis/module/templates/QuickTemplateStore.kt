package com.dpis.module.templates

import android.content.Context
import android.content.SharedPreferences
import com.dpis.module.DpisConfigStore
import com.dpis.module.backup.TemplateBackup
import java.util.Collections
import java.util.Objects
import java.util.UUID

class QuickTemplateStore {
    private val preferences: SharedPreferences

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    /** Owns template data outside the runtime configuration preference group.  */
    constructor(context: Context) {
        this.preferences = context.getSharedPreferences(GROUP, Context.MODE_PRIVATE)
        migrateLegacyTemplates(
            context.getSharedPreferences(
                DpisConfigStore.GROUP,
                Context.MODE_PRIVATE
            )
        )
    }

    private fun migrateLegacyTemplates(legacy: SharedPreferences?) {
        if (!preferences.all.isEmpty() || legacy == null) return
        val editor = preferences.edit()
        var found = false
        for (entry in legacy.all.entries) {
            if (entry.key.startsWith("template.")) {
                putTypedValue(editor, entry.key, entry.value)
                found = true
            }
        }
        if (found && editor.commit()) {
            val cleanup = legacy.edit()
            for (key in legacy.all.keys) {
                if (key.startsWith("template.")) cleanup.remove(key)
            }
            cleanup.commit()
        }
    }

    fun copyToBackup(entries: MutableMap<String, Any?>?) {
        if (entries == null) return
        for (entry in preferences.all.entries) {
            if (entry.key.startsWith("template.")) {
                val value: Any? = entry.value
                if (value is MutableSet<*>) entries.put(
                    entry.key,
                    java.util.LinkedHashSet<String?>(value as MutableSet<String?>)
                )
                else entries.put(entry.key, value)
            }
        }
    }

    fun restoreFromBackup(entries: MutableMap<String, Any?>?): Boolean {
        if (!containsTemplateEntries(entries)) return false
        val editor = preferences.edit()
        for (key in preferences.all.keys) if (key.startsWith("template.")) editor.remove(key)
        for (entry in entries!!.entries) {
            if (entry.key!!.startsWith("template.")) putTypedValue(editor, entry.key, entry.value)
        }
        return editor.commit()
    }

    /** Exports templates as domain values; preference key layout stays private to this store.  */
    fun exportBackup(): MutableList<TemplateBackup?> {
        val result: MutableList<TemplateBackup?> = ArrayList<TemplateBackup?>()
        for (template in readAll()) {
            result.add(
                TemplateBackup(
                    template.id, template.name!!, template.updatedAt,
                    template.selectedPackages.filterNotNull().toSet(), template.configValue
                )
            )
        }
        return result
    }

    /** Restores the complete ordered template catalog from domain values.  */
    fun restoreBackup(backups: MutableList<TemplateBackup?>?): Boolean {
        if (backups == null) return false
        val editor = preferences.edit()
        for (key in preferences.all.keys) {
            if (key.startsWith("template.")) editor.remove(key)
        }
        if (!editor.commit()) return false
        for (backup in backups) {
            if (backup == null || !save(
                    QuickTemplate(
                        backup.id,
                        backup.name,
                        backup.updatedAt,
                        backup.selectedPackages.toMutableSet() as MutableSet<String?>,
                        backup.configValue
                    )
                )
            ) {
                return false
            }
        }
        val orderedIds = ArrayList<String>()
        for (backup in backups) {
            if (backup != null) orderedIds.add(backup.id)
        }
        return reorder(orderedIds)
    }

    fun newTemplateId(): String {
        return UUID.randomUUID().toString()
    }

    fun readAll(): MutableList<QuickTemplate> {
        val templates = ArrayList<QuickTemplate>()
        for (id in this.templateIds) {
            val template = read(id)
            if (template != null) {
                templates.add(template)
            }
        }
        val orderedIds = this.templateOrderIds
        if (orderedIds.isEmpty()) {
            templates.sortWith(defaultComparator())
            return templates
        }
        templates.sortWith(Comparator { left, right ->
            val leftIndex = orderedIds.indexOf(left.id)
            val rightIndex = orderedIds.indexOf(right.id)
            val leftOrdered = leftIndex >= 0
            val rightOrdered = rightIndex >= 0
            if (leftOrdered && rightOrdered) {
                return@Comparator leftIndex.compareTo(rightIndex)
            }
            if (leftOrdered) {
                return@Comparator -1
            }
            if (rightOrdered) {
                return@Comparator 1
            }
            defaultComparator().compare(left, right)
        })
        return templates
    }

    fun read(id: String?): QuickTemplate? {
        val normalizedId: String? = normalizeId(id)
        if (normalizedId == null) {
            return null
        }
        val prefix: String = prefixFor(normalizedId)
        val name = getString(prefix + "name", null)
        if (name == null || name.trim { it <= ' ' }.isEmpty()) {
            return null
        }
        val updatedAt = getLong(prefix + "updated_at", 0L)
        return QuickTemplate(
            normalizedId,
            name.trim { it <= ' ' },
            updatedAt,
            getSelectedPackages(normalizedId) as MutableSet<String?>,
            TemplateCustomSemantics.customValue(
                TemplateConfigPreferences.read(preferences, prefix + "config.")
            )
        )
    }

    fun hasDuplicateName(name: String?, excludedId: String?): Boolean {
        val normalizedName: String? = normalizeName(name)
        if (normalizedName == null) {
            return false
        }
        val normalizedExcludedId: String? = normalizeId(excludedId)
        for (template in readAll()) {
            if (normalizedExcludedId != null && normalizedExcludedId == template.id) {
                continue
            }
            val candidateName: String? = normalizeName(template.name)
            if (normalizedName == candidateName) {
                return true
            }
        }
        return false
    }

    fun save(template: QuickTemplate?): Boolean {
        val normalizedId: String? = if (template != null) normalizeId(template.id) else null
        if (template == null || normalizedId == null || template.name == null || template.name.trim { it <= ' ' }
                .isEmpty()) {
            return false
        }
        val ids = this.templateIds
        val existingTemplate = ids.contains(normalizedId)
        ids.add(normalizedId)
        val prefix: String = prefixFor(normalizedId)
        val editor = preferences.edit()
            .putStringSet(KEY_TEMPLATE_IDS, ids)
            .putString(prefix + "name", template.name.trim { it <= ' ' })
            .putLong(prefix + "updated_at", template.updatedAt)
            .putStringSet(
                prefix + "selected_packages",
                sanitizeStringSet(template.selectedPackages)
            )
        editor.putString(KEY_TEMPLATE_ORDER, orderAfterSave(existingTemplate, normalizedId))
        TemplateConfigPreferences.write(
            editor, prefix + "config.",
            TemplateCustomSemantics.customValue(template.configValue)
        )
        return editor.commit()
    }

    fun reorder(orderedIds: List<String>?): Boolean {
        val currentIds = this.templateIds
        if (currentIds.isEmpty()) {
            return preferences.edit()
                .remove(KEY_TEMPLATE_ORDER)
                .commit()
        }
        val sanitized = java.util.LinkedHashSet<String>()
        if (orderedIds != null) {
            for (id in orderedIds) {
                val normalizedId: String? = normalizeId(id)
                if (normalizedId != null && currentIds.contains(normalizedId) && read(normalizedId) != null) {
                    sanitized.add(normalizedId)
                }
            }
        }
        for (id in currentIds) {
            if (!sanitized.contains(id) && read(id) != null) {
                sanitized.add(id)
            }
        }
        return preferences.edit()
            .putStringSet(KEY_TEMPLATE_IDS, sanitized)
            .putString(KEY_TEMPLATE_ORDER, java.lang.String.join("\n", sanitized))
            .commit()
    }

    fun setSelectedPackages(id: String?, packageNames: MutableSet<String?>?): Boolean {
        val normalizedId: String? = normalizeId(id)
        if (normalizedId == null || !this.templateIds.contains(normalizedId)) {
            return false
        }
        return preferences.edit()
            .putStringSet(
                prefixFor(normalizedId) + "selected_packages",
                sanitizeStringSet(packageNames)
            )
            .commit()
    }

    fun delete(id: String?): Boolean {
        val normalizedId: String? = normalizeId(id)
        if (normalizedId == null) {
            return false
        }
        val ids = this.templateIds
        ids.remove(normalizedId)
        val order = orderAfterDelete(normalizedId, ids)
        val prefix: String = prefixFor(normalizedId)
        val editor = preferences.edit()
            .putStringSet(KEY_TEMPLATE_IDS, ids)
            .putString(KEY_TEMPLATE_ORDER, order)
            .remove(prefix + "name")
            .remove(prefix + "updated_at")
            .remove(prefix + "selected_packages")
        TemplateConfigPreferences.clear(editor, prefix + "config.")
        return editor.commit()
    }

    private val templateIds: LinkedHashSet<String>
        get() {
            val ids =
                java.util.LinkedHashSet<String>()
            val storedIds =
                getStringSet(KEY_TEMPLATE_IDS)
            if (storedIds == null) {
                return ids
            }
            for (id in storedIds) {
                val normalized: String? = normalizeId(id)
                if (normalized != null) {
                    ids.add(normalized)
                }
            }
            return ids
        }

    private val templateOrderIds: MutableList<String>
        get() {
            val rawOrder =
                getString(KEY_TEMPLATE_ORDER, null)
            if (rawOrder == null || rawOrder.isBlank()) {
                return mutableListOf()
            }
            val ids = ArrayList<String>()
            for (part in rawOrder.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                val normalized: String? = normalizeId(part)
                if (normalized != null && !ids.contains(normalized)) {
                    ids.add(normalized)
                }
            }
            return ids
        }

    private fun getSelectedPackages(id: String): java.util.LinkedHashSet<String> {
        return sanitizeStringSet(getStringSet(prefixFor(id) + "selected_packages"))
    }

    private fun getStringSet(key: String?): MutableSet<String?> {
        try {
            return preferences.getStringSet(key, mutableSetOf<String?>())!!
        } catch (ignored: ClassCastException) {
            return mutableSetOf<String?>()
        }
    }

    private fun getString(key: String?, defaultValue: String?): String? {
        try {
            return preferences.getString(key, defaultValue)
        } catch (ignored: ClassCastException) {
            return defaultValue
        }
    }

    private fun getLong(key: String?, defaultValue: Long): Long {
        try {
            return preferences.getLong(key, defaultValue)
        } catch (ignored: ClassCastException) {
            return defaultValue
        }
    }

    private fun orderAfterDelete(
        deletedId: String?,
        remainingIds: java.util.LinkedHashSet<String>
    ): String {
        val ordered = java.util.LinkedHashSet<String>()
        for (id in this.templateOrderIds) {
            if (id != deletedId && remainingIds.contains(id)) {
                ordered.add(id)
            }
        }
        for (id in remainingIds) {
            if (id != deletedId) {
                ordered.add(id)
            }
        }
        return java.lang.String.join("\n", ordered)
    }

    private fun orderAfterSave(existingTemplate: Boolean, savedId: String?): String {
        val existingOrder = getString(KEY_TEMPLATE_ORDER, null)
        if (existingOrder != null && !existingOrder.isBlank()) {
            return appendToOrder(existingOrder, savedId)
        }
        val existingTemplates = ArrayList<QuickTemplate>()
        for (id in this.templateIds) {
            val template = read(id)
            if (template != null) {
                existingTemplates.add(template)
            }
        }
        existingTemplates.sortWith(defaultComparator())
        val orderedIds = ArrayList<String>(existingTemplates.size + 1)
        for (template in existingTemplates) {
            if (!orderedIds.contains(template.id)) {
                orderedIds.add(template.id)
            }
        }
        if (!existingTemplate) {
            orderedIds.remove(savedId)
            orderedIds.add(savedId!!)
        } else if (!orderedIds.contains(savedId)) {
            orderedIds.add(savedId!!)
        }
        return java.lang.String.join("\n", orderedIds)
    }

    class QuickTemplate(
        @JvmField val id: String,
        @JvmField val name: String?,
        @JvmField val updatedAt: Long,
        selectedPackages: MutableSet<String?>?,
        configValue: TemplateConfigValue?
    ) : QuickTemplateApplyCoordinator.Template<TemplateConfigValue?> {
        @JvmField
        val selectedPackages: MutableSet<String?>
        @JvmField
        val configValue: TemplateConfigValue

        init {
            this.selectedPackages = Collections.unmodifiableSet<String?>(
                sanitizeStringSet(selectedPackages) as MutableSet<String?>
            )
            this.configValue = if (configValue != null) configValue else TemplateConfigValue.EMPTY
        }

        override fun selectedPackages(): MutableSet<String?> {
            return selectedPackages
        }

        override fun configValue(): TemplateConfigValue {
            return configValue
        }

        override fun equals(`object`: Any?): Boolean {
            if (this === `object`) {
                return true
            }
            if (`object` !is QuickTemplate) {
                return false
            }
            return updatedAt == `object`.updatedAt && id == `object`.id
                    && name == `object`.name
                    && selectedPackages == `object`.selectedPackages
                    && configValue == `object`.configValue
        }

        override fun hashCode(): Int {
            return Objects.hash(id, name, updatedAt, selectedPackages, configValue)
        }
    }

    companion object {
        const val KEY_TEMPLATE_IDS: String = "template.ids"
        const val KEY_TEMPLATE_ORDER: String = "template.order"
        const val GROUP: String = "quick_templates"

        /**
         * A backup from before template storage was introduced has no template
         * entries. Such a backup must not delete the user's current local catalog.
         */
        fun containsTemplateEntries(entries: MutableMap<String, *>?): Boolean {
            if (entries == null) return false
            for (key in entries.keys) {
                if (key != null && key.startsWith("template.")) return true
            }
            return false
        }

        private fun putTypedValue(editor: SharedPreferences.Editor, key: String?, value: Any?) {
            if (value is String) editor.putString(key, value)
            else if (value is Boolean) editor.putBoolean(key, value)
            else if (value is Int) editor.putInt(key, value)
            else if (value is Long) editor.putLong(key, value)
            else if (value is Float) editor.putFloat(key, value)
            else if (value is MutableSet<*>) editor.putStringSet(
                key,
                java.util.LinkedHashSet<String>(value as MutableSet<String?>)
            )
        }

        private fun prefixFor(id: String): String {
            return "template." + id + "."
        }

        private fun normalizeId(id: String?): String? {
            if (id == null) {
                return null
            }
            val trimmed = id.trim { it <= ' ' }
            return if (trimmed.isEmpty() || trimmed.contains(".")) null else trimmed
        }

        private fun normalizeName(name: String?): String? {
            if (name == null) {
                return null
            }
            val trimmed = name.trim { it <= ' ' }
            return if (trimmed.isEmpty()) null else trimmed.lowercase()
        }

        private fun sanitizeStringSet(values: MutableSet<String?>?): java.util.LinkedHashSet<String> {
            val sanitized = java.util.LinkedHashSet<String>()
            if (values == null) {
                return sanitized
            }
            for (value in values) {
                if (value == null) {
                    continue
                }
                val trimmed = value.trim { it <= ' ' }
                if (!trimmed.isEmpty()) {
                    sanitized.add(trimmed)
                }
            }
            return sanitized
        }

        private fun appendToOrder(existingOrder: String?, id: String?): String {
            val ordered = java.util.LinkedHashSet<String>()
            if (existingOrder != null && !existingOrder.isBlank()) {
                for (part in existingOrder.split("\\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    val normalized: String? = normalizeId(part)
                    if (normalized != null) {
                        ordered.add(normalized)
                    }
                }
            }
            ordered.add(id!!)
            return java.lang.String.join("\n", ordered)
        }

        private fun defaultComparator(): Comparator<QuickTemplate> {
            return compareByDescending<QuickTemplate> { it.updatedAt }
                .thenBy { it.name.orEmpty() }
                .thenBy { it.id }
        }
    }
}
