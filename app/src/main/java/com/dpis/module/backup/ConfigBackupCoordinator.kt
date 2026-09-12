package com.dpis.module.backup

import android.content.ContentResolver
import android.net.Uri
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.templates.QuickTemplateStore
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
            resolver.openInputStream(uri)?.use(ConfigBackupRestorePolicy::readLimited)
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
        if (ConfigBackupRestorePolicy.hasUnknownKeys(incoming)) {
            return Result.failure(Code.INVALID_FILE, IllegalArgumentException("Unknown backup key"))
        }
        ConfigBackupRestorePolicy.normalizeIncoming(incoming)
        val snapshot = configStore.snapshotBackup().entries
            .mapNotNull { (key, value) -> key?.let { it to value } }
            .toMap(LinkedHashMap<String, Any?>())
            .also { templateStore.copyToBackup(it) }
        val configEntries = ConfigBackupRestorePolicy.configEntries(incoming)
        if (!configStore.replaceBackup(configEntries)) return Result.failure(Code.RESTORE_ERROR)
        if (!QuickTemplateStore.containsTemplateEntries(incoming) || templateStore.restoreFromBackup(incoming)) {
            return Result.success()
        }
        val rolledConfig = configStore.replaceBackup(ConfigBackupRestorePolicy.configEntries(snapshot))
        val rolledTemplates = templateStore.restoreFromBackup(snapshot)
        return Result.failure(if (rolledConfig && rolledTemplates) Code.RESTORE_ERROR else Code.ROLLBACK_ERROR)
    }
}
