@file:android.annotation.SuppressLint("LocalContextGetResourceValueCall")

package com.dpis.module.ui.compose

import android.content.Intent
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dpis.module.AppWorkspacePresentation
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.SettingsUiState
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListPage
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
    var filtersVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (state.selectedPage != AppListPage.ALL_APPS) {
            state.actions.changePage(AppListPage.ALL_APPS)
        }
    }
    if (filtersVisible) {
        WearAppFilterPage(
            filterState = state.filterState,
            onFilterChanged = state.actions::changeFilters,
            onBack = { filtersVisible = false },
        )
        return
    }
    WearWorkspaceList(title = R.string.workspace_app) {
        wearSearchFieldWithAction(
            key = "query",
            value = state.query,
            label = context.getString(R.string.search_hint),
            actionDescription = context.getString(R.string.filter_button),
            onValueChanged = state.actions::changeQuery,
            onActionClick = { filtersVisible = true },
        )
        wearPageTabs(
            selectedPage = state.selectedPage,
            onSelect = state.actions::changePage
        )
        state.visibleItems.forEach { item ->
            wearAppCard(
                key = item.packageName,
                label = item.label,
                secondaryLabel = item.packageName,
                icon = item.icon,
                onClick = { state.actions.openApp(item) }
            )
        }
    }
}

@Composable
private fun WearAppFilterPage(
    filterState: AppListFilterState,
    onFilterChanged: (AppListFilterState) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    WearWorkspaceList(title = R.string.filter_sheet_title) {
        wearSwitch(
            key = "show-system",
            label = R.string.filter_show_system_apps,
            checked = filterState.showSystemApps(),
            enabled = true,
        ) { checked ->
            onFilterChanged(
                AppListFilterState(
                    checked,
                    filterState.injectedOnly(),
                    filterState.widthConfiguredOnly(),
                    filterState.fontConfiguredOnly(),
                )
            )
        }
        wearSwitch(
            key = "injected-only",
            label = R.string.filter_scoped_only,
            checked = filterState.injectedOnly(),
            enabled = true,
        ) { checked ->
            onFilterChanged(
                AppListFilterState(
                    filterState.showSystemApps(),
                    checked,
                    filterState.widthConfiguredOnly(),
                    filterState.fontConfiguredOnly(),
                )
            )
        }
        wearSwitch(
            key = "width-only",
            label = R.string.filter_width_only,
            checked = filterState.widthConfiguredOnly(),
            enabled = true,
        ) { checked ->
            onFilterChanged(
                AppListFilterState(
                    filterState.showSystemApps(),
                    filterState.injectedOnly(),
                    checked,
                    filterState.fontConfiguredOnly(),
                )
            )
        }
        wearSwitch(
            key = "font-only",
            label = R.string.filter_font_only,
            checked = filterState.fontConfiguredOnly(),
            enabled = true,
        ) { checked ->
            onFilterChanged(
                AppListFilterState(
                    filterState.showSystemApps(),
                    filterState.injectedOnly(),
                    filterState.widthConfiguredOnly(),
                    checked,
                )
            )
        }
    }
}

/** Full-screen Wear editor; it deliberately never creates a phone bottom sheet. */
@Composable
internal fun WearAppConfigEditorContent(state: EditorPresentation.State) {
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
            selectedTypefaceId = state.draft.selectedTypefaceId ?: "",
            onTypefaceSelected = { typefaceId -> state.actions.updateTypeface(typefaceId ?: "") },
            onBack = closeOrBack
        )
        else -> WearWorkspaceList(title = R.string.dialog_title) {
            wearSwitch("scope", if (state.isScopeSelected) R.string.dialog_scope_in_scope else R.string.dialog_scope_apply, state.isScopeSelected, true) { state.actions.toggleScope() }
            wearSwitch("enabled", if (state.isDpisEnabled) R.string.dialog_config_disable else R.string.dialog_config_disabled, state.isDpisEnabled, true) { state.actions.toggleDpisEnabled() }
            wearTextFieldModeRow(
                key = "viewport",
                value = state.draft.viewportInputFor(state.draft.viewportMode),
                label = context.getString(R.string.dialog_viewport_hint_scale),
                onValueChanged = state.actions::updateViewportInput,
                startLabel = context.getString(R.string.dialog_viewport_mode_system),
                endLabel = context.getString(R.string.dialog_viewport_mode_compat),
                startSelected = !state.usesAbsoluteViewport(),
                onStartSelected = {
                    state.actions.changeViewportMode(ViewportTargetType.RELATIVE_SCALE)
                },
                onEndSelected = {
                    state.actions.changeViewportMode(ViewportTargetType.ABSOLUTE_DP)
                },
            )
            wearTextFieldModeRow(
                key = "font",
                value = state.draft.fontInput,
                label = context.getString(R.string.dialog_font_scale_hint),
                onValueChanged = state.actions::updateFontInput,
                startLabel = context.getString(R.string.dialog_font_mode_system),
                endLabel = context.getString(R.string.dialog_font_mode_compat),
                startSelected = state.usesSystemFontMode(),
                onStartSelected = {
                    state.actions.changeFontMode(FontApplyMode.SYSTEM_EMULATION)
                },
                onEndSelected = {
                    state.actions.changeFontMode(FontApplyMode.FIELD_REWRITE)
                },
            )
            wearButton("typeface", context.getString(R.string.dialog_typeface_dialog_title), state.typefaceSelectorText, R.drawable.ic_adjust_24, onClick = { state.actions.navigate(ConfigEditorDestination.TYPEFACE) })
            wearButton("hooks", context.getString(R.string.dialog_font_hook_domains_title), state.hookChainText, R.drawable.ic_checklist_rtl_24, onClick = { state.actions.navigate(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) })
            if (state.showsWechatDpi()) {
                wearTextField("wechat", state.draft.wechatDpiInput, context.getString(R.string.dialog_wechat_dpi_hint), state.actions::updateWechatDpiInput)
                wearButton("wechat-help", context.getString(R.string.dialog_wechat_dpi_help_button), icon = R.drawable.ic_question_mark_24, onClick = state.actions::showWechatDpiHelp)
            }
            wearActionRow(
                startLabel = context.getString(R.string.dialog_start_button),
                stopLabel = context.getString(R.string.dialog_stop_button),
                restartLabel = context.getString(R.string.dialog_restart_button),
                onStart = state.actions::startProcess,
                onStop = state.actions::stopProcess,
                onRestart = state.actions::restartProcess
            )
            wearButton("save", context.getString(R.string.status_save_button), icon = R.drawable.ic_save_24dp, enabled = state.saveEnabled, onClick = state.actions::save)
            wearButton("diagnostic", context.getString(R.string.feedback_diagnostic_action), icon = R.drawable.ic_bug_report_24, onClick = state.actions::startFeedbackDiagnostic)
            wearButton("reset", context.getString(R.string.dialog_disable_button), icon = R.drawable.ic_refresh_24, onClick = state.actions::reset)
        }
    }
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
    val effectiveViewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
        .let { if (it == ViewportApplyMode.OFF) ViewportApplyMode.AUTO else it }
    if (destination == ConfigEditorDestination.HOOK_CHAIN_INTERFACE) {
        WearWorkspaceList(title = R.string.dialog_hook_chain_tab_interface) {
            wearDualTabRow(
                key = "hook-tabs",
                startLabel = context.getString(R.string.dialog_hook_chain_tab_interface),
                endLabel = context.getString(R.string.dialog_hook_chain_tab_font),
                startSelected = true,
                onStartSelected = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) },
                onEndSelected = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_FONT) },
            )
            listOf(
                ViewportApplyMode.AUTO to R.string.dialog_viewport_apply_auto,
                ViewportApplyMode.SYSTEM to R.string.dialog_viewport_apply_system,
                ViewportApplyMode.COMPAT to R.string.dialog_viewport_apply_compat
            ).forEach { (mode, label) ->
                wearRadioButton(
                    key = "mode:$mode",
                    label = context.getString(label),
                    selected = effectiveViewportApplyMode == mode,
                    onClick = {
                        onChanged(rawDomains.orEmpty(), resetDomains, mode, false)
                    }
                )
            }
        }
    } else {
        WearWorkspaceList(title = R.string.dialog_hook_chain_tab_font) {
            wearDualTabRow(
                key = "hook-tabs",
                startLabel = context.getString(R.string.dialog_hook_chain_tab_interface),
                endLabel = context.getString(R.string.dialog_hook_chain_tab_font),
                startSelected = false,
                onStartSelected = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_INTERFACE) },
                onEndSelected = { onDestinationChanged(ConfigEditorDestination.HOOK_CHAIN_FONT) },
            )
            wearButton(
                key = "restore-recommended",
                label = context.getString(R.string.dialog_font_hook_domains_restore_button),
                icon = R.drawable.ic_reset_settings_24,
                onClick = {
                    onChanged("", true, effectiveViewportApplyMode, false)
                }
            )
            FontHookDomainRegistry.orderedCustomizableDisplayIdsList().forEach { id ->
                wearSwitch("domain:$id", FontHookDomainRegistry.titleResFor(id), selected.contains(id), editable) { checked ->
                    val next = selected.toMutableSet().apply { if (checked) add(id) else remove(id) }
                    val raw = HookDomainOverrideStore.rawValueForSelection(next, automaticDomains, unknown)
                    onChanged(raw.orEmpty(), raw == null, effectiveViewportApplyMode, false)
                }
            }
        }
    }
}

@Composable
internal fun WearHomeWorkspaceContent(state: HomeWorkspaceBinder.State) {
    val context = LocalContext.current
    val checkForUpdates: () -> Unit = if (state.xposedModuleActivated) {
        state.actions::checkForUpdates
    } else {
        {}
    }
    WearWorkspaceList(title = R.string.workspace_home) {
        wearButton(
            key = "status",
            label = context.getString(if (state.xposedModuleActivated) R.string.home_workspace_status_enabled else R.string.home_workspace_status_enable_in_lsposed),
            icon = if (state.xposedModuleActivated) R.drawable.ic_check_24 else R.drawable.ic_error_outline_24,
            onClick = checkForUpdates
        )
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
            automaticDomains = FontHookDomainRegistry.automaticCustomizableDomains(),
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
            wearTextFieldModeRow(
                key = "viewport",
                value = editorDraft.form.viewportInput,
                label = context.getString(R.string.dialog_viewport_hint_scale),
                onValueChanged = {
                    editorDraft.form.viewportInput = it
                    editorDraft.form.updateActiveViewportDraft()
                    changed()
                },
                startLabel = context.getString(R.string.dialog_viewport_mode_system),
                endLabel = context.getString(R.string.dialog_viewport_mode_compat),
                startSelected = ViewportTargetType.ABSOLUTE_DP != editorDraft.form.viewportMode,
                onStartSelected = {
                    editorDraft.form.switchViewportMode(ViewportTargetType.RELATIVE_SCALE)
                    changed()
                },
                onEndSelected = {
                    editorDraft.form.switchViewportMode(ViewportTargetType.ABSOLUTE_DP)
                    changed()
                },
            )
            wearTextFieldModeRow(
                key = "font",
                value = editorDraft.form.fontInput,
                label = context.getString(R.string.dialog_font_scale_hint),
                onValueChanged = {
                    editorDraft.form.fontInput = it
                    changed()
                },
                startLabel = context.getString(R.string.dialog_font_mode_system),
                endLabel = context.getString(R.string.dialog_font_mode_compat),
                startSelected = editorDraft.form.fontMode == FontApplyMode.SYSTEM_EMULATION,
                onStartSelected = {
                    editorDraft.form.fontMode = FontApplyMode.SYSTEM_EMULATION
                    changed()
                },
                onEndSelected = {
                    editorDraft.form.fontMode = FontApplyMode.FIELD_REWRITE
                    changed()
                },
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
    onRequestPermission: () -> Unit
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
    }
}

@Composable
internal fun WearSettingsWorkspaceContent(
    state: SettingsUiState?,
    onHooksChanged: (Boolean) -> Unit,
    onSafeModeChanged: (Boolean) -> Unit,
    onGlobalLogChanged: (Boolean) -> Unit,
    onOpenLogs: () -> Unit,
    onLauncherHiddenChanged: (Boolean) -> Unit,
    onFontLibrary: () -> Unit,
    onExperimental: () -> Unit,
    onThemeSettings: () -> Unit,
    onLanguage: () -> Unit,
    onBackup: () -> Unit,
    onClearCache: () -> Unit,
    onAbout: () -> Unit
) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_settings) {
        wearSectionHeader(R.string.system_settings_section_general)
        wearSwitch("hooks", R.string.system_hooks_enabled_label, state?.systemHooksEnabled == true, state?.storeAvailable == true, onHooksChanged)
        wearSwitch("safe", R.string.system_safe_mode_label, state?.safeModeEnabled == true, state?.storeAvailable == true, onSafeModeChanged)
        wearSwitch("logs", R.string.global_log_enabled_label, state?.globalLogEnabled == true, state?.storeAvailable == true, onGlobalLogChanged)
        if (state?.globalLogEnabled == true) wearButton("logs-page", context.getString(R.string.tools_log_title), context.getString(R.string.tools_log_subtitle), R.drawable.ic_overview_24, enabled = state.storeAvailable, onClick = onOpenLogs)
        wearButton("font-library", context.getString(R.string.settings_font_library_label), icon = R.drawable.ic_upload_file_24, enabled = state?.storeAvailable == true, onClick = onFontLibrary)
        wearButton("experimental", context.getString(R.string.settings_experimental_title), icon = R.drawable.ic_experiment_24, enabled = state?.storeAvailable == true, onClick = onExperimental)
        wearSectionHeader(R.string.settings_section_theme)
        wearButton("theme-settings", context.getString(R.string.settings_theme_settings_title), context.getString(R.string.settings_theme_settings_hint), R.drawable.ic_format_paint_24, onClick = onThemeSettings)
        wearButton("language", context.getString(R.string.settings_language_label), state?.languageLabel, R.drawable.ic_language_24, state?.storeAvailable == true, onLanguage)
        wearSectionHeader(R.string.settings_section_other)
        wearButton("backup", context.getString(R.string.settings_config_backup_label), icon = R.drawable.ic_upload_file_24, enabled = state?.storeAvailable == true, onClick = onBackup)
        wearButton("cache", context.getString(R.string.settings_clear_cache_label), state?.cacheUsage, R.drawable.ic_mop_24, state?.storeAvailable == true && state.cacheClearInProgress != true, onClearCache)
        wearSwitch("launcher", R.string.settings_hide_launcher_icon_label, state?.launcherIconHidden == true, state?.storeAvailable == true, onLauncherHiddenChanged)
        wearSectionHeader(R.string.settings_section_about)
        wearButton("about", context.getString(R.string.settings_about_label), icon = R.drawable.ic_info_24, onClick = onAbout)
    }
}

internal class WearListScope(
    private val scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope,
    private val transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    fun wearSectionHeader(@StringRes title: Int) = with(scope) {
        item(key = "section:$title") {
            ListHeader(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Text(stringResource(title))
            }
        }
    }

    fun wearSearchField(key: Any, value: String, label: String, onValueChanged: (String) -> Unit) = with(scope) {
        item(key = key) {
            var focused by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                contentPadding = CardDefaults.ContentPadding,
                transformation = SurfaceTransformation(transformationSpec)
            ) {
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
                    modifier = Modifier.fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth()) {
                            if (value.isEmpty() && !focused) {
                                Text(
                                    label,
                                    maxLines = 1,
                                    color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }

    fun wearSearchFieldWithAction(
        key: Any,
        value: String,
        label: String,
        actionDescription: String,
        onValueChanged: (String) -> Unit,
        onActionClick: () -> Unit
    ) = with(scope) {
        item(key = key) {
            var focused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    contentPadding = CardDefaults.ContentPadding,
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
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
                        modifier = Modifier.fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                        decorationBox = { inner ->
                            Box(Modifier.fillMaxWidth()) {
                                if (value.isEmpty() && !focused) {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Button(
                    onClick = rememberConfirmAction(onActionClick),
                    label = {},
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_tune_24),
                            contentDescription = actionDescription
                        )
                    },
                    modifier = Modifier.size(56.dp),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }

    fun wearTextField(key: Any, value: String, label: String, onValueChanged: (String) -> Unit) = with(scope) {
        item(key = key) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                contentPadding = CardDefaults.ContentPadding,
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Column(Modifier.fillMaxWidth()) {
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

    fun wearTextFieldModeRow(
        key: Any,
        value: String,
        label: String,
        onValueChanged: (String) -> Unit,
        startLabel: String,
        endLabel: String,
        startSelected: Boolean,
        onStartSelected: () -> Unit,
        onEndSelected: () -> Unit
    ) = with(scope) {
        item(key = key) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                contentPadding = CardDefaults.ContentPadding,
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Column(Modifier.fillMaxWidth()) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wearModeChoiceButton(
                            label = startLabel,
                            selected = startSelected,
                            modifier = Modifier.weight(1f),
                            onClick = onStartSelected
                        )
                        wearModeChoiceButton(
                            label = endLabel,
                            selected = !startSelected,
                            modifier = Modifier.weight(1f),
                            onClick = onEndSelected
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun wearModeChoiceButton(
        label: String,
        selected: Boolean,
        modifier: Modifier,
        onClick: () -> Unit
    ) {
        if (selected) {
            Button(
                onClick = rememberConfirmAction(onClick),
                label = { wearCenteredButtonLabel(label) },
                icon = {},
                modifier = modifier
            )
        } else {
            OutlinedButton(
                onClick = rememberConfirmAction(onClick),
                label = { wearCenteredButtonLabel(label) },
                icon = {},
                modifier = modifier
            )
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
                onClick = rememberConfirmAction(onClick),
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

    fun wearInfoCard(
        key: Any,
        title: String,
        secondaryLabel: String? = null,
        tertiaryLabel: String? = null,
        @DrawableRes icon: Int? = null
    ) = with(scope) {
        item(key = key) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                contentPadding = CardDefaults.ContentPadding,
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        secondaryLabel?.takeIf(String::isNotBlank)?.let { value ->
                            Text(
                                value,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        tertiaryLabel?.takeIf(String::isNotBlank)?.let { value ->
                            Text(
                                value,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    fun wearAppCard(
        key: Any,
        label: String,
        secondaryLabel: String,
        icon: android.graphics.drawable.Drawable?,
        onClick: () -> Unit
    ) = with(scope) {
        item(key = key) {
            Card(
                onClick = rememberConfirmAction(onClick),
                modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                contentPadding = CardDefaults.ContentPadding,
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WearAppIcon(
                        packageName = secondaryLabel,
                        initialIcon = icon,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Text(
                                label,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Text(
                                secondaryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = androidx.wear.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    fun wearPageTabs(
        selectedPage: AppListPage,
        onSelect: (AppListPage) -> Unit
    ) = with(scope) {
        item(key = "tabs") {
            val safeSelectedPage = selectedPage
            val allLabel = stringResource(R.string.tab_all_apps)
            val configuredLabel = stringResource(R.string.tab_configured_apps)
            Card(
                modifier = Modifier.fillMaxWidth()
                    .height(52.dp)
                    .transformedHeight(this, transformationSpec),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (safeSelectedPage == AppListPage.ALL_APPS) {
                        Button(
                            onClick = rememberConfirmAction { onSelect(AppListPage.ALL_APPS) },
                            label = { wearCenteredButtonLabel(allLabel) },
                            icon = {},
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedButton(
                            onClick = rememberConfirmAction { onSelect(AppListPage.ALL_APPS) },
                            label = { wearCenteredButtonLabel(allLabel) },
                            icon = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (safeSelectedPage == AppListPage.CONFIGURED_APPS) {
                        Button(
                            onClick = rememberConfirmAction { onSelect(AppListPage.CONFIGURED_APPS) },
                            label = { wearCenteredButtonLabel(configuredLabel) },
                            icon = {},
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedButton(
                            onClick = rememberConfirmAction { onSelect(AppListPage.CONFIGURED_APPS) },
                            label = { wearCenteredButtonLabel(configuredLabel) },
                            icon = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    fun wearDualTabRow(
        key: Any,
        startLabel: String,
        endLabel: String,
        startSelected: Boolean,
        onStartSelected: () -> Unit,
        onEndSelected: () -> Unit
    ) = with(scope) {
        item(key = key) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (startSelected) {
                    Button(
                        onClick = rememberConfirmAction(onStartSelected),
                        label = { wearCenteredButtonLabel(startLabel) },
                        icon = {},
                        modifier = Modifier.weight(1f),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                } else {
                    OutlinedButton(
                        onClick = rememberConfirmAction(onStartSelected),
                        label = { wearCenteredButtonLabel(startLabel) },
                        icon = {},
                        modifier = Modifier.weight(1f),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
                if (!startSelected) {
                    Button(
                        onClick = rememberConfirmAction(onEndSelected),
                        label = { wearCenteredButtonLabel(endLabel) },
                        icon = {},
                        modifier = Modifier.weight(1f),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                } else {
                    OutlinedButton(
                        onClick = rememberConfirmAction(onEndSelected),
                        label = { wearCenteredButtonLabel(endLabel) },
                        icon = {},
                        modifier = Modifier.weight(1f),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
            }
        }
    }

    fun wearRadioButton(
        key: Any,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) = with(scope) {
        item(key = key) {
            Button(
                onClick = rememberConfirmAction(onClick),
                label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                icon = {
                    Icon(
                        painterResource(
                            if (selected) {
                                R.drawable.ic_radio_button_checked_24
                            } else {
                                R.drawable.ic_radio_button_unchecked_24
                            }
                        ),
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth()
                    .transformedHeight(this, transformationSpec)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                transformation = SurfaceTransformation(transformationSpec)
            )
        }
    }

    fun wearActionRow(
        startLabel: String,
        stopLabel: String,
        restartLabel: String,
        onStart: () -> Unit,
        onStop: () -> Unit,
        onRestart: () -> Unit
    ) = with(scope) {
        item(key = "process_actions") {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .height(52.dp)
                    .transformedHeight(this, transformationSpec),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                transformation = SurfaceTransformation(transformationSpec)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wearIconActionButton(
                        description = startLabel,
                        icon = R.drawable.ic_wear_play_arrow_24,
                        modifier = Modifier.weight(1f),
                        onClick = onStart
                    )
                    wearIconActionButton(
                        description = stopLabel,
                        icon = R.drawable.ic_wear_stop_24,
                        modifier = Modifier.weight(1f),
                        onClick = onStop
                    )
                    wearIconActionButton(
                        description = restartLabel,
                        icon = R.drawable.ic_wear_restart_alt_24,
                        modifier = Modifier.weight(1f),
                        onClick = onRestart
                    )
                }
            }
        }
    }

    @Composable
    fun wearSmallActionButton(
        label: String,
        modifier: Modifier,
        enabled: Boolean = true,
        secondaryLabel: String? = null,
        onClick: () -> Unit
    ) {
        Button(
            onClick = rememberConfirmAction(onClick),
            enabled = enabled,
            label = { wearCenteredButtonLabel(label) },
            secondaryLabel = secondaryLabel?.let { value ->
                { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
            icon = {},
            modifier = modifier
        )
    }

    @Composable
    fun wearIconActionButton(
        description: String,
        @DrawableRes icon: Int,
        modifier: Modifier,
        onClick: () -> Unit
    ) {
        Button(
            onClick = rememberConfirmAction(onClick),
            label = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(icon),
                        contentDescription = description
                    )
                }
            },
            icon = {},
            modifier = modifier
        )
    }

    @Composable
    fun wearCenteredButtonLabel(label: String) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }

    fun wearSwitch(key: Any, @StringRes label: Int, checked: Boolean, enabled: Boolean, onChanged: (Boolean) -> Unit) = with(scope) {
        item(key = key) {
            SwitchButton(
                label = { Text(LocalContext.current.getString(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
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
private fun WearAppIcon(
    packageName: String,
    initialIcon: android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier
) {
    val icon = rememberInstalledAppIcon(packageName, initialIcon)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            // The surface is a fallback for unresolved icons, not a permanent icon mask.
            .then(
                if (icon == null) {
                    Modifier.background(
                        androidx.wear.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { it.setImageDrawable(icon) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_apps_24),
                contentDescription = null
            )
        }
    }
}

@Composable
internal fun WearWorkspaceList(@StringRes title: Int, content: WearListScope.() -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    WearDpisMaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.wear.compose.material3.MaterialTheme.colorScheme.background
        ) {
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
                    item(key = "bottom-spacer") {
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun WearDpisMaterialTheme(content: @Composable () -> Unit) {
    val phoneColors = androidx.compose.material3.MaterialTheme.colorScheme
    val wearColors = androidx.wear.compose.material3.MaterialTheme.colorScheme.copy(
        primary = phoneColors.primary,
        primaryContainer = phoneColors.primaryContainer,
        onPrimary = phoneColors.onPrimary,
        onPrimaryContainer = phoneColors.onPrimaryContainer,
        secondary = phoneColors.secondary,
        secondaryContainer = phoneColors.secondaryContainer,
        onSecondary = phoneColors.onSecondary,
        onSecondaryContainer = phoneColors.onSecondaryContainer,
        tertiary = phoneColors.tertiary,
        tertiaryContainer = phoneColors.tertiaryContainer,
        onTertiary = phoneColors.onTertiary,
        onTertiaryContainer = phoneColors.onTertiaryContainer,
        surfaceContainerLow = phoneColors.surfaceContainerLow,
        surfaceContainer = phoneColors.surfaceContainer,
        surfaceContainerHigh = phoneColors.surfaceContainerHigh,
        onSurface = phoneColors.onSurface,
        onSurfaceVariant = phoneColors.onSurfaceVariant,
        outline = phoneColors.outline,
        outlineVariant = phoneColors.outlineVariant,
        background = phoneColors.background,
        onBackground = phoneColors.onBackground,
        error = phoneColors.error,
        errorContainer = phoneColors.errorContainer,
        onError = phoneColors.onError,
        onErrorContainer = phoneColors.onErrorContainer
    )
    androidx.wear.compose.material3.MaterialTheme(colorScheme = wearColors, content = content)
}
