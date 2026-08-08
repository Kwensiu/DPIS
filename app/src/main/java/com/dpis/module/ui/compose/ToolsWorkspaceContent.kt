package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.dpis.module.R
import com.dpis.module.settings.SystemFontScaleToolState

@Composable
fun ToolsWorkspaceContent(
    state: SystemFontScaleToolState?,
    padding: PaddingValues,
    expanded: Boolean,
    onExpandedChanged: () -> Unit,
    onPendingChanged: (Int) -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Scaffold(
        modifier = Modifier.padding(padding),
        // The adaptive shell owns horizontal cutout insets; this page owns the top inset only.
        contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                title = {
                    Text(
                        stringResource(R.string.workspace_tools),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { topBarPadding ->
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = workspaceContentPadding(topBarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp)) }
        item {
            Card(
                onClick = rememberDpisConfirmAction(onExpandedChanged),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.system_font_scale_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                SystemFontScaleBadge(state)
                            }
                            Text(stringResource(R.string.system_font_scale_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = rememberDpisConfirmAction(onApply), enabled = state?.canApply() == true) {
                            Icon(painterResource(R.drawable.ic_save_24dp), stringResource(R.string.system_font_scale_apply))
                        }
                    }
                    if (expanded && state != null) {
                        if (!state.canWrite && !state.unavailable) {
                            Button(onClick = rememberDpisConfirmAction(onRequestPermission), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(stringResource(R.string.system_font_scale_permission_button)) }
                        } else if (state.unavailable) {
                            Text(stringResource(R.string.system_font_scale_unavailable_message), modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = rememberDpisConfirmAction { onPendingChanged(state.pendingPercent - 1) },
                                    enabled = state.canDecrement()
                                ) { Icon(painterResource(R.drawable.ic_remove_24), stringResource(R.string.system_font_scale_decrement)) }
                                Text("${state.pendingPercent}%", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = rememberDpisConfirmAction { onPendingChanged(state.pendingPercent + 1) },
                                    enabled = state.canIncrement()
                                ) { Icon(painterResource(R.drawable.ic_add_24), stringResource(R.string.system_font_scale_increment)) }
                            }
                            Slider(
                                value = state.pendingPercent.toFloat(),
                                onValueChange = {
                                    onPendingChanged(SystemFontScaleToolState.normalizeSliderPercent(it))
                                },
                                valueRange = 50f..200f,
                                steps = 0,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                val displayDensity = LocalDensity.current
                                // Keep the preview target-relative. The active system font scale
                                // must not be multiplied into a control that previews that scale.
                                CompositionLocalProvider(
                                    LocalDensity provides Density(displayDensity.density, fontScale = 1f)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(
                                            stringResource(R.string.system_font_scale_preview_title),
                                            fontSize = SystemFontScaleToolState.previewTextSp(
                                                SystemFontScaleToolState.PREVIEW_TITLE_SP,
                                                state.pendingPercent
                                            ).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            stringResource(R.string.system_font_scale_preview_body),
                                            modifier = Modifier.padding(top = 8.dp),
                                            fontSize = SystemFontScaleToolState.previewTextSp(
                                                SystemFontScaleToolState.PREVIEW_BODY_SP,
                                                state.pendingPercent
                                            ).sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = rememberDpisConfirmAction(onRestore),
                                enabled = state.canRestore(),
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                            ) { Text(stringResource(R.string.system_font_scale_restore_default)) }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SystemFontScaleBadge(state: SystemFontScaleToolState?) {
    val label = when (state?.badge()) {
        SystemFontScaleToolState.Badge.UNAVAILABLE -> R.string.system_font_scale_badge_unavailable
        SystemFontScaleToolState.Badge.PERMISSION_REQUIRED -> R.string.system_font_scale_badge_permission_required
        SystemFontScaleToolState.Badge.OUT_OF_RANGE -> R.string.system_font_scale_badge_out_of_range
        SystemFontScaleToolState.Badge.MODIFIED -> R.string.system_font_scale_badge_modified
        else -> null
    } ?: return
    Text(
        stringResource(label),
        modifier = Modifier.padding(start = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
