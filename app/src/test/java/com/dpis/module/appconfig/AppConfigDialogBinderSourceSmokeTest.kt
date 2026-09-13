package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigDialogBinderSourceSmokeTest {
    @Test
    fun composeEditorOwnsTypefaceAndHookChainDestinations() {
        val host = read("src/main/java/com/dpis/module/appconfig/AppConfigEditor.kt")
        val content = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")
        val overlay = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")
        val typefacePage = read("src/main/java/com/dpis/module/appconfig/presentation/AppTypefacePickerPage.kt")
        val hookPage = read("src/main/java/com/dpis/module/fonts/presentation/HookChainEditorPage.kt")
        val templates = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")

        assertTrue(host.contains("interface AppConfigEditorHost"))
        assertTrue(host.contains("fun toggleScope("))
        assertTrue(host.contains("fun getFontHookDomainsButtonText("))
        assertFalse(host.contains("fun bind(dialogView: View"))
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
        val process = read("src/main/java/com/dpis/module/appconfig/AppConfigEditor.kt")
        val actions = read("src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt")
        val gateway = read(
            "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt",
        )

        assertTrue(process.contains("enum class AppConfigProcessAction"))
        assertTrue(process.contains("START"))
        assertTrue(process.contains("RESTART"))
        assertTrue(process.contains("STOP"))
        assertTrue(actions.contains("host.executeProcessAction(AppConfigProcessAction.START)"))
        assertTrue(gateway.contains("fun typefaceSelectorText(typefaceId: String?): String"))
        assertTrue(gateway.contains("AppConfigTypefaceLabels.selectorText(activity, typefaceId)"))
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
    fun dialogStateKeepsIndependentViewportInputs() {
        val stateSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogState.kt")
        assertTrue(stateSource.contains("fun viewportInputFor(viewportTargetType: String?)"))
        assertTrue(stateSource.contains("fun clearViewportInputs()"))
        assertTrue(stateSource.contains("fun updateViewportInput("))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
