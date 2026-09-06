package com.dpis.module.backup

import android.content.ContentResolver
import android.net.Uri
import com.dpis.module.DpisConfigStore
import com.dpis.module.templates.QuickTemplateStore
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/** Coordinates portable backup I/O and cross-store restore semantics. */
class ConfigBackupCoordinator(
    private val resolver: ContentResolver,
    private val configStore: DpisConfigStore,
    private val templateStore: QuickTemplateStore
) {
    enum class Code { SUCCESS, INVALID_FILE, IO_ERROR, RESTORE_ERROR, ROLLBACK_ERROR }

    class Result private constructor(
        @JvmField val code: Code,
        @JvmField val cause: Throwable?
    ) {
        fun isSuccess() = code == Code.SUCCESS
        companion object {
            @JvmStatic fun success() = Result(Code.SUCCESS, null)
            @JvmStatic fun failure(code: Code, cause: Throwable? = null) = Result(code, cause)
        }
    }

    fun export(uri: Uri?): Result {
        if (uri == null) return Result.failure(Code.IO_ERROR)
        val entries = configStore.snapshotBackup().entries
            .mapNotNull { (key, value) -> key?.let { it to value } }
            .toMap(LinkedHashMap<String, Any?>())
            .also { templateStore.copyToBackup(it) }
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(ConfigBackupCodec.encode(entries).toByteArray(StandardCharsets.UTF_8))
            } ?: return Result.failure(Code.IO_ERROR)
            Result.success()
        } catch (error: Exception) {
            Result.failure(Code.IO_ERROR, error)
        }
    }

    fun restore(uri: Uri?): Result {
        if (uri == null) return Result.failure(Code.INVALID_FILE)
        val payload = try {
            resolver.openInputStream(uri)?.use(::readLimited)
                ?: return Result.failure(Code.IO_ERROR)
        } catch (error: IOException) {
            return Result.failure(Code.IO_ERROR, error)
        }
        val incoming = try {
            ConfigBackupCodec.decode(payload).entries
                .mapNotNull { (key, value) -> key?.let { it to value } }
                .toMap(LinkedHashMap<String, Any?>())
        } catch (error: Exception) {
            return Result.failure(Code.INVALID_FILE, error)
        }
        if (incoming.keys.any { !BackupKeyPolicy.isImportable(it) }) {
            return Result.failure(Code.INVALID_FILE, IllegalArgumentException("Unknown backup key"))
        }
        normalizeLegacyTargetPackages(incoming)
        val snapshot = configStore.snapshotBackup().entries
            .mapNotNull { (key, value) -> key?.let { it to value } }
            .toMap(LinkedHashMap<String, Any?>())
            .also { templateStore.copyToBackup(it) }
        val configEntries = incoming.filterKeys { !it.startsWith("template.") }.toMutableMap()
        if (!configStore.replaceBackup(configEntries)) return Result.failure(Code.RESTORE_ERROR)
        if (!QuickTemplateStore.containsTemplateEntries(incoming) || templateStore.restoreFromBackup(incoming)) {
            return Result.success()
        }
        val rolledConfig = configStore.replaceBackup(snapshot.filterKeys { !it.startsWith("template.") }.toMutableMap())
        val rolledTemplates = templateStore.restoreFromBackup(snapshot)
        return Result.failure(if (rolledConfig && rolledTemplates) Code.RESTORE_ERROR else Code.ROLLBACK_ERROR)
    }

    private fun readLimited(input: java.io.InputStream): String {
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

    private fun normalizeLegacyTargetPackages(entries: MutableMap<String, Any?>) {
        normalizeLegacyResolutionKeys(entries)
        if (entries.keys.any { it.startsWith("package_config.") }) {
            entries.remove("target_packages")
            return
        }
        val values = entries["target_packages"] as? Set<*> ?: return
        val valid = values.filterIsInstance<String>()
            .filter { it.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) }
            .toCollection(LinkedHashSet())
        if (valid.isEmpty()) entries.remove("target_packages") else entries["target_packages"] = valid
    }

    private fun normalizeLegacyResolutionKeys(entries: MutableMap<String, Any?>) {
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

    companion object {
        const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
    }
}
