package com.dpis.module.quickconfig

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class QuickConfigSourceSmokeTest {
    @Test
    @Throws(IOException::class)
    fun manifestDeclaresQuickSettingsTileAndUsageStatsPermission() {
        val manifest: String = read("src/main/AndroidManifest.xml")

        Assert.assertTrue(manifest.contains("android.permission.PACKAGE_USAGE_STATS"))
        Assert.assertTrue(manifest.contains("android:name=\".quickconfig.QuickConfigActivity\""))
        Assert.assertTrue(manifest.contains("android:taskAffinity=\"\""))
        Assert.assertTrue(manifest.contains("@style/Theme.Dpis.QuickConfig"))
        Assert.assertTrue(manifest.contains("android:name=\".QuickConfigTileService\""))
        Assert.assertTrue(manifest.contains("android:icon=\"@drawable/ic_quick_config_24\""))
        Assert.assertTrue(manifest.contains("android.permission.BIND_QUICK_SETTINGS_TILE"))
        Assert.assertTrue(manifest.contains("android.service.quicksettings.action.QS_TILE"))
    }

    @Test
    @Throws(IOException::class)
    fun quickConfigUsesAppConfigSheetForForegroundPackage() {
        val activity: String = read("src/main/java/com/dpis/module/quickconfig/QuickConfigActivity.kt")
        val editor: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigComposeEditor.kt")
        val resolver: String =
            read("src/main/java/com/dpis/module/applist/ForegroundPackageResolver.java")
        val manifestTile: String = read("src/main/java/com/dpis/module/QuickConfigTileService.java")
        val tile: String =
            read("src/main/java/com/dpis/module/quickconfig/QuickConfigTileService.java")
        val styles: String = read("src/main/res/values/styles.xml")
        val content: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigContent.kt")

        Assert.assertTrue(activity.contains("EXTRA_PACKAGE_NAME"))
        Assert.assertTrue(activity.contains("installQuickConfig(presentation!!)"))
        Assert.assertTrue(editor.contains("EditorPresentationFactory.create("))
        Assert.assertTrue(content.contains("SheetVisualChrome()"))
        Assert.assertFalse(content.contains("extraTopPadding = 12.dp"))
        Assert.assertTrue(content.contains("AppHookChainEditorPage(state = state)"))
        Assert.assertFalse(content.contains("startFeedbackDiagnostic"))
        Assert.assertTrue(content.contains("contentAlignment = Alignment.BottomCenter"))
        Assert.assertTrue(content.contains("RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)"))
        Assert.assertTrue(activity.contains("InstalledAppCatalogCoordinator.createAppListItem("))
        Assert.assertTrue(styles.contains("Theme.Dpis.QuickConfig"))
        Assert.assertTrue(styles.contains("android:windowIsTranslucent"))
        Assert.assertTrue(resolver.contains("UsageStatsManager"))
        Assert.assertTrue(resolver.contains("AppOpsManager.OPSTR_GET_USAGE_STATS"))
        Assert.assertTrue(activity.contains("QuickConfigTargetDecision.decide("))
        Assert.assertTrue(activity.contains("Settings.ACTION_USAGE_ACCESS_SETTINGS"))
        Assert.assertTrue(activity.contains("Uri.parse(\"package:${'$'}packageName\")"))
        Assert.assertTrue(activity.contains("Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)"))
        Assert.assertTrue(resolver.contains("UsageEvents.Event.MOVE_TO_FOREGROUND"))
        Assert.assertTrue(resolver.contains("SYSTEM_UI_PACKAGE"))
        Assert.assertTrue(manifestTile.contains("extends com.dpis.module.quickconfig.QuickConfigTileService"))
        Assert.assertTrue(tile.contains("QuickConfigActivity.createIntent("))
        Assert.assertTrue(tile.contains("startActivityAndCollapse"))
    }

    @Test
    @Throws(IOException::class)
    fun quickConfigRoutesSheetActionsToExistingRuntimeSemantics() {
        val activity: String = read("src/main/java/com/dpis/module/quickconfig/QuickConfigActivity.kt")
        val editor: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigComposeEditor.kt")

        Assert.assertTrue(editor.contains("saveHandler.saveResolved("))
        Assert.assertTrue(editor.contains("WechatDpiEditor.save("))
        Assert.assertTrue(editor.contains("currentDraft.wechatDpiInput"))
        Assert.assertTrue(editor.contains("currentDraft.dpisEnabled"))
        Assert.assertTrue(editor.contains("activity.hookConfigStore"))
        Assert.assertTrue(editor.contains("scopeCoordinator.requestScope("))
        Assert.assertTrue(editor.contains("requestScopeAfterSave(currentItem)"))
        Assert.assertTrue(editor.contains("editorSession = current.withScopeSelected(true)"))
        Assert.assertTrue(activity.contains("executeHyperOsNativeProxyMount("))
        Assert.assertTrue(activity.contains("executeDialogProcessAction(item, action)"))
        Assert.assertTrue(editor.contains("FontRuntimePropertySyncer.clearTargetAsync(targetPackageName)"))
        Assert.assertFalse(activity.contains("quick_config_open_main_for_advanced"))
        Assert.assertFalse(activity.contains("showMainAppToast"))
        Assert.assertFalse(activity.contains("publishAfterSave(item.packageName)\n                finish()"))
    }

    @Test
    @Throws(IOException::class)
    fun quickConfigKeepsFeedbackDiagnosticSemanticsAvailable() {
        val activity: String = read("src/main/java/com/dpis/module/quickconfig/QuickConfigActivity.kt")
        val editor: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigComposeEditor.kt")
        val diagnostics: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigDiagnosticSession.kt")

        Assert.assertTrue(diagnostics.contains("Coordinator(createHost())"))
        Assert.assertTrue(editor.contains("onStartDiagnostic(currentItem, AppConfigDialogState.from(currentItem, draft))"))
        Assert.assertTrue(diagnostics.contains("Coordinator.Request.fromPersisted("))
        Assert.assertTrue(diagnostics.contains("QuickConfigDialog.FeedbackStart("))
        Assert.assertTrue(diagnostics.contains("QuickConfigDialog.EnableLogs"))
        Assert.assertTrue(editor.contains("QuickConfigDialog.Message("))
        Assert.assertFalse(activity.contains("MaterialAlertDialogBuilder"))
        Assert.assertTrue(diagnostics.contains("showPackagingDialog()"))
        Assert.assertTrue(diagnostics.contains("showResultSheet(built)"))
        Assert.assertTrue(editor.contains("saveCurrentForDiagnostic"))
        Assert.assertFalse(activity.contains("dialog_feedback_diagnostic_button);\n        if (feedbackDiagnosticButton != null)"))
    }

    @Test
    @Throws(IOException::class)
    fun quickConfigTileUsesDedicatedDisplaySettingsIcon() {
        val tileIcon: String = read("src/main/res/drawable/ic_quick_config_24.xml")

        Assert.assertTrue(tileIcon.contains("android:viewportWidth=\"960\""))
        Assert.assertTrue(tileIcon.contains("M300,550v20"))
    }

    @Test
    @Throws(IOException::class)
    fun quickConfigKeepsItsTranslucentActivityBackdropTransparent() {
        val content: String =
            read("src/main/java/com/dpis/module/quickconfig/presentation/QuickConfigContent.kt")
        val theme: String = read("src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt")

        Assert.assertTrue(content.contains("transparentWindowBackground = true"))
        Assert.assertTrue(theme.contains("transparentWindowBackground: Boolean = false"))
        Assert.assertTrue(theme.contains("Color.Transparent.toArgb()"))
    }

    companion object {
        @Throws(IOException::class)
        private fun read(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath)
        }
    }
}
