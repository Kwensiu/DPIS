package com.dpis.module.fonts.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.appconfig.editor.EditorPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.presentation.design.LocalSemanticColors
import com.dpis.module.ui.presentation.design.rememberClickValueAction
import com.dpis.module.ui.presentation.editor.EditorSheetChildPageHeader
import com.dpis.module.ui.presentation.editor.FeedbackOutlinedButton
import com.dpis.module.ui.presentation.editor.FeedbackSwitch
import com.dpis.module.ui.presentation.editor.SheetDestinationAnimatedContent
import com.dpis.module.ui.presentation.workspace.dpisSegmentedShapes
import com.dpis.module.ui.presentation.workspace.segmentedRowColors
import com.dpis.module.viewport.ViewportApplyMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Animates destinations inside one editor surface without replacing its sheet or detail pane. */
@Composable
internal fun ConfigEditorAnimatedContent(
    destination: ConfigEditorDestination,
    modifier: Modifier = Modifier,
    animateSize: Boolean = true,
    clipContentToAnimatedBounds: Boolean = true,
    mainContent: @Composable () -> Unit,
    hookContent: @Composable () -> Unit,
    typefaceContent: (@Composable () -> Unit)? = null
) {
    val editorPage = when {
        destination == ConfigEditorDestination.TYPEFACE -> ConfigEditorPage.TYPEFACE
        destination.isHookChain -> ConfigEditorPage.HOOK_CHAIN
        else -> ConfigEditorPage.MAIN
    }
    SheetDestinationAnimatedContent(
        targetState = editorPage,
        modifier = modifier,
        animateSize = animateSize,
        clipContentToAnimatedBounds = clipContentToAnimatedBounds,
        towardChild = { it != ConfigEditorPage.MAIN },
        label = "config-editor-destination",
    ) { targetPage ->
        when (targetPage) {
            ConfigEditorPage.TYPEFACE -> {
                if (typefaceContent != null) {
                    typefaceContent()
                } else {
                    mainContent()
                }
            }

            ConfigEditorPage.HOOK_CHAIN -> hookContent()
            ConfigEditorPage.MAIN -> mainContent()
        }
    }
}

private enum class ConfigEditorPage { MAIN, HOOK_CHAIN, TYPEFACE }

/** Child content of an existing configuration session; it edits the caller-owned draft directly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HookChainEditorPage(
    destination: ConfigEditorDestination,
    rawDomains: String?,
    fontDomainsResetRequested: Boolean,
    automaticDomains: Set<String>,
    fontDomainsEditable: Boolean,
    viewportApplyMode: String,
    onHookChainChanged: (String, Boolean, String, Boolean) -> Unit,
    onDestinationChanged: (ConfigEditorDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    BackHandler(onBack = onBack)
    val override = remember(rawDomains) { HookDomainOverrideStore.fromRaw(rawDomains) }
    val usesAutomaticDomains = fontDomainsResetRequested || !override.customPathEnabled
    var selectedDomains by remember(rawDomains, fontDomainsResetRequested, automaticDomains) {
        mutableStateOf(
            (if (usesAutomaticDomains) automaticDomains else override.enabledKnownDomains).toSet()
        )
    }
    var selectedApplyMode by remember(viewportApplyMode) {
        mutableStateOf(displayViewportApplyMode(viewportApplyMode))
    }
    val unknownDomains = remember(rawDomains, fontDomainsResetRequested) {
        if (usesAutomaticDomains) emptySet() else override.unknownDomains
    }
    val destinationPage = destination.hookChainTabIndex()
    val pagerState = rememberPagerState(initialPage = destinationPage, pageCount = { 2 })
    val pagerScope = rememberCoroutineScope()
    val selectDestination = rememberClickValueAction<ConfigEditorDestination> { nextDestination ->
        pagerScope.launch { pagerState.animateScrollToPage(nextDestination.hookChainTabIndex()) }
    }
    val currentDestination by rememberUpdatedState(destination)
    val interfacePage = ConfigEditorDestination.HOOK_CHAIN_INTERFACE.hookChainTabIndex()
    val fontPage = ConfigEditorDestination.HOOK_CHAIN_FONT.hookChainTabIndex()

    // The saved editor destination remains authoritative across rotation and restoration. Pager
    // state is only the gesture/animation surface and reports a new destination once the nearest
    // snap page changes, without waiting for the remaining fling animation to finish.
    LaunchedEffect(destinationPage) {
        if (destination.isHookChain && pagerState.settledPage != destinationPage) {
            pagerState.animateScrollToPage(destinationPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val selectedDestination = ConfigEditorDestination.forHookChainTab(page)
                if (currentDestination.isHookChain &&
                    selectedDestination != currentDestination) {
                    onDestinationChanged(selectedDestination)
                }
            }
    }

    fun commitDomains(next: Set<String>) {
        selectedDomains = next
        val raw = HookDomainOverrideStore.rawValueForSelection(
            next, automaticDomains, unknownDomains
        )
        onHookChainChanged(
            raw ?: "",
            raw == null,
            selectedApplyMode,
            ViewportApplyMode.OFF.equals(selectedApplyMode)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        EditorSheetChildPageHeader(
            title = stringResource(R.string.dialog_font_hook_domains_title),
            onBack = onBack,
        )
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            listOf(
                ConfigEditorDestination.HOOK_CHAIN_INTERFACE to
                    R.string.dialog_hook_chain_tab_interface,
                ConfigEditorDestination.HOOK_CHAIN_FONT to
                    R.string.dialog_hook_chain_tab_font
            ).forEach { (tabDestination, titleRes) ->
                Tab(
                    selected = pagerState.currentPage == tabDestination.hookChainTabIndex(),
                    onClick = {
                        if (pagerState.currentPage != tabDestination.hookChainTabIndex()) {
                            selectDestination(tabDestination)
                        }
                    },
                    text = { Text(stringResource(titleRes)) }
                )
            }
        }
        val fontPagePadding = PaddingValues(
            start = HookChainPageTokens.HorizontalPadding,
            top = HookChainPageTokens.ContentTopPadding,
            end = HookChainPageTokens.HorizontalPadding,
            bottom = HookChainPageTokens.ContentBottomPadding + bottomPadding
        )
        val interfacePagePadding = PaddingValues(
            start = HookChainPageTokens.HorizontalPadding,
            top = HookChainPageTokens.InterfaceContentTopPadding,
            end = HookChainPageTokens.HorizontalPadding,
            bottom = HookChainPageTokens.InterfaceContentBottomPadding + bottomPadding
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { page ->
            if (page == fontPage) {
                FontDomainsPage(
                    selectedDomains = selectedDomains,
                    automaticDomains = automaticDomains,
                    unknownDomains = unknownDomains,
                    editable = fontDomainsEditable,
                    onSelectedDomainsChanged = ::commitDomains,
                    contentPadding = fontPagePadding
                )
            } else {
                ViewportApplyModePage(
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
                    },
                    contentPadding = interfacePagePadding
                )
            }
        }
    }
}

@Composable
internal fun AppHookChainEditorPage(
    state: EditorPresentation.State,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    bottomPadding: Dp = 0.dp
) {
    HookChainEditorPage(
        destination = state.destination,
        rawDomains = state.draft.draftFontHookDomainsRaw,
        fontDomainsResetRequested = state.draft.fontHookDomainsResetRequested,
        automaticDomains = state.automaticFontHookDomains,
        fontDomainsEditable = state.draft.fontHookDomainsEditable(),
        viewportApplyMode = state.draft.viewportApplyMode,
        onHookChainChanged = state.actions::updateHookChain,
        onDestinationChanged = state.actions::navigate,
        onBack = onBack ?: { state.actions.navigate(state.destination.backDestination()) },
        modifier = modifier,
        bottomPadding = bottomPadding
    )
}

@Composable
private fun ViewportApplyModePage(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val selectMode = rememberClickValueAction(onModeSelected)
    val modes = listOf(
        Triple(
            ViewportApplyMode.AUTO,
            R.string.dialog_viewport_apply_auto,
            R.string.dialog_viewport_apply_auto_subtitle
        ),
        Triple(
            ViewportApplyMode.SYSTEM,
            R.string.dialog_viewport_apply_system,
            R.string.dialog_viewport_apply_system_subtitle
        ),
        Triple(
            ViewportApplyMode.COMPAT,
            R.string.dialog_viewport_apply_compat,
            R.string.dialog_viewport_apply_compat_subtitle
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(HookChainPageTokens.InterfaceRowGap)
    ) {
        Text(
            stringResource(R.string.dialog_viewport_apply_strategy_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        modes.forEach { (mode, labelRes, subtitleRes) ->
            val selected = selectedMode == mode
            Surface(
                onClick = { selectMode(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HookChainPageTokens.InterfaceRowMinHeight),
                shape = HookChainPageTokens.InterfaceRowShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceBright
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HookChainPageTokens.InterfaceRowHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HookChainPageTokens.RowContentGap)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(subtitleRes),
                            modifier = Modifier.padding(top = HookChainPageTokens.SubtitleTopGap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FontDomainsPage(
    selectedDomains: Set<String>,
    automaticDomains: Set<String>,
    unknownDomains: Set<String>,
    editable: Boolean,
    onSelectedDomainsChanged: (Set<String>) -> Unit,
    contentPadding: PaddingValues
) {
    val knownIds = FontHookDomainRegistry.orderedCustomizableDisplayIdsList()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        if (!editable) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = HookChainPageTokens.NoticeShape,
                    color = LocalSemanticColors.current.warningContainer
                ) {
                    Text(
                        stringResource(R.string.dialog_font_hook_domains_font_disabled_hint),
                        modifier = Modifier.padding(HookChainPageTokens.NoticePadding),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalSemanticColors.current.onWarningContainer
                    )
                }
            }
        }
        FontHookDomainRegistry.orderedGroups().forEach { group ->
            val groupIds = knownIds.filter { FontHookDomainRegistry.groupFor(it) == group }
            item(key = "group:$group") {
                Text(
                    stringResource(groupTitleRes(group)),
                    modifier = Modifier.padding(
                        top = HookChainPageTokens.GroupTopGap,
                        bottom = HookChainPageTokens.GroupBottomGap
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "domains:$group") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    groupIds.forEachIndexed { index, domainId ->
                        HookDomainOptionRow(
                            title = stringResource(FontHookDomainRegistry.titleResFor(domainId)),
                            domainId = domainId,
                            checked = selectedDomains.contains(domainId),
                            enabled = editable,
                            index = index,
                            total = groupIds.size,
                            onCheckedChange = { checked ->
                                val next = selectedDomains.toMutableSet()
                                if (checked) next.add(domainId) else next.remove(domainId)
                                onSelectedDomainsChanged(next)
                            }
                        )
                    }
                }
            }
        }
        if (unknownDomains.isNotEmpty()) {
            item { HorizontalDivider(Modifier.padding(top = HookChainPageTokens.GroupTopGap)) }
            item {
                Text(
                    stringResource(R.string.dialog_font_hook_domains_unknown_group),
                    modifier = Modifier.padding(top = HookChainPageTokens.GroupBottomGap),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(unknownDomains.toList(), key = { "unknown:$it" }) { domainId ->
                HookDomainOptionRow(
                    title = domainId,
                    domainId = domainId,
                    checked = true,
                    enabled = false,
                    index = 0,
                    total = 1,
                    onCheckedChange = {}
                )
            }
        }
        item {
            FeedbackOutlinedButton(
                onClick = { onSelectedDomainsChanged(automaticDomains) },
                enabled = editable,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HookChainPageTokens.ActionTopGap)
            ) {
                Text(stringResource(R.string.dialog_font_hook_domains_restore_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HookDomainOptionRow(
    title: String,
    domainId: String,
    checked: Boolean,
    enabled: Boolean,
    index: Int,
    total: Int,
    onCheckedChange: (Boolean) -> Unit
) {
    val updateChecked = rememberClickValueAction(onCheckedChange)
    val shapes = dpisSegmentedShapes(index, total)
    val disabledScrim = MaterialTheme.colorScheme.surface.copy(
        alpha = HookChainPageTokens.DisabledScrimAlpha
    )
    SegmentedListItem(
        onClick = { updateChecked(!checked) },
        enabled = enabled,
        modifier = Modifier
            // The disabled veil is drawn after the item. Clip it here so it cannot square off
            // the outer corners owned by the segmented-list shape policy.
            .clip(shapes.shape)
            .drawWithContent {
                drawContent()
                if (!enabled) drawRect(disabledScrim)
            },
        shapes = shapes,
        colors = segmentedRowColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ).copy(
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
            disabledSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        verticalAlignment = Alignment.CenterVertically,
        content = { Text(title) },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HookChainPageTokens.StatusDotGap)
            ) {
                hookDomainStatusColor(domainId)?.let { statusColor ->
                    Box(
                        modifier = Modifier
                            .size(HookChainPageTokens.StatusDotSize)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Text(
                    text = domainId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            FeedbackSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

private object HookChainPageTokens {
    val ContentTopPadding = 12.dp
    val ContentBottomPadding = 12.dp
    val HorizontalPadding = 20.dp
    // Keep the three strategy cards visually distinct without widening the page content.
    val InterfaceRowGap = 8.dp
    val InterfaceRowShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val InterfaceRowMinHeight = 64.dp
    val InterfaceContentTopPadding = 16.dp
    val InterfaceContentBottomPadding = 20.dp
    val InterfaceRowHorizontalPadding = 12.dp
    const val DisabledScrimAlpha = 0.42f
    val SubtitleTopGap = 2.dp
    val StatusDotSize = 6.dp
    val StatusDotGap = 6.dp
    val NoticePadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    val NoticeShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val RowContentGap = 12.dp
    val GroupTopGap = 8.dp
    val GroupBottomGap = 2.dp
    val ActionTopGap = 8.dp
    val RiskLow = Color(0xFF2E9E55)
    val RiskMedium = Color(0xFFD88916)
    val RiskHigh = Color(0xFFD64B45)
}

private fun hookDomainStatusColor(domainId: String): Color? = when (domainId) {
    FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE -> HookChainPageTokens.RiskLow
    FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE -> HookChainPageTokens.RiskMedium
    FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK,
    FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK,
    FontHookDomainRegistry.ID_RESOURCES_FONT -> HookChainPageTokens.RiskHigh
    else -> null
}

private fun displayViewportApplyMode(mode: String): String {
    val normalized = ViewportApplyMode.normalize(mode)
    return if (ViewportApplyMode.isEnabled(normalized)) normalized else ViewportApplyMode.AUTO
}

private fun groupTitleRes(group: String): Int = when (group) {
    FontHookDomainRegistry.GROUP_RESOURCES -> R.string.dialog_font_hook_group_resources
    FontHookDomainRegistry.GROUP_TEXT_VIEW_FALLBACK -> R.string.dialog_font_hook_group_text_view_fallback
    FontHookDomainRegistry.GROUP_WEB -> R.string.dialog_font_hook_group_web
    FontHookDomainRegistry.GROUP_CROSS_RUNTIME -> R.string.dialog_font_hook_group_cross_runtime
    else -> error("Unknown Hook domain group: $group")
}
