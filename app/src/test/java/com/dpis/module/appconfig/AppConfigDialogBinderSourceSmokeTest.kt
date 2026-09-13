package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigDialogBinderSourceSmokeTest {
    @Test
    fun composeEditorOwnsTypefaceAndHookChainDestinations() {
        val binder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val content = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val overlay = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        val typefacePage = read("src/main/java/com/dpis/module/appconfig/presentation/AppTypefacePickerPage.kt")
        val hookPage = read("src/main/java/com/dpis/module/fonts/presentation/HookChainEditorPage.kt")
        val templates = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")

        assertTrue(binder.contains("fun typefaceSelectorText("))
        assertFalse(binder.contains("fun bind(dialogView: View"))
        assertFalse(binder.contains("fun showTypefaceSelector("))
        assertFalse(binder.contains("R.layout.dialog_typeface_selection"))
        assertFalse(binder.contains("UnsavedBadgeBinder"))
        assertFalse(binder.contains("fun showFontHookDomains("))
        assertTrue(content.contains("state.actions.navigate(ConfigEditorDestination.TYPEFACE)"))
        assertTrue(content.contains("state.actions.navigate(ConfigEditorDestination.HOOK_CHAIN_INTERFACE)"))
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertTrue(typefacePage.contains("fun AppTypefacePickerPage("))
        assertTrue(hookPage.contains("fun HookChainEditorPage("))
        assertTrue(templates.contains("AppTypefacePickerPage("))
        assertTrue(templates.contains("HookChainEditorPage("))
    }

    @Test
    fun composeEditorKeepsProcessActionsAndTypefaceTextContract() {
        val binder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val actions = read("src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt")
        val gateway = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt",
        )

        assertTrue(binder.contains("enum class ProcessAction"))
        assertTrue(binder.contains("START"))
        assertTrue(binder.contains("RESTART"))
        assertTrue(binder.contains("STOP"))
        assertTrue(actions.contains("host.executeProcessAction(AppConfigDialogBinder.ProcessAction.START)"))
        assertTrue(gateway.contains("fun typefaceSelectorText(typefaceId: String?): String"))
        assertTrue(gateway.contains("AppConfigDialogBinder(activity, dialogHost).typefaceSelectorText(typefaceId)"))
    }

    @Test
    fun modeToggleThumbUsesHalfOfMeasuredTrackAfterRelayout() {
        val binder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        assertTrue(binder.contains("private fun updateModeToggleThumbLayout(toggle: ModeToggle?): Int"))
        assertTrue(binder.contains("private fun modeToggleTrack(toggle: ModeToggle): View"))
        assertTrue(binder.contains("val half = availableWidth / 2"))
    }

    @Test
    fun appConfigWizardHintUsesNamedDimensions() {
        val coordinator = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspacePresentationCoordinator.kt",
        )
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.shouldShowAdvancedHint(context)"))
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.markAdvancedHintDismissed(context)"))
    }

    @Test
    fun viewportModePolicyKeepsSeparateInputValues() {
        val modeSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogPolicy.kt")
        val stateSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogModels.kt")
        val switchStart = modeSource.indexOf("fun switchViewportTargetType(")
        val switchBlock = modeSource.substring(switchStart)

        assertTrue(stateSource.contains("fun viewportInputFor(viewportTargetType: String?)"))
        assertTrue(stateSource.contains("fun clearViewportInputs()"))
        assertTrue(switchBlock.contains("bindViewportModeToggle(toggle, nextType, animate)"))
        assertTrue(switchBlock.contains("state.updateViewportInput(resolveViewportMode(toggle), inputView.text)"))
        assertTrue(switchBlock.contains("inputView.text = state.viewportInputFor(nextType)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
