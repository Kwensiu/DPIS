package com.dpis.module.updates

/** JVM-testable prompt decisions for the shared update-available dialog. */
object UpdatePromptPolicy {
    enum class PrimaryAction { DOWNLOAD, VIEW_RELEASE }

    fun primaryAction(apkUrl: String?): PrimaryAction =
        if (apkUrl.isNullOrBlank()) PrimaryAction.VIEW_RELEASE else PrimaryAction.DOWNLOAD

    fun releasePageUrl(releasePage: String?, fallback: String): String =
        releasePage.takeUnless { it.isNullOrEmpty() } ?: fallback

    fun cancelStopsDownload(downloadInProgress: Boolean): Boolean = downloadInProgress

    fun shouldCancelDownloadOnDismiss(
        changingConfigurations: Boolean,
        downloadInProgress: Boolean,
    ): Boolean = !changingConfigurations && downloadInProgress
}
