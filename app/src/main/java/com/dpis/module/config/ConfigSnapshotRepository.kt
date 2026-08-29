package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.backup.BackupKeyPolicy
import com.dpis.module.backup.BackupReplaceResult
import com.dpis.module.backup.BackupReplaceStage

/**
 * Owns export and restore snapshots for the package configuration store.
 * Restore deliberately writes the primary preferences directly: mirroring is a
 * final transaction stage, after legacy package keys have been migrated.
 */
internal class ConfigSnapshotRepository(
    private val preferences: SharedPreferences,
    private val targetPackagesKey: String,
    private val normalizeValue: (Any?) -> Any?,
    private val putTypedValue: (SharedPreferences.Editor, String?, Any?) -> Unit,
    private val isLegacyPackageConfigKey: (String) -> Boolean,
    private val migrateLegacyPackageConfig: () -> Boolean,
    private val mirrorLegacyPreferences: () -> Boolean,
    private val commitAll: (SharedPreferences.Editor.() -> Unit) -> Boolean
) {
    fun snapshotAll(): MutableMap<String, Any?> = LinkedHashMap<String, Any?>().also {
        copyEntries(it, preferences.all, backupOnly = false)
    }

    fun snapshotRuntimeDelivery(): MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>(snapshotAll()).also { snapshot ->
        snapshot.entries.removeIf { entry -> isLocalOnlyRuntimeDeliveryKey(entry.key ?: return@removeIf true) }
    }

    fun snapshotBackup(): MutableMap<String, Any?> {
        val snapshot = LinkedHashMap<String, Any?>()
        copyEntries(snapshot, preferences.all, backupOnly = true)
        val packages = LinkedHashSet<String>()
        for (key in snapshot.keys) packageNameFromAggregatedKey(key)?.let(packages::add)
        if (packages.isNotEmpty()) snapshot[targetPackagesKey] = packages
        return snapshot
    }

    fun replaceAll(entries: MutableMap<String?, Any?>?): Boolean {
        if (entries == null) return false
        return commitAll {
            clear()
            for ((key, value) in entries) if (!key.isNullOrEmpty()) putTypedValue(this, key, value)
        }
    }

    fun replaceBackup(entries: MutableMap<String, Any?>?): Boolean = replaceBackupResult(entries).isSuccess()

    fun replaceBackupResult(entries: MutableMap<String, Any?>?): BackupReplaceResult {
        if (entries == null) return BackupReplaceResult.failed(BackupReplaceStage.MANAGED_CONFIG)
        val previous = snapshotBackup()
        if (!replaceBackupEntries(entries)) return BackupReplaceResult.failed(BackupReplaceStage.MANAGED_CONFIG)
        if (!migrateLegacyPackageConfig()) {
            replaceBackupEntries(previous)
            return BackupReplaceResult.failed(BackupReplaceStage.LEGACY_MIGRATION)
        }
        if (!mirrorLegacyPreferences()) {
            replaceBackupEntries(previous)
            mirrorLegacyPreferences()
            return BackupReplaceResult.failed(BackupReplaceStage.LEGACY_MIRROR)
        }
        return BackupReplaceResult.success()
    }

    private fun replaceBackupEntries(entries: Map<String, Any?>): Boolean {
        val preserved = LinkedHashMap<String, Any?>()
        for ((key, value) in preferences.all) {
            if (BackupKeyPolicy.isLocalOnly(key)) normalizeValue(value)?.let { preserved[key] = it }
        }
        val editor = preferences.edit().clear()
        for ((key, value) in preserved) putTypedValue(editor, key, value)
        for ((key, value) in entries) if (BackupKeyPolicy.isImportable(key)) putTypedValue(editor, key, value)
        return editor.commit()
    }

    private fun copyEntries(target: MutableMap<String, Any?>, source: Map<String, *>?, backupOnly: Boolean) {
        if (source == null) return
        for ((key, rawValue) in source) {
            if (key.isEmpty() || backupOnly && !isBackupConfigKey(key)) continue
            normalizeValue(rawValue)?.let { target[key] = it }
        }
    }

    private fun isBackupConfigKey(key: String): Boolean =
        BackupKeyPolicy.isImportable(key) && key != targetPackagesKey && !isLegacyPackageConfigKey(key)

    private fun packageNameFromAggregatedKey(key: String): String? {
        if (!key.startsWith("package_config.")) return null
        val packageStart = "package_config.".length
        val domains = arrayOf(".viewport.", ".font.", ".target.", ".app.")
        val domainStart = domains.map { key.indexOf(it, packageStart) }.filter { it > packageStart }.minOrNull()
        return domainStart?.let { key.substring(packageStart, it) }?.takeUnless { it.isBlank() }
    }

    private fun isLocalOnlyRuntimeDeliveryKey(key: String): Boolean =
        key == KEY_INTERFACE_SCALE_PERCENT || key == KEY_STARTUP_DISCLAIMER_ACCEPTED ||
            key.startsWith("default_config.") || key.startsWith("template.")

    private companion object {
        const val KEY_INTERFACE_SCALE_PERCENT = "ui.interface_scale_percent"
        const val KEY_STARTUP_DISCLAIMER_ACCEPTED = "ui.startup_disclaimer_accepted"
    }
}
