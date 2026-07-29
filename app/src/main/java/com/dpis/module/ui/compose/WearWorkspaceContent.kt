package com.dpis.module.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.dpis.module.R
import com.dpis.module.SettingsUiState
import com.dpis.module.applist.AppListItem
import com.dpis.module.home.HomeWorkspaceBinder
import com.dpis.module.settings.SystemFontScaleToolState
import com.dpis.module.templates.TemplateWorkspacePresentation

/** Wear-native presentation for the five main workspaces. Domain state remains Java-owned. */
@Composable
internal fun WearAppWorkspaceContent(state: AppWorkspacePresentation.State) {
    WearWorkspaceList(title = R.string.workspace_app) {
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

@Composable
internal fun WearHomeWorkspaceContent(state: HomeWorkspaceBinder.State) {
    val context = LocalContext.current
    WearWorkspaceList(title = R.string.workspace_home) {
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
internal fun WearTemplateWorkspaceContent(state: TemplateWorkspacePresentation.State) {
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
        state.templates.forEach { template ->
            wearButton(
                key = template.id,
                label = template.name,
                secondaryLabel = template.summaryParts.joinToString(" / "),
                icon = R.drawable.ic_template_24,
                onClick = { state.actions.editTemplate(template.id) }
            )
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
        // The compact workspace switch floats at the bottom of the AppScaffold.
        // Reserve its visual and touch area so the last row remains reachable.
        contentPadding = PaddingValues(bottom = 68.dp)
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
