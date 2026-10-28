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
    private val migrateOwnerScopedPackageConfig: () -> Boolean,
    private val ownerUserId: Int,
    private val mirrorLegacyPreferences: () -> Boolean,
    private val commitAll: (SharedPreferences.Editor.() -> Unit) -> Boolean
) {
    private val packageConfigPersistence = PackageConfigPersistence()
    private val ownerScopedConfigPrefix = "$IDENTITY_PREFIX$ownerUserId."

    fun snapshotAll(): MutableMap<String, Any?> = LinkedHashMap<String, Any?>().also {
        copyEntries(it, preferences.all, backupOnly = false)
    }

    fun snapshotRuntimeDelivery(): MutableMap<String?, Any?> = LinkedHashMap<String?, Any?>(snapshotAll()).also { snapshot ->
        snapshot.entries.removeIf { entry ->
            val key = entry.key ?: return@removeIf true
            isLocalOnlyRuntimeDeliveryKey(key) ||
                    shouldExcludeLegacyNamespaceEntry(key, entry.value) ||
                    (key.startsWith(PACKAGE_CONFIG_PREFIX) &&
                            !isValidAggregatedPackageConfigEntry(key, entry.value)) ||
                    (isLegacyPackageConfigKey(key) &&
                            !shouldIncludeLegacyPackageConfigRuntimeEntry(
                                key,
                                entry.value,
                                preferences.all,
                            ))
        }
    }

    fun mergeRuntimeDeliverySnapshotOnce(entries: MutableMap<String?, Any?>?): Boolean {
        if (preferences.getBoolean(KEY_RUNTIME_REMOTE_SEED_COMPLETE, false)) return true
        if (entries == null) return false
        return commitAll {
            for ((key, rawValue) in entries) {
                if (key.isNullOrEmpty() || isLocalOnlyRuntimeDeliveryKey(key) ||
                    shouldExcludeLegacySeedEntry(key, rawValue, entries) ||
                    (key.startsWith(PACKAGE_CONFIG_PREFIX) &&
                            !isValidAggregatedPackageConfigEntry(key, rawValue)) ||
                    preferences.contains(key)
                ) {
                    continue
                }
                normalizeValue(rawValue)?.let { value -> putTypedValue(this, key, value) }
            }
            putBoolean(KEY_RUNTIME_REMOTE_SEED_COMPLETE, true)
        }
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
        val previousIdentityEntries = preferences.all
            .filterKeys { it.startsWith(IDENTITY_PREFIX) || it.startsWith(IDENTITY_RESIDUAL_PREFIX) }
        val previousLegacyEntries = preferences.all.filterKeys(isLegacyPackageConfigKey)
        if (!replaceBackupEntries(entries)) return BackupReplaceResult.failed(BackupReplaceStage.MANAGED_CONFIG)
        if (!migrateLegacyPackageConfig() || !migrateOwnerScopedPackageConfig()) {
            return if (rollback(previous, previousIdentityEntries, previousLegacyEntries)) {
                BackupReplaceResult.failed(BackupReplaceStage.LEGACY_MIGRATION)
            } else {
                BackupReplaceResult.failed(BackupReplaceStage.ROLLBACK)
            }
        }
        if (!mirrorLegacyPreferences()) {
            return if (rollback(previous, previousIdentityEntries, previousLegacyEntries)) {
                BackupReplaceResult.failed(BackupReplaceStage.LEGACY_MIRROR)
            } else {
                BackupReplaceResult.failed(BackupReplaceStage.ROLLBACK)
            }
        }
        return BackupReplaceResult.success()
    }

    private fun rollback(
        entries: Map<String, Any?>,
        identityEntries: Map<String, Any?>,
        legacyEntries: Map<String, Any?>,
    ): Boolean {
        val managedRestored = replaceBackupEntries(entries)
        val identityRestored = commitAll {
            preferences.all.keys
                .filter { it.startsWith(IDENTITY_PREFIX) || it.startsWith(IDENTITY_RESIDUAL_PREFIX) }
                .forEach(::remove)
            identityEntries.forEach { (key, value) -> putTypedValue(this, key, value) }
            preferences.all.keys.filter { isLegacyPackageConfigKey(it) }.forEach(::remove)
            legacyEntries.forEach { (key, value) -> putTypedValue(this, key, value) }
        }
        val mirrorRestored = mirrorLegacyPreferences()
        return managedRestored && identityRestored && mirrorRestored
    }

    private fun replaceBackupEntries(entries: Map<String, Any?>): Boolean {
        val preserved = LinkedHashMap<String, Any?>()
        for ((key, value) in preferences.all) {
            if (BackupKeyPolicy.isLocalOnly(key) && !key.startsWith(ownerScopedConfigPrefix)) {
                normalizeValue(value)?.let { preserved[key] = it }
            }
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
        BackupKeyPolicy.isImportable(key) && key != targetPackagesKey &&
                !isLegacyPackageConfigKey(key) &&
                (!key.startsWith(PACKAGE_CONFIG_PREFIX) ||
                        isValidAggregatedPackageConfigEntry(key, preferences.all[key]))

    private fun packageNameFromAggregatedKey(key: String): String? {
        if (!key.startsWith("package_config.")) return null
        val packageStart = "package_config.".length
        val domains = arrayOf(".viewport.", ".font.", ".target.", ".app.")
        val domainStart = domains.map { key.indexOf(it, packageStart) }.filter { it > packageStart }.minOrNull()
        return domainStart?.let { key.substring(packageStart, it) }?.takeUnless { it.isBlank() }
    }

    private fun isPackageLegacyNamespaceKey(key: String): Boolean =
        packageConfigPersistence.isLegacyPackageConfigNamespaceKey(key)

    private fun shouldExcludeLegacySeedEntry(
        key: String,
        value: Any?,
        values: Map<String?, Any?>,
    ): Boolean =
        if (!packageConfigPersistence.isLegacyPackageConfigNamespaceKey(key)) {
            false
        } else {
            !isLegacyPackageConfigKey(key) ||
                    !packageConfigPersistence.shouldIncludeLegacyPackageConfigRuntimeEntry(
                        key,
                        value,
                        values,
                    )
        }

    private fun shouldExcludeLegacyNamespaceEntry(key: String, value: Any?): Boolean {
        if (!packageConfigPersistence.isLegacyPackageConfigNamespaceKey(key)) return false
        return !isLegacyPackageConfigKey(key) ||
                !shouldIncludeLegacyPackageConfigRuntimeEntry(key, value, preferences.all)
    }

    private fun shouldIncludeLegacyPackageConfigRuntimeEntry(
        key: String,
        value: Any?,
        values: Map<String, *>,
    ): Boolean = packageConfigPersistence.shouldIncludeLegacyPackageConfigRuntimeEntry(
        key,
        value,
        values,
    )

    private fun isValidAggregatedPackageConfigEntry(key: String, value: Any?): Boolean =
        packageConfigPersistence.isValidAggregatedPackageConfigEntry(key, value)

    private fun isLocalOnlyRuntimeDeliveryKey(key: String): Boolean =
        key == KEY_INTERFACE_SCALE_PERCENT || key == KEY_STARTUP_DISCLAIMER_ACCEPTED ||
                key.startsWith("default_config.") || key.startsWith("template.") ||
                key.startsWith("runtime.") || key.startsWith("package_identity.") ||
                key.startsWith("package_identity_residual.")

    private companion object {
        const val PACKAGE_CONFIG_PREFIX = "package_config."
        const val IDENTITY_PREFIX = "package_identity."
        const val IDENTITY_RESIDUAL_PREFIX = "package_identity_residual."
        const val KEY_INTERFACE_SCALE_PERCENT = "ui.interface_scale_percent"
        const val KEY_STARTUP_DISCLAIMER_ACCEPTED = "ui.startup_disclaimer_accepted"
        const val KEY_RUNTIME_REMOTE_SEED_COMPLETE = "runtime.remote_config_seed_complete_v1"
    }
}
