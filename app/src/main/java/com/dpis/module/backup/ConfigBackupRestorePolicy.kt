package com.dpis.module.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/** JVM-testable restore normalization and size rules for portable backups. */
object ConfigBackupRestorePolicy {
    const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
    private val TARGET_PACKAGE_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    fun hasUnknownKeys(entries: Map<String, *>): Boolean =
        entries.keys.any { !BackupKeyPolicy.isImportable(it) }

    fun normalizeIncoming(entries: MutableMap<String, Any?>) {
        normalizeLegacyResolutionKeys(entries)
        normalizeLegacyTargetPackages(entries)
    }

    fun configEntries(incoming: Map<String, Any?>): MutableMap<String, Any?> =
        incoming.filterKeys { !it.startsWith("template.") }.toMutableMap()

    @Throws(IOException::class)
    fun readLimited(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_BACKUP_BYTES) throw IOException("Backup exceeds size limit")
            output.write(buffer, 0, count)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    internal fun normalizeLegacyTargetPackages(entries: MutableMap<String, Any?>) {
        if (entries.keys.any { it.startsWith("package_config.") }) {
            entries.remove("target_packages")
            return
        }
        val values = entries["target_packages"] as? Set<*> ?: return
        val valid = values.filterIsInstance<String>()
            .filter { it.matches(TARGET_PACKAGE_PATTERN) }
            .toCollection(LinkedHashSet())
        if (valid.isEmpty()) entries.remove("target_packages") else entries["target_packages"] = valid
    }

    internal fun normalizeLegacyResolutionKeys(entries: MutableMap<String, Any?>) {
        val migrated = LinkedHashMap<String, Any?>()
        entries.forEach { (key, value) ->
            val marker = "package_config."
            val resolution = ".resolution."
            val start = key.indexOf(resolution, marker.length)
            if (key.startsWith(marker) && start > marker.length) {
                val packageName = key.substring(marker.length, start)
                val field = key.substring(start + resolution.length)
                if (packageName.isNotEmpty() && field.isNotEmpty()) {
                    migrated["resolution.$packageName.$field"] = value
                    return@forEach
                }
            }
            migrated[key] = value
        }
        entries.clear()
        entries.putAll(migrated)
    }
}
