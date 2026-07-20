package com.dpis.module

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpis.module.home.HomeWorkspaceBinder
import com.dpis.module.settings.SystemFontScaleToolState
import com.dpis.module.ui.compose.HomeWorkspaceContent
import com.dpis.module.ui.compose.ToolsWorkspaceContent
import com.dpis.module.ui.compose.SettingsWorkspaceContent
import com.dpis.module.ui.compose.TemplateWorkspaceContent
import com.dpis.module.templates.TemplateWorkspacePresentation

/** Compose workspace presentation boundary; domain actions remain in MainActivity. */
internal class MainWorkspacePresentationCoordinator(private val content: Content) {
    interface Content {
        fun homeState(): HomeWorkspaceBinder.State
        fun toolsState(): SystemFontScaleToolState?
        fun changeToolsPending(percent: Int)
        fun applyTools()
        fun restoreTools()
        fun requestToolsPermission()
        fun openToolsLogs()
        fun settingsState(): SettingsUiState?
        fun setSettingsHooks(enabled: Boolean)
        fun setSettingsSafeMode(enabled: Boolean)
        fun setSettingsGlobalLog(enabled: Boolean)
        fun setSettingsLauncherHidden(hidden: Boolean)
        fun setSettingsScale(percent: Int)
        fun openSettingsScaleDetails()
        fun openSettingsFontDebug(); fun openSettingsFontLibrary(); fun openSettingsExperimental()
        fun openSettingsLanguage(); fun openSettingsBackup(); fun clearSettingsCache(); fun openSettingsAbout(); fun openSettingsDonate()
        fun templateState(): TemplateWorkspacePresentation.State
        fun changeTemplateQuery(query: String)
        fun openTemplateEditor(quickTemplate: Boolean, templateId: String?)
        fun updateTemplateEditor(form: com.dpis.module.templates.TemplateEditorForm)
        fun closeTemplateEditor()
        fun usesComposeTemplateWorkspace(): Boolean
    }
    private var homeRevision by mutableStateOf(0)
    private var toolsRevision by mutableStateOf(0)
    private var toolsExpanded by mutableStateOf(false)
    private var settingsRevision by mutableStateOf(0)
    private var templateRevision by mutableStateOf(0)
    @Composable fun render(mode: MainUiState.WorkspaceMode, padding: PaddingValues): Boolean = when (mode) {
        MainUiState.WorkspaceMode.HOME -> { homeRevision; ComposeWorkspaceSurface { HomeWorkspaceContent(content.homeState(), padding) }; true }
        MainUiState.WorkspaceMode.TOOLS -> { toolsRevision; ComposeWorkspaceSurface { ToolsWorkspaceContent(content.toolsState(), padding, toolsExpanded, { toolsExpanded = !toolsExpanded }, content::changeToolsPending, content::applyTools, content::restoreTools, content::requestToolsPermission, content::openToolsLogs) }; true }
        MainUiState.WorkspaceMode.SETTINGS -> { settingsRevision; ComposeWorkspaceSurface { SettingsWorkspaceContent(content.settingsState(), padding, content::setSettingsHooks, content::setSettingsSafeMode, content::setSettingsGlobalLog, content::setSettingsLauncherHidden, content::setSettingsScale, content::openSettingsScaleDetails, content::openSettingsFontDebug, content::openSettingsFontLibrary, content::openSettingsExperimental, content::openSettingsLanguage, content::openSettingsBackup, content::clearSettingsCache, content::openSettingsAbout, content::openSettingsDonate) }; true }
        MainUiState.WorkspaceMode.TEMPLATE -> {
            if (!content.usesComposeTemplateWorkspace()) false
            else {
                templateRevision
                ComposeWorkspaceSurface {
                    TemplateWorkspaceContent(
                        state = content.templateState(),
                        padding = padding,
                        onQueryChanged = content::changeTemplateQuery,
                        onEditorOpened = { quickTemplate, templateId ->
                            content.openTemplateEditor(quickTemplate, templateId)
                        },
                        onEditorChanged = content::updateTemplateEditor,
                        onEditorClosed = content::closeTemplateEditor
                    )
                }
                true
            }
        }
        else -> false
    }
    fun refreshHome() { homeRevision++ }
    fun refreshTools(collapse: Boolean = false) { if (collapse) toolsExpanded = false; toolsRevision++ }
    fun refreshSettings() { settingsRevision++ }
    fun refreshTemplates() { templateRevision++ }
    fun owns(mode: MainUiState.WorkspaceMode): Boolean = mode == MainUiState.WorkspaceMode.HOME || mode == MainUiState.WorkspaceMode.TOOLS || mode == MainUiState.WorkspaceMode.SETTINGS || (mode == MainUiState.WorkspaceMode.TEMPLATE && content.usesComposeTemplateWorkspace())
}

/** Supplies the Material content color for unframed workspace titles and empty states. */
@Composable
private fun ComposeWorkspaceSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content
    )
}
