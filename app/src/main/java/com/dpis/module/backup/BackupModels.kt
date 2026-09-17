package com.dpis.module.backup

data class BackupDocument(
    @JvmField val metadata: BackupMetadata,
    @JvmField val entries: Map<String, Any?>,
    /** Optional Modern LSPosed application scope; never part of config entries. */
    @JvmField val moduleScope: List<String> = emptyList(),
)

data class BackupMetadata(
    @JvmField val schemaVersion: Int,
    @JvmField val createdAtEpochMs: Long,
    @JvmField val packageName: String,
    @JvmField val appVersionCode: Long,
    @JvmField val appVersionName: String
)

enum class BackupReplaceStage { MANAGED_CONFIG, LEGACY_MIGRATION, LEGACY_MIRROR }

class BackupReplaceResult private constructor(
    @JvmField val success: Boolean,
    @JvmField val failedStage: BackupReplaceStage?
) {
    fun isSuccess(): Boolean = success

    companion object {
        @JvmStatic fun success() = BackupReplaceResult(true, null)
        @JvmStatic fun failed(stage: BackupReplaceStage) = BackupReplaceResult(false, stage)
    }
}
