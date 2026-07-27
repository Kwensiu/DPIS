package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.dpis.module.home.ModeGuideActivity
import com.dpis.module.about.OpenSourceLicenseActivity
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
}
