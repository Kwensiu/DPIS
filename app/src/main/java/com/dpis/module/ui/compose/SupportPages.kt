package com.dpis.module.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R

/**
 * Compose presentation for standalone support activities. Activity classes retain
 * locale/scale wrapping and Intent contracts; this file owns only rendered state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateSupportPage(onBack: () -> Unit) {
    var supportersVisible by remember { mutableStateOf(false) }
    SecondaryPageScaffold(titleRes = R.string.donate_title, onBack = onBack) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 18.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SupportCard {
                    Text(stringResource(R.string.donate_message), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.donate_trust_note),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SupportCard(
                    modifier = Modifier.clickable(
                        onClick = rememberDpisConfirmAction { supportersVisible = true }
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.donate_supporters_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.donate_supporters_summary),
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { DonationQrCard(R.drawable.donate_wechat, R.string.donate_wechat_title, R.string.donate_wechat_qr_description) }
            item { DonationQrCard(R.drawable.donate_alipay, R.string.donate_alipay_title, R.string.donate_alipay_qr_description) }
        }
    }
    if (supportersVisible) {
        ModalBottomSheet(
            onDismissRequest = { supportersVisible = false },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            ),
            dragHandle = null
        ) {
            Column(Modifier.fillMaxWidth()) {
                DpisSheetVisualChrome()
                SupportersSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeHelpPage(onBack: () -> Unit, onOpenModeGuide: () -> Unit) {
    SecondaryPageScaffold(
        titleRes = R.string.mode_help_title,
        onBack = onBack
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.mode_help_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Spacer(
                        Modifier.width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(stringResource(R.string.mode_help_tip_font_lag_question), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.mode_help_tip_font_lag_steps), modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.mode_help_tip_font_lag_reason), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text(stringResource(R.string.mode_help_more_title), modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(
                        onClick = rememberDpisConfirmAction(onOpenModeGuide)
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_help_mode_guide_entry_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.mode_help_mode_guide_entry_summary), modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_chevron_right_24), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeGuidePage(onBack: () -> Unit) {
    SecondaryPageScaffold(
        titleRes = R.string.mode_guide_title,
        onBack = onBack
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.mode_guide_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { GuideSection(R.string.mode_help_font_routes_title) }
            item { GuideCard(R.string.help_tutorial_system_title, R.string.help_tutorial_system_badge, R.string.help_tutorial_system_summary, R.string.help_tutorial_system_points) }
            item { GuideCard(R.string.help_tutorial_compat_title, R.string.help_tutorial_compat_badge, R.string.help_tutorial_compat_summary, R.string.help_tutorial_compat_points) }
            item { GuideSection(R.string.mode_help_viewport_types_title, topPadding = true) }
            item { GuideCard(R.string.help_tutorial_scale_title, R.string.help_tutorial_scale_badge, R.string.help_tutorial_scale_summary, R.string.help_tutorial_scale_points) }
            item { GuideCard(R.string.help_tutorial_width_title, null, R.string.help_tutorial_width_summary, R.string.help_tutorial_width_points) }
            item { GuideSection(R.string.mode_help_font_features_title, topPadding = true) }
            item { FontHooksGuideCard() }
            item { GuideCard(R.string.help_tutorial_typeface_title, null, R.string.help_tutorial_typeface_summary, R.string.help_tutorial_typeface_points) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryPageScaffold(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    SecondaryPageScaffold(
        modifier = modifier,
        onBack = onBack,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryPageScaffold(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The header owns status bars. Keep the list viewport edge-to-edge and reserve
        // the gesture area inside each scrollable page instead of outside this scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SecondaryPageTopBar(
                onBack = onBack,
                actions = actions,
                scrollBehavior = scrollBehavior,
                title = title
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
        content = { scaffoldPadding ->
            content(secondaryPageContentPadding(scaffoldPadding))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrimaryPageScaffold(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    PrimaryPageScaffold(
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrimaryPageScaffold(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        // The header is a scrim overlay. Content owns its first-item breathing room.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PrimaryPageTopBar(
                actions = actions,
                scrollBehavior = scrollBehavior,
                title = title
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
        content = { scaffoldPadding ->
            content(primaryPageContentPadding(scaffoldPadding))
        }
    )
}

/** Shared Compose treatment for standalone secondary pages. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryPageTopBar(
    @StringRes titleRes: Int,
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    SecondaryPageTopBar(
        onBack = onBack,
        includeHorizontalSafeInsets = includeHorizontalSafeInsets,
        actions = actions,
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@Composable
internal fun SecondaryPageTopBar(
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit
) {
    PageTopBar(
        onBack = onBack?.let { action -> rememberDpisConfirmAction(action) },
        actions = actions,
        includeHorizontalSafeInsets = includeHorizontalSafeInsets,
        scrollBehavior = scrollBehavior,
        title = title
    )
}

@Composable
private fun PrimaryPageTopBar(
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit
) {
    PageTopBar(
        actions = actions,
        includeHorizontalSafeInsets = false,
        scrollBehavior = scrollBehavior,
        title = title
    )
}

@Composable
private fun PageTopBar(
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    includeHorizontalSafeInsets: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    title: @Composable () -> Unit
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            if (onBack != null) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(onClick = onBack, role = Role.Button),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24),
                        contentDescription = stringResource(R.string.system_settings_back),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = actions,
        windowInsets = if (includeHorizontalSafeInsets) {
            WindowInsets.statusBars.union(WindowInsets.displayCutout)
        } else {
            WindowInsets.statusBars
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun secondaryPageContentPadding(scaffoldPadding: PaddingValues): PaddingValues {
    return pageContentPadding(scaffoldPadding)
}

@Composable
private fun primaryPageContentPadding(scaffoldPadding: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = scaffoldPadding.calculateStartPadding(layoutDirection),
        top = scaffoldPadding.calculateTopPadding(),
        end = scaffoldPadding.calculateEndPadding(layoutDirection),
        bottom = scaffoldPadding.calculateBottomPadding(),
    )
}

/** Keeps standard page headers readable while allowing their content to pass beneath them. */
@Composable
private fun pageContentPadding(scaffoldPadding: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    return PaddingValues(
        start = scaffoldPadding.calculateStartPadding(layoutDirection) +
            safeDrawingPadding.calculateStartPadding(layoutDirection),
        top = scaffoldPadding.calculateTopPadding(),
        end = scaffoldPadding.calculateEndPadding(layoutDirection) +
            safeDrawingPadding.calculateEndPadding(layoutDirection),
        bottom = scaffoldPadding.calculateBottomPadding(),
    )
}

@Composable
private fun SupportCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun DonationQrCard(@DrawableRes imageRes: Int, @StringRes titleRes: Int, @StringRes descriptionRes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Box {
            Image(
                painter = painterResource(imageRes),
                contentDescription = stringResource(descriptionRes),
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            )
            Text(
                stringResource(titleRes),
                modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SupportersSheet() {
    val supporters = listOf(
        R.string.donate_supporter_nickyoung_name to R.string.donate_supporter_nickyoung_amount,
        R.string.donate_supporter_tadow_name to R.string.donate_supporter_tadow_amount,
        R.string.donate_supporter_han_name to R.string.donate_supporter_han_amount,
        R.string.donate_supporter_spine_name to R.string.donate_supporter_spine_amount,
        R.string.donate_supporter_anonymous_name to R.string.donate_supporter_anonymous_amount
    )
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 512.dp)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(stringResource(R.string.donate_supporters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.donate_supporters_summary), modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        supporters.forEachIndexed { index, (nameRes, amountRes) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(nameRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(amountRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(stringResource(R.string.donate_supporters_sheet_note), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuideSection(@StringRes titleRes: Int, topPadding: Boolean = false) {
    Text(stringResource(titleRes), modifier = if (topPadding) Modifier.padding(top = 6.dp) else Modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun GuideCard(@StringRes titleRes: Int, @StringRes badgeRes: Int?, @StringRes summaryRes: Int, @StringRes pointsRes: Int) {
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(titleRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            badgeRes?.let {
                Text(stringResource(it), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Text(stringResource(summaryRes), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(pointsRes), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FontHooksGuideCard() {
    val routes = listOf(
        R.string.help_tutorial_font_hook_resources_title to R.string.help_tutorial_font_hook_resources_desc,
        R.string.help_tutorial_font_hook_textview_sp_title to R.string.help_tutorial_font_hook_textview_sp_desc,
        R.string.help_tutorial_font_hook_textview_absolute_title to R.string.help_tutorial_font_hook_textview_absolute_desc,
        R.string.help_tutorial_font_hook_textview_current_title to R.string.help_tutorial_font_hook_textview_current_desc,
        R.string.help_tutorial_font_hook_paint_title to R.string.help_tutorial_font_hook_paint_desc,
        R.string.help_tutorial_font_hook_webview_title to R.string.help_tutorial_font_hook_webview_desc,
        R.string.help_tutorial_font_hook_flutter_title to R.string.help_tutorial_font_hook_flutter_desc,
        R.string.help_tutorial_font_hook_hyperos_title to R.string.help_tutorial_font_hook_hyperos_desc
    )
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.help_tutorial_font_hooks_title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.help_tutorial_font_hooks_badge), modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.help_tutorial_font_hooks_summary), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        routes.forEach { (titleRes, descriptionRes) ->
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(descriptionRes), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
