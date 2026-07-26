package com.dpis.module.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.AppConfigEditorPresentation
import com.dpis.module.R
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
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
    hookContent: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = destination.isHookChain(),
        modifier = modifier
            .fillMaxWidth()
            .then(if (clipContentToAnimatedBounds) Modifier.clipToBounds() else Modifier),
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(EditorDestinationAnimationDurationMillis),
                initialOffsetX = { direction * it }
            ) + fadeIn(tween(EditorDestinationFadeDurationMillis))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(EditorDestinationAnimationDurationMillis),
                    targetOffsetX = { -direction * it }
                ) + fadeOut(tween(EditorDestinationFadeDurationMillis))) using
                SizeTransform(
                    clip = false,
                    sizeAnimationSpec = { _, _ ->
                        if (animateSize) {
                            tween(EditorDestinationHeightDurationMillis)
                        } else {
                            snap()
                        }
                    }
                )
        },
        contentKey = { it },
        label = "config-editor-destination"
    ) { showingHook ->
        if (showingHook) hookContent() else mainContent()
    }
}

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
    animateTabSize: Boolean = true,
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
    val currentDestination by rememberUpdatedState(destination)
    val interfacePage = ConfigEditorDestination.HOOK_CHAIN_INTERFACE.hookChainTabIndex()
    val fontPage = ConfigEditorDestination.HOOK_CHAIN_FONT.hookChainTabIndex()

    // The saved editor destination remains authoritative across rotation and restoration. Pager
    // state is only the gesture/animation surface and reports a new destination once the nearest
    // snap page changes, without waiting for the remaining fling animation to finish.
    LaunchedEffect(destinationPage) {
        if (destination.isHookChain() && pagerState.settledPage != destinationPage) {
            pagerState.animateScrollToPage(destinationPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val selectedDestination = ConfigEditorDestination.forHookChainTab(page)
                if (currentDestination.isHookChain() &&
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
            .then(
                if (animateTabSize) {
                    Modifier.animateContentSize(tween(HookTabHeightDurationMillis))
                } else {
                    Modifier
                }
            )
            .heightIn(max = HookChainPageTokens.MaxContentHeight)
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back_24),
                    contentDescription = stringResource(R.string.system_settings_back)
                )
            }
            Text(
                stringResource(R.string.dialog_font_hook_domains_title),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge
            )
        }
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
            listOf(
                ConfigEditorDestination.HOOK_CHAIN_INTERFACE to
                    R.string.dialog_hook_chain_tab_interface,
                ConfigEditorDestination.HOOK_CHAIN_FONT to
                    R.string.dialog_hook_chain_tab_font
            ).forEach { (tabDestination, titleRes) ->
                Tab(
                    selected = pagerState.currentPage == tabDestination.hookChainTabIndex(),
                    onClick = {
                        pagerScope.launch {
                            pagerState.animateScrollToPage(tabDestination.hookChainTabIndex())
                        }
                    },
                    text = { Text(stringResource(titleRes)) }
                )
            }
        }
        val pagePadding = PaddingValues(
            start = HookChainPageTokens.HorizontalPadding,
            top = HookChainPageTokens.ContentTopPadding,
            end = HookChainPageTokens.HorizontalPadding,
            bottom = HookChainPageTokens.ContentBottomPadding + bottomPadding
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
                    contentPadding = pagePadding
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
                    contentPadding = pagePadding
                )
            }
        }
    }
}

@Composable
internal fun AppHookChainEditorPage(
    state: AppConfigEditorPresentation.State,
    modifier: Modifier = Modifier,
    animateTabSize: Boolean = true,
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
        animateTabSize = animateTabSize,
        bottomPadding = bottomPadding
    )
}

@Composable
private fun ViewportApplyModePage(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    contentPadding: PaddingValues
) {
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
                onClick = { onModeSelected(mode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HookChainPageTokens.InterfaceRowMinHeight),
                shape = HookChainPageTokens.InterfaceRowShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
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
                        onClick = { onModeSelected(mode) }
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
                    color = colorResource(R.color.font_hook_domain_notice_container)
                ) {
                    Text(
                        stringResource(R.string.dialog_font_hook_domains_font_disabled_hint),
                        modifier = Modifier.padding(HookChainPageTokens.NoticePadding),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorResource(R.color.font_hook_domain_notice_text)
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
                            warning = if (domainId == FontHookDomainRegistry.ID_RESOURCES_FONT) {
                                stringResource(R.string.dialog_font_hook_domain_resources_font_warning)
                            } else null,
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
                    warning = null,
                    checked = true,
                    enabled = false,
                    index = 0,
                    total = 1,
                    onCheckedChange = {}
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { onSelectedDomainsChanged(automaticDomains) },
                enabled = editable,
                modifier = Modifier.fillMaxWidth().padding(top = HookChainPageTokens.ActionTopGap)
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
    warning: String?,
    checked: Boolean,
    enabled: Boolean,
    index: Int,
    total: Int,
    onCheckedChange: (Boolean) -> Unit
) {
    val defaultShapes = ListItemDefaults.segmentedShapes(index, total)
    val singleItemShape = HookChainPageTokens.SingleItemShape
    val shapes = if (total == 1) {
        defaultShapes.copy(
            shape = singleItemShape,
            selectedShape = singleItemShape,
            pressedShape = singleItemShape,
            focusedShape = singleItemShape,
            hoveredShape = singleItemShape,
            draggedShape = singleItemShape
        )
    } else {
        defaultShapes
    }
    val disabledScrim = MaterialTheme.colorScheme.surface.copy(
        alpha = HookChainPageTokens.DisabledScrimAlpha
    )
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = Modifier.drawWithContent {
            drawContent()
            if (!enabled) drawRect(disabledScrim)
        },
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ).copy(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
            disabledSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        verticalAlignment = Alignment.CenterVertically,
        content = { Text(title) },
        supportingContent = {
            Column {
                Text(
                    domainId,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (warning != null) {
                    Text(
                        warning,
                        modifier = Modifier.padding(top = HookChainPageTokens.SubtitleTopGap),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(R.color.font_hook_domain_notice_text)
                    )
                }
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

private object HookChainPageTokens {
    val MaxContentHeight = 520.dp
    val ContentTopPadding = 16.dp
    val ContentBottomPadding = 20.dp
    val HorizontalPadding = 20.dp
    val InterfaceRowGap = 2.dp
    val InterfaceRowShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val InterfaceRowMinHeight = 64.dp
    val InterfaceRowHorizontalPadding = 12.dp
    val SingleItemShape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    const val DisabledScrimAlpha = 0.42f
    val SubtitleTopGap = 2.dp
    val NoticePadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    val NoticeShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    val RowContentGap = 12.dp
    val GroupTopGap = 10.dp
    val GroupBottomGap = 2.dp
    val ActionTopGap = 12.dp
}

private const val EditorDestinationAnimationDurationMillis = 220
private const val EditorDestinationFadeDurationMillis = 140
private const val EditorDestinationHeightDurationMillis = 180
private const val HookTabHeightDurationMillis = 180

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
