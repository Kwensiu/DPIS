package com.dpis.module.about.presentation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.diagnostics.LogActivity
import com.dpis.module.quickconfig.QuickConfigActivity
import com.dpis.module.about.OpenSourceLicenseItem
import com.dpis.module.fonts.FontDetailActivity
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.home.ModeGuideActivity
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.InterfaceScaleStore
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.ui.WatchUiMode
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.settings.presentation.ExperimentalSettingsContent
import com.dpis.module.ui.compose.FontDetailContent
import com.dpis.module.ui.compose.FontDetailPresentation
import com.dpis.module.ui.compose.FontLibraryContent
import com.dpis.module.ui.compose.FontLibraryPresentation
import com.dpis.module.ui.compose.LogContent
import com.dpis.module.ui.compose.LogPresentation
import com.dpis.module.quickconfig.presentation.QuickConfigContent
import com.dpis.module.quickconfig.presentation.QuickConfigPresentation
import com.dpis.module.settings.presentation.ThemeSettingsContent
import com.dpis.module.ui.compose.WearAboutContent
import com.dpis.module.ui.compose.WearExperimentalSettingsContent
import com.dpis.module.ui.compose.WearFontLibraryContent
import com.dpis.module.ui.compose.WearOpenSourceLicenseContent
import com.dpis.module.ui.compose.WearThemeSettingsContent
import com.dpis.module.ui.compose.resolveDarkTheme
import java.util.function.Consumer

/** Type-safe Compose entry points for Java-owned standalone Activity contracts. */
object SupportActivityContent {
    @JvmStatic
    fun installThemeSettings(activity: ComponentActivity) {
        activity.setContent {
            var mode by remember { mutableStateOf(ThemeModeStore.getMode(activity)) }
            var dynamicColorEnabled by remember {
                mutableStateOf(ThemeModeStore.isDynamicColorEnabled(activity))
            }
            var themeColor by remember { mutableStateOf(ThemeModeStore.getThemeColor(activity)) }
            var paletteStyle by remember { mutableStateOf(ThemeModeStore.getPaletteStyle(activity)) }
            var colorSpecification by remember {
                mutableStateOf(ThemeModeStore.getColorSpecification(activity))
            }
            var showHomeEditButton by remember { mutableStateOf(PageSettingsStore.isHomeEditButtonVisible(activity)) }
            var defaultStartupPage by remember { mutableStateOf(PageSettingsStore.getDefaultStartupPage(activity)) }
            ComposeDesignSystem(
                // Appearance state is intentionally read at this root so every saved choice
                // recolors the same composition instead of recreating the Activity.
                darkTheme = ThemeModeStore.resolveDarkTheme(mode, isSystemInDarkTheme()),
                dynamicColor = dynamicColorEnabled,
                themeColor = themeColor,
                paletteStyle = paletteStyle,
                colorSpecification = colorSpecification,
            ) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearThemeSettingsContent(
                        mode = mode,
                        dynamicColorEnabled = dynamicColorEnabled,
                        themeColor = themeColor,
                        paletteStyle = paletteStyle,
                        colorSpecification = colorSpecification,
                        interfaceScalePercent = AppUiScaleManager.getScalePercent(activity),
                        onModeSelected = { selectedMode ->
                            ThemeModeStore.setMode(activity, selectedMode)
                            mode = selectedMode
                            activity.markAppearanceAppliedInPlace()
                        },
                        onDynamicColorChanged = { enabled ->
                            ThemeModeStore.setDynamicColorEnabled(activity, enabled)
                            dynamicColorEnabled = enabled
                            activity.markAppearanceAppliedInPlace()
                        },
                        onThemeColorSelected = { color ->
                            ThemeModeStore.setThemeColor(activity, color)
                            themeColor = color
                            activity.markAppearanceAppliedInPlace()
                        },
                        onPaletteStyleSelected = { style ->
                            ThemeModeStore.setPaletteStyle(activity, style)
                            paletteStyle = style
                            activity.markAppearanceAppliedInPlace()
                        },
                        onColorSpecificationSelected = { specification ->
                            ThemeModeStore.setColorSpecification(activity, specification)
                            colorSpecification = specification
                            activity.markAppearanceAppliedInPlace()
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
                        mode = mode,
                        dynamicColorEnabled = dynamicColorEnabled,
                        themeColor = themeColor,
                        paletteStyle = paletteStyle,
                        colorSpecification = colorSpecification,
                        interfaceScalePercent = AppUiScaleManager.getScalePercent(activity),
                        onModeSelected = { selectedMode ->
                            ThemeModeStore.setMode(activity, selectedMode)
                            mode = selectedMode
                            activity.markAppearanceAppliedInPlace()
                        },
                        onDynamicColorChanged = { enabled ->
                            ThemeModeStore.setDynamicColorEnabled(activity, enabled)
                            dynamicColorEnabled = enabled
                            activity.markAppearanceAppliedInPlace()
                        },
                        onThemeColorSelected = { color ->
                            ThemeModeStore.setThemeColor(activity, color)
                            themeColor = color
                            activity.markAppearanceAppliedInPlace()
                        },
                        onPaletteStyleSelected = { style ->
                            ThemeModeStore.setPaletteStyle(activity, style)
                            paletteStyle = style
                            activity.markAppearanceAppliedInPlace()
                        },
                        onColorSpecificationSelected = { specification ->
                            ThemeModeStore.setColorSpecification(activity, specification)
                            colorSpecification = specification
                            activity.markAppearanceAppliedInPlace()
                        },
                        onInterfaceScaleChanged = { percent ->
                            val store = InterfaceScaleStore(activity)
                            val normalized = AppUiScaleManager.normalizeScalePercent(percent)
                            if (normalized != store.percent || !store.hasExplicitPercent()) {
                                if (store.setPercent(normalized)) activity.recreate()
                            }
                        },
                        onShowHomeEditButtonChanged = { value ->
                            PageSettingsStore.setHomeEditButtonVisible(activity, value)
                            showHomeEditButton = value
                        },
                        onDefaultStartupPageSelected = { value ->
                            PageSettingsStore.setDefaultStartupPage(activity, value)
                            defaultStartupPage = value
                        },
                        showHomeEditButton = showHomeEditButton,
                        defaultStartupPage = defaultStartupPage,
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
            ComposeDesignSystem(
                darkTheme = resolveDarkTheme(),
                transparentWindowBackground = true,
            ) {
                QuickConfigContent(presentation = presentation, onDismiss = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installDonate(activity: ComponentActivity) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                DonateSupportPage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installModeHelp(activity: ComponentActivity) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
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
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                ModeGuidePage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installExperimentalSettings(activity: ComponentActivity) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
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
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
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
        items: List<OpenSourceLicenseItem>,
        onOpenUrl: (String) -> Unit,
    ) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearOpenSourceLicenseContent(
                        items = items,
                        onOpenUrl = onOpenUrl,
                    )
                } else {
                    OpenSourceLicenseContent(
                        items = items,
                        onBack = activity::finish,
                        onOpenUrl = onOpenUrl,
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
        onFontSelected: Consumer<String>,
        onNameSubmit: (String) -> Unit,
        onLargeConfirm: Runnable,
        onRepairConfirm: Runnable,
    ) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    WearFontLibraryContent(
                        presentation = presentation,
                        onImportFont = onImportFont::run,
                        onExportArchive = onExportArchive::run,
                        onImportArchive = onImportArchive::run,
                        onFontSelected = onFontSelected::accept,
                        onNameSubmit = onNameSubmit,
                        onLargeConfirm = onLargeConfirm::run,
                        onRepairConfirm = onRepairConfirm::run,
                    )
                } else {
                    FontLibraryContent(
                        presentation = presentation,
                        onBack = activity::finish,
                        onImportFont = onImportFont::run,
                        onExportArchive = onExportArchive::run,
                        onImportArchive = onImportArchive::run,
                        onFontSelected = onFontSelected::accept,
                        onNameSubmit = onNameSubmit,
                        onLargeConfirm = onLargeConfirm::run,
                        onRepairConfirm = onRepairConfirm::run,
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
        onRemoveReference: Consumer<String>,
        onRenameSubmit: (String) -> Boolean,
        onFallbackRetry: Runnable,
        onDeleteConfirm: Runnable,
        onRestoreConfirm: Runnable,
    ) {
        activity.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                FontDetailContent(
                    presentation = presentation,
                    onBack = activity::finish,
                    onRetryPublication = onRetryPublication::run,
                    onRename = onRename::run,
                    onDelete = onDelete::run,
                    onRemoveReference = onRemoveReference::accept,
                    onRenameSubmit = onRenameSubmit,
                    onFallbackRetry = onFallbackRetry::run,
                    onDeleteConfirm = onDeleteConfirm::run,
                    onRestoreConfirm = onRestoreConfirm::run,
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
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
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
                    onCopyEntry = onCopyEntry::accept,
                    onEnableLogs = activity::enableDiagnosticLogs,
                )
            }
        }
    }

    /**
     * Only LocalizedActivity needs lifecycle reconciliation. Other ComponentActivity callers
     * receive the same in-place Compose recoloring without an additional contract.
     */
    private fun ComponentActivity.markAppearanceAppliedInPlace() {
        (this as? LocalizedActivity)?.markAppearanceAppliedInPlace()
    }
}
