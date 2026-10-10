package com.dpis.module.home

import android.content.Context
import com.dpis.module.R
import com.dpis.module.updates.StartupUpdateManifest

/** Presentation-only result of the latest update check. */
class HomeUpdateUiState private constructor(
    @JvmField val status: Status,
    versionName: String?,
) {
    @JvmField
    val versionName: String = versionName?.trim().orEmpty()

    enum class Status {
        CHECKING,
        UP_TO_DATE,
        FAILED,
        AVAILABLE,
    }

    fun subtitle(context: Context?): String {
        context ?: return ""
        return when (status) {
            Status.CHECKING -> context.getString(R.string.home_update_checking)
            Status.UP_TO_DATE -> context.getString(R.string.home_update_up_to_date)
            Status.FAILED -> context.getString(R.string.home_update_check_failed_retry)
            Status.AVAILABLE -> context.getString(
                R.string.home_update_available,
                versionName.ifEmpty { context.getString(R.string.home_update_version_unknown) },
            )
        }
    }

    companion object {
        @JvmField
        val CHECKING = HomeUpdateUiState(Status.CHECKING, null)

        @JvmField
        val UP_TO_DATE = HomeUpdateUiState(Status.UP_TO_DATE, null)

        @JvmField
        val FAILED = HomeUpdateUiState(Status.FAILED, null)

        @JvmStatic
        fun available(manifest: StartupUpdateManifest?): HomeUpdateUiState =
            manifest?.let { HomeUpdateUiState(Status.AVAILABLE, it.versionName) } ?: UP_TO_DATE
    }
}
