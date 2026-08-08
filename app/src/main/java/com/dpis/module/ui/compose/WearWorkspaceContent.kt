@file:android.annotation.SuppressLint("LocalContextGetResourceValueCall")

package com.dpis.module.ui.compose

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dpis.module.AppWorkspacePresentation
import com.dpis.module.AppConfigEditorPresentation
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.SettingsUiState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.home.HomeWorkspaceBinder
import com.dpis.module.settings.SystemFontScaleToolState
import com.dpis.module.templates.TemplateWorkspacePresentation
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.hooks.HookDomainOverrideStore

/** Wear-native presentation for the five main workspaces. Domain state remains Java-owned. */
@Composable
internal fun WearAppWorkspaceContent(state: AppWorkspacePresentation.State) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_app) {
        wearTextField("query", state.query, context.getString(R.string.search_hint), state.actions::changeQuery)
        wearButton("all", context.getString(R.string.tab_all_apps), state.allAppsCount.toString(), R.drawable.ic_apps_24, onClick = { state.actions.changePage(AppListPage.ALL_APPS) })
        wearButton("configured", context.getString(R.string.tab_configured_apps), state.configuredAppsCount.toString(), R.drawable.ic_check_24, onClick = { state.actions.changePage(AppListPage.CONFIGURED_APPS) })
        wearButton("refresh", context.getString(R.string.log_action_refresh), icon = R.drawable.ic_refresh_24, enabled = !state.refreshing, onClick = { state.actions.refresh(state.selectedPage) })
        state.visibleItems.forEach { item ->
            wearButton(
                key = item.packageName,
                label = item.label,
                secondaryLabel = wearAppSummary(item),
                icon = R.drawable.ic_apps_24,
                onClick = { state.actions.openApp(item) }
            )
        }
    }
}

/** Full-screen Wear editor; it deliberately never creates a phone bottom sheet. */
@Composable
internal fun WearAppConfigEditorContent(state: AppConfigEditorPresentation.State) {
    val context = LocalContext.current
    val closeOrBack = {
        if (state.destination.isChildPage) {
            state.actions.navigate(state.destination.backDestination())
        } else {
            state.actions.close()
        }
    }
    BackHandler(onBack = closeOrBack)
    when {
        state.destination.isHookChain -> WearHookChainEditorPage(
            destination = state.destination,
            rawDomains = state.draft.draftFontHookDomainsRaw,
            resetDomains = state.draft.fontHookDomainsResetRequested,
            automaticDomains = state.automaticFontHookDomains,
            editable = state.draft.fontHookDomainsEditable(),
            viewportApplyMode = state.draft.viewportApplyMode,
            onChanged = state.actions::updateHookChain,
            onDestinationChanged = state.actions::navigate,
            onBack = closeOrBack
        )
        state.destination == ConfigEditorDestination.TYPEFACE -> WearTypefacePickerPage(
            selectedTypefaceId = state.draft.selectedTypefaceId,
            onTypefaceSelected = state.actions::updateTypeface,
            onBack = closeOrBack
        )
        else -> WearWorkspaceList(title = R.string.dialog_title) {
            wearButton("back", context.getString(R.string.system_settings_back), icon = R.drawable.ic_arrow_back_24, onClick = closeOrBack)
            wearSwitch("scope", R.string.dialog_scope_button, state.isScopeSelected, true) { state.actions.toggleScope() }
            wearSwitch("enabled", R.string.dialog_dpis_enable_button, state.isDpisEnabled, true) { state.actions.toggleDpisEnabled() }
            val viewport = state.draft.viewportInputFor(state.draft.viewportMode).toDoubleOrNull() ?: 0.0
            wearTextField("viewport", state.draft.viewportInputFor(state.draft.viewportMode), context.getString(R.string.dialog_viewport_hint_scale), state.actions::updateViewportInput)
            wearButton("viewport-minus", context.getString(R.string.system_font_scale_decrement), wearNumber(viewport), R.drawable.ic_remove_24, onClick = { state.actions.updateViewportInput(wearNumber(viewport - 1.0)) })
            wearButton("viewport-plus", context.getString(R.string.system_font_scale_increment), wearNumber(viewport), R.drawable.ic_add_24, onClick = { state.actions.updateViewportInput(wearNumber(viewport + 1.0)) })
            wearButton(
                "viewport-mode",
                context.getString(if (state.usesAbsoluteViewport()) R.string.dialog_viewport_mode_compat else R.string.dialog_viewport_mode_system),
                icon = R.drawable.ic_fit_width_24,
                onClick = {
                    state.actions.changeViewportMode(if (state.usesAbsoluteViewport()) ViewportTargetType.RELATIVE_SCALE else ViewportTargetType.ABSOLUTE_DP)
                }
            )
            val font = state.draft.fontInput.toIntOrNull() ?: 100
            wearTextField("font", state.draft.fontInput, context.getString(R.string.dialog_font_scale_hint), state.actions::updateFontInput)
            wearButton("font-minus", context.getString(R.string.system_font_scale_decrement), "$font%", R.drawable.ic_remove_24, onClick = { state.actions.updateFontInput((font - 1).toString()) })
            wearButton("font-plus", context.getString(R.string.system_font_scale_increment), "$font%", R.drawable.ic_add_24, onClick = { state.actions.updateFontInput((font + 1).toString()) })
            wearButton(
                "font-mode",
                context.getString(if (state.usesSystemFontMode()) R.string.dialog_font_mode_system else R.string.dialog_font_mode_compat),
                icon = R.drawable.ic_adjust_24,
                onClick = {
                    state.actions.changeFontMode(if (state.usesSystemFontMode()) FontApplyMode.FIELD_REWRITE else FontApplyMode.SYSTEM_EMULATION)
                }
            )
            wearButton("typeface", context.getString(R.string.dialog_typeface_dialog_title), state.typefaceSelectorText, R.drawable.ic_adjust_24, onClick = { state.actions.navigate(ConfigEditorDestination.TYPEFACE) })
            wearButton("hooks", context.getString(R.string.dialog_font_hook_domains_title), state.hookChainText, R.drawable.ic_checklist_rtl_24, onClick = { state.actions.navigate(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) })
            if (state.showsWechatDpi()) {
                wearTextField("wechat", state.draft.wechatDpiInput, context.getString(R.string.dialog_wechat_dpi_hint), state.actions::updateWechatDpiInput)
                wearButton("wechat-help", context.getString(R.string.dialog_wechat_dpi_help_button), icon = R.drawable.ic_question_mark_24, onClick = state.actions::showWechatDpiHelp)
            }
            wearButton("launch", context.getString(R.string.dialog_start_button), icon = R.drawable.ic_play_arrow_24, onClick = state.actions::startProcess)
            wearButton("restart", context.getString(R.string.dialog_restart_button), icon = R.drawable.ic_refresh_24, onClick = state.actions::restartProcess)
            wearButton("stop", context.getString(R.string.dialog_stop_button), icon = R.drawable.ic_pause_24, onClick = state.actions::stopProcess)
            wearButton("diagnostic", context.getString(R.string.feedback_diagnostic_action), icon = R.drawable.ic_bug_report_24, onClick = state.actions::startFeedbackDiagnostic)
            wearButton("reset", context.getString(R.string.dialog_disable_button), icon = R.drawable.ic_refresh_24, onClick = state.actions::reset)
            wearButton("save", context.getString(R.string.status_save_button), icon = R.drawable.ic_save_24dp, enabled = state.saveEnabled, onClick = state.actions::save)
        }
    }
}

private fun wearNumber(value: Double): String = if (value % 1.0 == 0.0) {
    value.toInt().toString()
} else {
    value.toString()
}

@Composable
private fun WearTypefacePickerPage(
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    val systemFonts = SystemFontRegistry.listRecommendedFonts()
    val library = ConfigStoreFactory.createLocalUiFontLibraryStore(context, null)
    val importedFonts = library.listFonts()
    WearWorkspaceList(title = R.string.dialog_typeface_dialog_title) {
        wearButton("back", context.getString(R.string.system_settings_back), icon = R.drawable.ic_arrow_back_24, onClick = onBack)
        wearButton("default", context.getString(R.string.dialog_typeface_default), icon = if (selectedTypefaceId == null) R.drawable.ic_check_24 else R.drawable.ic_adjust_24, onClick = { onTypefaceSelected(null) })
        systemFonts.forEach { font ->
            wearButton("system:${font.id()}", font.displayName(), font.id(), if (selectedTypefaceId == font.id()) R.drawable.ic_check_24 else R.drawable.ic_adjust_24, onClick = { onTypefaceSelected(font.id()) })
        }
        importedFonts.forEach { font ->
            wearButton("imported:${font.id}", font.displayName, font.id, if (selectedTypefaceId == font.id) R.drawable.ic_check_24 else R.drawable.ic_upload_file_24, onClick = { onTypefaceSelected(font.id) })
        }
        wearButton("manage", context.getString(R.string.dialog_typeface_manage_action), icon = R.drawable.ic_settings_24, onClick = {
            context.startActivity(Intent(context, FontLibraryActivity::class.java))
        })
    }
}

@Composable
private fun WearHookChainEditorPage(
    destination: ConfigEditorDestination,
    rawDomains: String?,
    resetDomains: Boolean,
    automaticDomains: Set<String>,
    editable: Boolean,
    viewportApplyMode: String,
    onChanged: (String, Boolean, String, Boolean) -> Unit,
    onDestinationChanged: (ConfigEditorDestination) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    val override = HookDomainOverrideStore.fromRaw(rawDomains)
    val automatic = resetDomains || !override.customPathEnabled
    val selected = if (automatic) automaticDomains else override.enabledKnownDomains
    val unknown = if (automatic) emptySet() else override.unknownDomains
    if (destination == ConfigEditorDestination.HOOK_CHAIN_INTERFACE) {
        WearWorkspaceList(title = R.string.dialog_hook_chain_tab_interface) {
            wearButton("back", context.getString(R.string.system_settings_back), icon = R.drawable.ic_arrow_back_24, onClick = onBack)
            wearButton("font-tab", context.getString(R.string.dialog_hook_chain_tab_font), icon = R.drawable.ic_chevron_right_24, onClick = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_FONT) })
            listOf(
                ViewportApplyMode.AUTO to R.string.dialog_viewport_apply_auto,
                ViewportApplyMode.SYSTEM to R.string.dialog_viewport_apply_system,
                ViewportApplyMode.COMPAT to R.string.dialog_viewport_apply_compat,
                ViewportApplyMode.OFF to R.string.dialog_disable_button
            ).forEach { (mode, label) ->
                wearButton("mode:$mode", context.getString(label), icon = if (ViewportApplyMode.normalize(viewportApplyMode) == mode) R.drawable.ic_check_24 else R.drawable.ic_fit_width_24, onClick = {
                    onChanged(rawDomains.orEmpty(), resetDomains, mode, mode == ViewportApplyMode.OFF)
                })
            }
        }
    } else {
        WearWorkspaceList(title = R.string.dialog_hook_chain_tab_font) {
            wearButton("back", context.getString(R.string.system_settings_back), icon = R.drawable.ic_arrow_back_24, onClick = onBack)
            wearButton("interface-tab", context.getString(R.string.dialog_hook_chain_tab_interface), icon = R.drawable.ic_chevron_right_24, onClick = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) })
            FontHookDomainRegistry.orderedCustomizableDisplayIdsList().forEach { id ->
                wearSwitch("domain:$id", FontHookDomainRegistry.titleResFor(id), selected.contains(id), editable) { checked ->
                    val next = selected.toMutableSet().apply { if (checked) add(id) else remove(id) }
                    val raw = HookDomainOverrideStore.rawValueForSelection(next, automaticDomains, unknown)
                    onChanged(raw.orEmpty(), raw == null, viewportApplyMode, ViewportApplyMode.OFF == viewportApplyMode)
                }
            }
        }
    }
}

@Composable
internal fun WearHomeWorkspaceContent(state: HomeWorkspaceBinder.State) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_home) {
        wearButton(
            key = "status",
            label = context.getString(if (state.xposedModuleActivated) R.string.home_workspace_status_enabled else R.string.home_workspace_status_enable_in_lsposed),
            secondaryLabel = state.updateState.subtitle(context),
            icon = if (state.xposedModuleActivated) R.drawable.ic_check_24 else R.drawable.ic_error_outline_24,
            onClick = if (state.updateState.status == HomeUpdateUiState.Status.FAILED) state.actions::retryUpdateCheck else ({})
        )
        if (state.updateState.showsUpdateActionCard()) {
            wearButton("release-notes", context.getString(R.string.home_update_action_release_notes), icon = R.drawable.ic_notes_24, onClick = state.actions::showReleaseNotes)
            wearButton(
                "update-action",
                context.getString(if (state.updateState.status == HomeUpdateUiState.Status.INSTALL_READY) R.string.home_update_action_install_ready else R.string.home_update_action_install),
                icon = R.drawable.ic_save_24dp,
                onClick = if (state.updateState.status == HomeUpdateUiState.Status.INSTALL_READY) state.actions::installDownloadedUpdate else state.actions::startUpdateDownload
            )
        }
        wearButton(
            key = "apps",
            label = context.getString(R.string.workspace_app),
            secondaryLabel = state.configuredAppCount.toString(),
            icon = R.drawable.ic_apps_24,
            onClick = state.actions::openConfiguredAppsWorkspace
        )
        wearButton(
            key = "fonts",
            label = context.getString(R.string.settings_font_library_label),
            secondaryLabel = state.importedFontCount.toString(),
            icon = R.drawable.ic_upload_file_24,
            onClick = state.actions::openFontLibrary
        )
        wearButton(
            key = "templates",
            label = context.getString(R.string.workspace_template),
            secondaryLabel = state.templateCount.toString(),
            icon = R.drawable.ic_template_24,
            onClick = state.actions::openTemplateWorkspace
        )
    }
}

@Composable
internal fun WearTemplateWorkspaceContent(
    state: TemplateWorkspacePresentation.State,
    onEditorChanged: (TemplateEditorForm) -> Unit,
    onDestinationChanged: (ConfigEditorDestination) -> Unit,
    onEditorClosed: () -> Unit
) {
    if (state.detailKind == TemplateWorkspacePresentation.DetailKind.GLOBAL_PREFILL ||
        state.detailKind == TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE
    ) {
        WearTemplateEditorContent(state, onEditorChanged, onDestinationChanged, onEditorClosed)
        return
    }
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_template) {
        wearButton(
            key = "global",
            label = context.getString(R.string.template_workspace_global_prefill_title),
            secondaryLabel = state.globalPrefillSummaryParts.joinToString(" / "),
            icon = R.drawable.ic_settings_24,
            onClick = state.actions::editGlobalPrefill
        )
        wearButton(
            key = "create",
            label = context.getString(R.string.quick_template_create_action),
            icon = R.drawable.ic_add_24,
            onClick = state.actions::createTemplate
        )
        wearButton("sort", context.getString(R.string.quick_template_sort_title), icon = R.drawable.ic_sort_24, onClick = state.actions::sortTemplates)
        state.templates.forEach { template ->
            wearButton(
                key = template.id,
                label = template.name,
                secondaryLabel = template.summaryParts.joinToString(" / "),
                icon = R.drawable.ic_template_24,
                onClick = { state.actions.editTemplate(template.id) }
            )
            wearButton("apply:${template.id}", context.getString(R.string.template_workspace_action_apply), template.name, R.drawable.ic_check_24, onClick = { state.actions.applyTemplate(template.id) })
            wearButton("targets:${template.id}", context.getString(R.string.template_workspace_action_select_apps), template.name, R.drawable.ic_apps_24, onClick = { state.actions.selectTargets(template.id) })
        }
    }
}

@Composable
private fun WearTemplateEditorContent(
    state: TemplateWorkspacePresentation.State,
    onEditorChanged: (TemplateEditorForm) -> Unit,
    onDestinationChanged: (ConfigEditorDestination) -> Unit,
    onEditorClosed: () -> Unit
) {
    val context = LocalContext.current
    val quick = state.detailKind == TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE
    val selectedTemplate = state.templates.firstOrNull { it.id == state.detailTemplateId }
    val editorKey = "wear:${state.detailKind}:${state.detailTemplateId.orEmpty()}"
    val editorDraft = rememberTemplateEditorDraftState(editorKey) {
        if (quick) {
            TemplateEditorForm.quick(
                selectedTemplate?.let {
                    QuickTemplateStore.QuickTemplate(
                        it.id, it.name, 0L, linkedSetOf(), it.configValue
                    )
                },
                ""
            ).also { it.applyDraft(state.quickTemplateDraft) }
        } else {
            TemplateEditorForm.global(state.globalPrefill).also {
                it.applyDraft(state.globalPrefillDraft)
            }
        }
    }
    fun changed() {
        editorDraft.changed()
        onEditorChanged(editorDraft.form)
    }
    fun save() {
        val result = if (editorDraft.form.quickTemplate) {
            state.actions.saveQuickTemplate(editorDraft.form)
        } else {
            state.actions.saveGlobalPrefill(editorDraft.form)
        }
        if (result.success) {
            editorDraft.form.markSaved(result.templateId)
            changed()
        }
    }
    val closeOrBack = {
        if (state.editorDestination.isChildPage) {
            onDestinationChanged(state.editorDestination.backDestination())
        } else {
            onEditorClosed()
        }
    }
    BackHandler(onBack = closeOrBack)
    @Suppress("UNUSED_VARIABLE") val revision = editorDraft.observe()
    when {
        state.editorDestination.isHookChain -> WearHookChainEditorPage(
            destination = state.editorDestination,
            rawDomains = editorDraft.form.fontHookDomainsRaw,
            resetDomains = editorDraft.form.fontHookDomainsRaw == null,
            automaticDomains = FontHookDomainRegistry.recommendedTemplateKnownDomains(),
            editable = editorDraft.form.fontMode == FontApplyMode.FIELD_REWRITE,
            viewportApplyMode = editorDraft.form.viewportApplyMode,
            onChanged = { raw, reset, mode, _ ->
                editorDraft.form.fontHookDomainsRaw = if (reset) null else raw
                editorDraft.form.viewportApplyMode = mode
                changed()
            },
            onDestinationChanged = onDestinationChanged,
            onBack = closeOrBack,
        )
        state.editorDestination == ConfigEditorDestination.TYPEFACE -> WearTypefacePickerPage(
            selectedTypefaceId = editorDraft.form.selectedTypefaceId,
            onTypefaceSelected = {
                editorDraft.form.selectedTypefaceId = it
                changed()
            },
            onBack = closeOrBack
        )
        else -> WearWorkspaceList(title = if (quick) {
            if (editorDraft.form.newTemplate) R.string.quick_template_edit_page_title_new else R.string.quick_template_edit_page_title_edit
        } else R.string.template_workspace_global_prefill_title) {
            wearButton("back", context.getString(R.string.system_settings_back), icon = R.drawable.ic_arrow_back_24, onClick = closeOrBack)
            if (quick) wearTextField("name", editorDraft.form.nameInput, context.getString(R.string.quick_template_name_hint)) {
                editorDraft.form.nameInput = it
                changed()
            }
            wearTextField("viewport", editorDraft.form.viewportInput, context.getString(R.string.dialog_viewport_hint_scale)) {
                editorDraft.form.viewportInput = it
                editorDraft.form.updateActiveViewportDraft()
                changed()
            }
            wearButton(
                "viewport-mode",
                context.getString(if (ViewportTargetType.ABSOLUTE_DP == editorDraft.form.viewportMode) R.string.dialog_viewport_mode_compat else R.string.dialog_viewport_mode_system),
                icon = R.drawable.ic_fit_width_24,
                onClick = {
                    editorDraft.form.switchViewportMode(if (ViewportTargetType.ABSOLUTE_DP == editorDraft.form.viewportMode) ViewportTargetType.RELATIVE_SCALE else ViewportTargetType.ABSOLUTE_DP)
                    changed()
                }
            )
            wearTextField("font", editorDraft.form.fontInput, context.getString(R.string.dialog_font_scale_hint)) {
                editorDraft.form.fontInput = it
                changed()
            }
            wearButton(
                "font-mode",
                context.getString(if (editorDraft.form.fontMode == FontApplyMode.SYSTEM_EMULATION) R.string.dialog_font_mode_system else R.string.dialog_font_mode_compat),
                icon = R.drawable.ic_adjust_24,
                onClick = {
                    editorDraft.form.fontMode = if (editorDraft.form.fontMode == FontApplyMode.SYSTEM_EMULATION) FontApplyMode.FIELD_REWRITE else FontApplyMode.SYSTEM_EMULATION
                    changed()
                }
            )
            wearButton("typeface", context.getString(R.string.dialog_typeface_dialog_title), editorDraft.form.selectedTypefaceId, R.drawable.ic_adjust_24, onClick = { onDestinationChanged(ConfigEditorDestination.TYPEFACE) })
            wearButton("hooks", context.getString(R.string.dialog_font_hook_domains_title), icon = R.drawable.ic_checklist_rtl_24, onClick = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) })
            wearButton("reset", context.getString(R.string.template_workspace_action_reset), icon = R.drawable.ic_refresh_24, onClick = { editorDraft.form.reset(); changed() })
            wearButton("save", context.getString(R.string.status_save_button), icon = R.drawable.ic_save_24dp, enabled = editorDraft.form.isValid, onClick = ::save)
        }
    }
}

@Composable
internal fun WearToolsWorkspaceContent(
    state: SystemFontScaleToolState?,
    onPendingChanged: (Int) -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLogs: () -> Unit
) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_tools) {
        if (state != null) {
            wearButton(
                key = "font-minus",
                label = context.getString(R.string.system_font_scale_decrement),
                secondaryLabel = "${state.pendingPercent}%",
                icon = R.drawable.ic_remove_24,
                enabled = state.canDecrement(),
                onClick = { onPendingChanged(state.pendingPercent - 1) }
            )
            wearButton(
                key = "font-plus",
                label = context.getString(R.string.system_font_scale_increment),
                secondaryLabel = "${state.pendingPercent}%",
                icon = R.drawable.ic_add_24,
                enabled = state.canIncrement(),
                onClick = { onPendingChanged(state.pendingPercent + 1) }
            )
            if (!state.canWrite && !state.unavailable) {
                wearButton("permission", context.getString(R.string.system_font_scale_permission_button), icon = R.drawable.ic_settings_24, onClick = onRequestPermission)
            } else {
                wearButton("apply", context.getString(R.string.system_font_scale_apply), icon = R.drawable.ic_save_24dp, enabled = state.canApply(), onClick = onApply)
                wearButton("restore", context.getString(R.string.system_font_scale_restore_default), icon = R.drawable.ic_refresh_24, enabled = state.canRestore(), onClick = onRestore)
            }
        }
        wearButton("logs", context.getString(R.string.tools_log_title), context.getString(R.string.tools_log_subtitle), R.drawable.ic_notes_24, onClick = onOpenLogs)
    }
}

@Composable
internal fun WearSettingsWorkspaceContent(
    state: SettingsUiState?,
    onHooksChanged: (Boolean) -> Unit,
    onSafeModeChanged: (Boolean) -> Unit,
    onGlobalLogChanged: (Boolean) -> Unit,
    onLauncherHiddenChanged: (Boolean) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
    onInterfaceScaleDetails: () -> Unit,
    onFontDebug: () -> Unit,
    onFontLibrary: () -> Unit,
    onExperimental: () -> Unit,
    onLanguage: () -> Unit,
    onBackup: () -> Unit,
    onClearCache: () -> Unit,
    onAbout: () -> Unit,
    onDonate: () -> Unit
) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_settings) {
        wearSwitch("hooks", R.string.system_hooks_enabled_label, state?.systemHooksEnabled == true, state?.storeAvailable == true, onHooksChanged)
        wearSwitch("safe", R.string.system_safe_mode_label, state?.safeModeEnabled == true, state?.storeAvailable == true, onSafeModeChanged)
        wearSwitch("logs", R.string.global_log_enabled_label, state?.globalLogEnabled == true, state?.storeAvailable == true, onGlobalLogChanged)
        val scale = state?.interfaceScalePercent ?: 100
        wearButton("scale-minus", context.getString(R.string.system_font_scale_decrement), "$scale%", R.drawable.ic_remove_24, enabled = state?.storeAvailable == true, onClick = { onInterfaceScaleChanged(scale - 5) })
        wearButton("scale-plus", context.getString(R.string.system_font_scale_increment), "$scale%", R.drawable.ic_add_24, enabled = state?.storeAvailable == true, onClick = { onInterfaceScaleChanged(scale + 5) })
        wearButton("scale-details", context.getString(R.string.settings_interface_scale_dialog_title), icon = R.drawable.ic_fit_width_24, enabled = state?.storeAvailable == true, onClick = onInterfaceScaleDetails)
        wearButton("font-debug", context.getString(R.string.font_debug_overlay_label), icon = R.drawable.ic_bug_report_24, enabled = state?.storeAvailable == true, onClick = onFontDebug)
        wearButton("font-library", context.getString(R.string.settings_font_library_label), icon = R.drawable.ic_upload_file_24, enabled = state?.storeAvailable == true, onClick = onFontLibrary)
        wearButton("experimental", context.getString(R.string.settings_experimental_title), icon = R.drawable.ic_experiment_24, enabled = state?.storeAvailable == true, onClick = onExperimental)
        wearButton("language", context.getString(R.string.settings_language_label), state?.languageLabel, R.drawable.ic_language_24, state?.storeAvailable == true, onLanguage)
        wearButton("backup", context.getString(R.string.settings_config_backup_label), icon = R.drawable.ic_upload_file_24, enabled = state?.storeAvailable == true, onClick = onBackup)
        wearButton("cache", context.getString(R.string.settings_clear_cache_label), state?.cacheUsage, R.drawable.ic_mop_24, state?.storeAvailable == true && state.cacheClearInProgress != true, onClearCache)
        wearSwitch("launcher", R.string.settings_hide_launcher_icon_label, state?.launcherIconHidden == true, state?.storeAvailable == true, onLauncherHiddenChanged)
        wearButton("about", context.getString(R.string.settings_about_label), icon = R.drawable.ic_info_24, onClick = onAbout)
        wearButton("donate", context.getString(R.string.settings_donate_label), icon = R.drawable.ic_volunteer_24, onClick = onDonate)
    }
}

private fun wearAppSummary(item: AppListItem): String = when {
    !item.installed -> item.packageName
    item.configured -> item.packageName
    else -> item.packageName
}

private class WearListScope(
    private val scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope,
    private val transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    fun wearTextField(key: Any, value: String, label: String, onValueChanged: (String) -> Unit) = with(scope) {
        item(key = key) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(18.dp),
                color = androidx.wear.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        label,
                        maxLines = 1,
                        color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChanged,
                        singleLine = true,
                        textStyle = androidx.wear.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(
                            androidx.wear.compose.material3.MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }

    fun wearButton(
        key: Any,
        label: String,
        secondaryLabel: String? = null,
        @DrawableRes icon: Int,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) = with(scope) {
        item(key = key) {
            Button(
                onClick = rememberDpisConfirmAction(onClick),
                enabled = enabled,
                label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                secondaryLabel = secondaryLabel?.takeIf(String::isNotBlank)?.let { value ->
                    { Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                },
                icon = { Icon(painterResource(icon), contentDescription = null) },
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                transformation = SurfaceTransformation(transformationSpec)
            )
        }
    }

    fun wearSwitch(key: Any, @StringRes label: Int, checked: Boolean, enabled: Boolean, onChanged: (Boolean) -> Unit) = with(scope) {
        item(key = key) {
            SwitchButton(
                label = { Text(androidx.compose.ui.platform.LocalContext.current.getString(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                checked = checked,
                enabled = enabled,
                onCheckedChange = onChanged,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                transformation = SurfaceTransformation(transformationSpec)
            )
        }
    }
}

@Composable
private fun WearWorkspaceList(@StringRes title: Int, content: WearListScope.() -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = state,
        contentPadding = LocalWearWorkspaceContentPadding.current
    ) { contentPadding: PaddingValues ->
        TransformingLazyColumn(
            state = state,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") { ListHeader { Text(stringResource(title)) } }
            WearListScope(this, transformationSpec).content()
        }
    }
}
