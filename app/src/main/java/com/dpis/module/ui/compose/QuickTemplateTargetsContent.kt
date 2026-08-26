package com.dpis.module.ui.compose

import android.widget.ImageView

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import com.dpis.module.R
import com.dpis.module.templates.QuickTemplateTargetsPresentationController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTemplateTargetsContent(
    state: QuickTemplateTargetsPresentationController.State?,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onFiltersChanged: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onSelectionChanged: (String, Boolean) -> Unit,
    onSaveAndExit: () -> Boolean,
    showBackButton: Boolean = true,
    handleSystemBack: Boolean = false
) {
    val current = state ?: return
    val focusManager = LocalFocusManager.current
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    var discardDialogVisible by rememberSaveable { mutableStateOf(false) }
    val searchOffset by animateDpAsState(
        targetValue = if (searchVisible) 76.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "target search offset"
    )
    val requestBack = {
        focusManager.clearFocus()
        if (current.hasUnsavedChanges) discardDialogVisible = true else onBack()
    }
    if (handleSystemBack) {
        BackHandler(onBack = requestBack)
    }
    val listState = rememberLazyListState()

    // This picker is also embedded in the landscape workspace. Keep the shared top bar as
    // an in-flow sibling instead of Scaffold.topBar, whose overlay slot clips scrolled rows.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {},
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSaveAndExit()
                    },
                    enabled = !current.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .then(if (showBackButton) Modifier.navigationBarsPadding() else Modifier)
                        .height(48.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(stringResource(R.string.status_save_button))
                }
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(scaffoldPadding)
        ) {
            Box(Modifier.zIndex(1f)) {
                SecondaryPageTopBar(
                    onBack = requestBack.takeIf { showBackButton },
                    includeHorizontalSafeInsets = showBackButton,
                    title = {
                    Column {
                        Text(
                            text = current.templateName.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.quick_template_targets_selected_count,
                                current.selectedCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    },
                    actions = {
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) focusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search_24),
                            contentDescription = stringResource(R.string.quick_search_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            filterSheetVisible = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune_24),
                            contentDescription = stringResource(R.string.filter_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clearTextInputFocusOnPointerDown(focusManager)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        when {
                            current.loading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(R.string.quick_template_targets_loading),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            current.apps.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(R.string.quick_template_targets_empty),
                                        modifier = Modifier.padding(24.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .dialogListContentFade(
                                            state = listState,
                                            edgeColor = MaterialTheme.colorScheme.surfaceContainer,
                                            edgeHeight = 4.dp
                                        ),
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        start = 0.dp,
                                        top = searchOffset,
                                        end = 0.dp,
                                        bottom = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(
                                        count = current.apps.size,
                                        key = { current.apps[it].packageName }
                                    ) { index ->
                                        val app = current.apps[index]
                                        TargetAppRow(
                                            app = app,
                                            onSelected = { selected ->
                                                onSelectionChanged(app.packageName, selected)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchVisible,
                        modifier = Modifier.align(Alignment.TopCenter),
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        TargetSearchCard(
                            query = current.query,
                            onQueryChanged = onQueryChanged,
                            onClearQuery = { onQueryChanged("") },
                            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (filterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { filterSheetVisible = false },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            ),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding()
            ) {
                DpisSheetVisualChrome()
                Text(
                    text = stringResource(R.string.filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.quick_template_targets_filter_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = current.showAllApps,
                        onClick = { onFiltersChanged(true, false, false, current.hideConfiguredApps) },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_all)) }
                    )
                    FilterChip(
                        selected = current.showSystemApps,
                        onClick = {
                            onFiltersChanged(false, !current.showSystemApps, current.showUserApps,
                                current.hideConfiguredApps)
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_system)) }
                    )
                    FilterChip(
                        selected = current.showUserApps,
                        onClick = {
                            onFiltersChanged(false, current.showSystemApps, !current.showUserApps,
                                current.hideConfiguredApps)
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_user)) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                TargetFilterSwitch(
                    label = stringResource(R.string.quick_template_targets_filter_hide_configured),
                    checked = current.hideConfiguredApps,
                    onCheckedChange = {
                        onFiltersChanged(current.showAllApps, current.showSystemApps, current.showUserApps, it)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (discardDialogVisible) {
        DpisModalDialog(onDismissRequest = { discardDialogVisible = false }) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.quick_template_targets_unsaved_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.quick_template_targets_unsaved_message),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        discardDialogVisible = false
                        onBack()
                    }) {
                        Text(stringResource(R.string.quick_template_targets_discard_changes))
                    }
                    TextButton(onClick = {
                        if (onSaveAndExit()) discardDialogVisible = false
                    }) {
                        Text(stringResource(R.string.quick_template_targets_save_and_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetSearchCard(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetAppRow(
    app: QuickTemplateTargetsPresentationController.TargetApp,
    onSelected: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = app.selected,
                role = Role.Checkbox,
                onValueChange = onSelected
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TargetAppIcon(app.icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIdentityMarqueeText(
                    text = app.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                if (app.configured) {
                    ConfiguredBadge()
                }
            }
            AppIdentityMarqueeText(
                text = app.packageName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = app.selected,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun TargetAppIcon(icon: android.graphics.drawable.Drawable?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            // Do not leave the loading mask under an application icon once it resolves.
            .then(
                if (icon == null) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
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
                update = { imageView -> imageView.setImageDrawable(icon) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ConfiguredBadge() {
    Surface(
        modifier = Modifier.padding(start = 8.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = stringResource(R.string.quick_template_targets_configured_badge),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun TargetFilterSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
