package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.viewport.ViewportApplyMode

/** Compose editor for the same Hook-domain override model used by the legacy dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppHookChainEditorSheet(
    rawDomains: String?,
    fontDomainsResetRequested: Boolean,
    automaticDomains: Set<String>,
    fontDomainsEditable: Boolean,
    viewportApplyMode: String,
    onHookChainChanged: (String, Boolean, String, Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val override = remember(rawDomains) { HookDomainOverrideStore.fromRaw(rawDomains) }
    // The draft's reset request is authoritative. The action API carries an empty string because
    // it is Java-facing, but an empty custom override is not the same as the automatic selection.
    val usesAutomaticDomains = fontDomainsResetRequested || !override.customPathEnabled
    var selectedDomains by remember(rawDomains, fontDomainsResetRequested, automaticDomains) {
        mutableStateOf(
            (if (usesAutomaticDomains) automaticDomains else override.enabledKnownDomains)
                .toSet()
        )
    }
    var selectedApplyMode by remember(viewportApplyMode) {
        mutableStateOf(ViewportApplyMode.normalize(viewportApplyMode))
    }
    val unknownDomains = remember(rawDomains, fontDomainsResetRequested) {
        if (usesAutomaticDomains) emptySet() else override.unknownDomains
    }

    fun commitDomains(next: Set<String>) {
        selectedDomains = next
        val raw = HookDomainOverrideStore.rawValueForSelection(next, automaticDomains, unknownDomains)
        onHookChainChanged(
            raw ?: "",
            raw == null,
            selectedApplyMode,
            ViewportApplyMode.OFF.equals(selectedApplyMode)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            DpisSheetVisualChrome()
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.dialog_hook_chain_tab_interface)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.dialog_hook_chain_tab_font)) }
                )
            }
            if (selectedTab == 0) {
                ViewportApplyModeEditor(
                    selectedMode = selectedApplyMode,
                    onModeSelected = { mode ->
                        selectedApplyMode = mode
                        val raw = HookDomainOverrideStore.rawValueForSelection(
                            selectedDomains, automaticDomains, unknownDomains
                        )
                        onHookChainChanged(
                            raw ?: "",
                            raw == null,
                            mode,
                            ViewportApplyMode.OFF.equals(mode)
                        )
                    }
                )
            } else {
                FontDomainsEditor(
                    selectedDomains = selectedDomains,
                    automaticDomains = automaticDomains,
                    unknownDomains = unknownDomains,
                    editable = fontDomainsEditable,
                    onSelectedDomainsChanged = ::commitDomains
                )
            }
        }
    }
}

@Composable
private fun ViewportApplyModeEditor(selectedMode: String, onModeSelected: (String) -> Unit) {
    val modes = listOf(
        ViewportApplyMode.AUTO to R.string.dialog_viewport_apply_auto,
        ViewportApplyMode.SYSTEM to R.string.dialog_viewport_apply_system,
        ViewportApplyMode.COMPAT to R.string.dialog_viewport_apply_compat
    )
    Column(
        Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.dialog_viewport_apply_strategy_title), style = MaterialTheme.typography.titleMedium)
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(stringResource(label)) }
            )
        }
    }
}

@Composable
private fun FontDomainsEditor(
    selectedDomains: Set<String>,
    automaticDomains: Set<String>,
    unknownDomains: Set<String>,
    editable: Boolean,
    onSelectedDomainsChanged: (Set<String>) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (!editable) {
            Text(
                stringResource(R.string.dialog_font_hook_domains_font_disabled_hint),
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(FontHookDomainRegistry.orderedCustomizableIdsList(), key = { it }) { domainId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(FontHookDomainRegistry.titleResFor(domainId)))
                    Switch(
                        checked = selectedDomains.contains(domainId),
                        enabled = editable,
                        onCheckedChange = { checked ->
                            val next = selectedDomains.toMutableSet()
                            if (checked) next.add(domainId) else next.remove(domainId)
                            onSelectedDomainsChanged(next)
                        }
                    )
                }
            }
            if (unknownDomains.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(
                        HookDomainOverrideStore.formatCsv(emptySet(), unknownDomains),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Button(
                    onClick = { onSelectedDomainsChanged(automaticDomains) },
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth().padding(20.dp)
                ) { Text(stringResource(R.string.dialog_font_hook_domains_restore_button)) }
            }
        }
    }
}
