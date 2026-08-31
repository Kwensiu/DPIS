package com.dpis.module.updates

/** Immutable update information needed to restore a visible prompt after Activity recreation. */
data class UpdatePromptRequest(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String?,
    val releasePage: String?,
    val releaseNotes: String?,
) {
    companion object {
        @JvmStatic
        fun from(manifest: StartupUpdateManifest) = UpdatePromptRequest(
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            apkUrl = manifest.apkUrl,
            releasePage = manifest.releasePage,
            releaseNotes = manifest.releaseNotes,
        )
    }
}
