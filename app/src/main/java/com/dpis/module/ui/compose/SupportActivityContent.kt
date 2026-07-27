package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.dpis.module.home.ModeGuideActivity
import com.dpis.module.LogActivity
import com.dpis.module.about.OpenSourceLicenseActivity
import com.dpis.module.fonts.FontDetailActivity
import com.dpis.module.fonts.FontLibraryActivity
import java.util.function.Consumer

/** Type-safe Compose entry points for Java-owned standalone Activity contracts. */
object SupportActivityContent {
    @JvmStatic
    fun installDonate(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                DonateSupportPage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installModeHelp(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ModeHelpPage(
                    onBack = activity::finish,
                    onOpenModeGuide = {
                        activity.startActivity(Intent(activity, ModeGuideActivity::class.java))
                    }
                )
            }
        }
    }

    @JvmStatic
    fun installModeGuide(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ModeGuidePage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installExperimentalSettings(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ExperimentalSettingsContent(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installAbout(
        activity: ComponentActivity,
        versionText: String,
        showDebugUpdateEntry: Boolean,
        onCheckUpdates: Runnable,
        onShowDebugUpdate: Runnable,
        onOpenSource: Runnable,
        onOpenFeedback: Runnable,
        onOpenLicenses: Runnable
    ) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                AboutContent(
                    versionText = versionText,
                    showDebugUpdateEntry = showDebugUpdateEntry,
                    onBack = activity::finish,
                    onCheckUpdates = onCheckUpdates::run,
                    onShowDebugUpdate = onShowDebugUpdate::run,
                    onOpenSource = onOpenSource::run,
                    onOpenFeedback = onOpenFeedback::run,
                    onOpenLicenses = onOpenLicenses::run
                )
            }
        }
    }

    @JvmStatic
    fun installOpenSourceLicenses(
        activity: ComponentActivity,
        items: List<OpenSourceLicenseActivity.LicenseItem>,
        onItemSelected: Consumer<OpenSourceLicenseActivity.LicenseItem>
    ) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                OpenSourceLicenseContent(
                    items = items,
                    onBack = activity::finish,
                    onItemSelected = onItemSelected::accept
                )
            }
        }
    }

    @JvmStatic
    fun installFontLibrary(
        activity: FontLibraryActivity,
        presentation: FontLibraryPresentation,
        onImportFont: Runnable,
        onExportArchive: Runnable,
        onImportArchive: Runnable,
        onFontSelected: Consumer<String>
    ) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                FontLibraryContent(
                    presentation = presentation,
                    onBack = activity::finish,
                    onImportFont = onImportFont::run,
                    onExportArchive = onExportArchive::run,
                    onImportArchive = onImportArchive::run,
                    onFontSelected = onFontSelected::accept
                )
            }
        }
    }

    @JvmStatic
    fun installFontDetail(
        activity: FontDetailActivity,
        presentation: FontDetailPresentation,
        onRetryPublication: Runnable,
        onRename: Runnable,
        onDelete: Runnable,
        onRemoveReference: Consumer<String>
    ) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                FontDetailContent(
                    presentation = presentation,
                    onBack = activity::finish,
                    onRetryPublication = onRetryPublication::run,
                    onRename = onRename::run,
                    onDelete = onDelete::run,
                    onRemoveReference = onRemoveReference::accept
                )
            }
        }
    }

    @JvmStatic
    fun installLog(
        activity: LogActivity,
        presentation: LogPresentation,
        onSelectPage: Consumer<Int>,
        onToggleSort: Runnable,
        onToggleAutoRefresh: Runnable,
        onSaveLogs: Runnable,
        onShareLogs: Runnable,
        onRefresh: Runnable,
        onToggleExpanded: Consumer<String>,
        onCopyEntry: Consumer<String>
    ) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                LogContent(
                    presentation = presentation,
                    onBack = activity::finish,
                    onSelectPage = onSelectPage::accept,
                    onToggleSort = onToggleSort::run,
                    onToggleAutoRefresh = onToggleAutoRefresh::run,
                    onSaveLogs = onSaveLogs::run,
                    onShareLogs = onShareLogs::run,
                    onRefresh = onRefresh::run,
                    onToggleExpanded = onToggleExpanded::accept,
                    onCopyEntry = onCopyEntry::accept
                )
            }
        }
    }
}
