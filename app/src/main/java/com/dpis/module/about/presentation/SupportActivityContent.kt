package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dpis.module.home.ModeGuideActivity
import com.dpis.module.LogActivity
import com.dpis.module.QuickConfigActivity
import com.dpis.module.about.OpenSourceLicenseActivity
import com.dpis.module.fonts.FontDetailActivity
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.InterfaceScaleStore
import java.util.function.Consumer

/** Type-safe Compose entry points for Java-owned standalone Activity contracts. */
object SupportActivityContent {
    @JvmStatic
    fun installThemeSettings(activity: ComponentActivity) {
        activity.setContent {
            var dynamicColorEnabled by remember {
                mutableStateOf(ThemeModeStore.isDynamicColorEnabled(activity))
            }
            var themeColor by remember { mutableStateOf(ThemeModeStore.getThemeColor(activity)) }
            var paletteStyle by remember { mutableStateOf(ThemeModeStore.getPaletteStyle(activity)) }
            var colorSpecification by remember {
                mutableStateOf(ThemeModeStore.getColorSpecification(activity))
            }
            DpisTheme(
                darkTheme = dpisDarkTheme(),
                dynamicColor = dynamicColorEnabled,
                themeColor = themeColor,
                paletteStyle = paletteStyle,
                colorSpecification = colorSpecification,
            ) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearThemeSettingsContent(
                        mode = ThemeModeStore.getMode(activity),
                        dynamicColorEnabled = dynamicColorEnabled,
                        themeColor = themeColor,
                        paletteStyle = paletteStyle,
                        colorSpecification = colorSpecification,
                        interfaceScalePercent = AppUiScaleManager.getScalePercent(activity),
                        onModeSelected = { mode ->
                            ThemeModeStore.setMode(activity, mode)
                            activity.recreate()
                        },
                        onDynamicColorChanged = { enabled ->
                            ThemeModeStore.setDynamicColorEnabled(activity, enabled)
                            dynamicColorEnabled = enabled
                            activity.recreate()
                        },
                        onThemeColorSelected = { color ->
                            ThemeModeStore.setThemeColor(activity, color)
                            themeColor = color
                            activity.recreate()
                        },
                        onPaletteStyleSelected = { style ->
                            ThemeModeStore.setPaletteStyle(activity, style)
                            paletteStyle = style
                            activity.recreate()
                        },
                        onColorSpecificationSelected = { specification ->
                            ThemeModeStore.setColorSpecification(activity, specification)
                            colorSpecification = specification
                            activity.recreate()
                        },
                        onInterfaceScaleChanged = { percent ->
                            val store = InterfaceScaleStore(activity)
                            val normalized = AppUiScaleManager.normalizeScalePercent(percent)
                            if (normalized != store.percent || !store.hasExplicitPercent()) {
                                if (store.setPercent(normalized)) activity.recreate()
                            }
                        }
                    )
                } else {
                    ThemeSettingsContent(
                        mode = ThemeModeStore.getMode(activity),
                        dynamicColorEnabled = dynamicColorEnabled,
                        themeColor = themeColor,
                        paletteStyle = paletteStyle,
                        colorSpecification = colorSpecification,
                        interfaceScalePercent = AppUiScaleManager.getScalePercent(activity),
                        onModeSelected = { mode ->
                            ThemeModeStore.setMode(activity, mode)
                            activity.recreate()
                        },
                        onDynamicColorChanged = { enabled ->
                            ThemeModeStore.setDynamicColorEnabled(activity, enabled)
                            dynamicColorEnabled = enabled
                            activity.recreate()
                        },
                        onThemeColorSelected = { color ->
                            ThemeModeStore.setThemeColor(activity, color)
                            themeColor = color
                            activity.recreate()
                        },
                        onPaletteStyleSelected = { style ->
                            ThemeModeStore.setPaletteStyle(activity, style)
                            paletteStyle = style
                            activity.recreate()
                        },
                        onColorSpecificationSelected = { specification ->
                            ThemeModeStore.setColorSpecification(activity, specification)
                            colorSpecification = specification
                            activity.recreate()
                        },
                        onInterfaceScaleChanged = { percent ->
                            val store = InterfaceScaleStore(activity)
                            val normalized = AppUiScaleManager.normalizeScalePercent(percent)
                            if (normalized != store.percent || !store.hasExplicitPercent()) {
                                if (store.setPercent(normalized)) activity.recreate()
                            }
                        },
                        onBack = activity::finish,
                    )
                }
            }
        }
    }

    @JvmStatic
    fun installQuickConfig(
        activity: QuickConfigActivity,
        presentation: QuickConfigPresentation
    ) {
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                QuickConfigContent(presentation = presentation, onDismiss = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installDonate(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                DonateSupportPage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installModeHelp(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
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
            DpisTheme(darkTheme = dpisDarkTheme()) {
                ModeGuidePage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installExperimentalSettings(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearExperimentalSettingsContent()
                } else {
                    ExperimentalSettingsContent(onBack = activity::finish)
                }
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
            DpisTheme(darkTheme = dpisDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearAboutContent(
                        versionText = versionText,
                        showDebugUpdateEntry = showDebugUpdateEntry,
                        onCheckUpdates = onCheckUpdates::run,
                        onShowDebugUpdate = onShowDebugUpdate::run,
                        onOpenSource = onOpenSource::run,
                        onOpenFeedback = onOpenFeedback::run,
                        onOpenLicenses = onOpenLicenses::run,
                    )
                } else {
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
    }

    @JvmStatic
    fun installOpenSourceLicenses(
        activity: ComponentActivity,
        items: List<OpenSourceLicenseActivity.LicenseItem>,
        onItemSelected: Consumer<OpenSourceLicenseActivity.LicenseItem>
    ) {
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearOpenSourceLicenseContent(
                        items = items,
                        onItemSelected = onItemSelected::accept,
                    )
                } else {
                    OpenSourceLicenseContent(
                        items = items,
                        onBack = activity::finish,
                        onItemSelected = onItemSelected::accept
                    )
                }
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
            DpisTheme(darkTheme = dpisDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearFontLibraryContent(
                        presentation = presentation,
                        onImportFont = onImportFont::run,
                        onExportArchive = onExportArchive::run,
                        onImportArchive = onImportArchive::run,
                        onFontSelected = onFontSelected::accept,
                    )
                } else {
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
            DpisTheme(darkTheme = dpisDarkTheme()) {
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
            DpisTheme(darkTheme = dpisDarkTheme()) {
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
