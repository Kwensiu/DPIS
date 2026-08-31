package com.dpis.module.templates.presentation

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.zIndex
import com.dpis.module.R
import com.dpis.module.templates.QuickTemplateTargetsPresentationController
import com.dpis.module.ui.compose.AppIdentityMarqueeText
import com.dpis.module.ui.compose.SecondaryPageTopBar
import com.dpis.module.ui.compose.clearTextInputFocusOnPointerDown
import com.dpis.module.ui.compose.dialogListContentFade
import com.dpis.module.ui.dialog.ModalDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTemplateTargetsContent(
    state: QuickTemplateTargetsPresentationController.State?,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onFiltersChanged: (Boolean, Boolean, Boolean, Boolean, Int, Boolean) -> Unit,
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
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.quick_template_targets_empty),
                                        modifier = Modifier.padding(bottom = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!current.missingTemplate && (current.query.isNotBlank() || !current.showAllApps || current.showConfiguredApps || current.sortMode != QuickTemplateTargetsPresentationController.SORT_NAME || current.reverseOrder)) {
                                        Button(onClick = {
                                            onQueryChanged("")
                                            onFiltersChanged(
                                                true,
                                                false,
                                                false,
                                                true,
                                                QuickTemplateTargetsPresentationController.SORT_NAME,
                                                false
                                            )
                                        }, shape = RoundedCornerShape(50)) {
                                            Text(stringResource(R.string.reset_filters_button))
                                        }
                                    }
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
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { filterSheetVisible = false },
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = (-8).dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24),
                            contentDescription = stringResource(R.string.dialog_close),
                        )
                    }
                    Text(
                        text = stringResource(R.string.quick_template_targets_filter_list_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(x = (-4).dp)
                    )
                    Spacer(Modifier.weight(1f))
                    FilterChip(
                        shape = RoundedCornerShape(50),
                        selected = current.reverseOrder,
                        onClick = {
                            onFiltersChanged(
                                current.showAllApps, current.showSystemApps, current.showUserApps,
                                current.showConfiguredApps, current.sortMode, !current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_reverse)) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                RoundedCornerShape(50)
                            )
                            .clickable(role = Role.Button) {
                                onFiltersChanged(
                                    true, false, false, true,
                                    QuickTemplateTargetsPresentationController.SORT_NAME, false
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reset_settings_24),
                            contentDescription = stringResource(R.string.quick_template_targets_filter_reset),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.quick_template_targets_filter_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = current.showAllApps,
                        onClick = {
                            onFiltersChanged(
                                true,
                                false,
                                false,
                                current.showConfiguredApps,
                                current.sortMode,
                                current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_all)) }
                    )
                    FilterChip(
                        selected = current.showSystemApps,
                        onClick = {
                            onFiltersChanged(
                                false, !current.showSystemApps, current.showUserApps,
                                current.showConfiguredApps, current.sortMode, current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_system)) },
                        leadingIcon = if (current.showSystemApps && !current.showAllApps) {
                            {
                                Icon(
                                    painterResource(R.drawable.ic_check_24),
                                    contentDescription = null
                                )
                            }
                        } else null
                    )
                    FilterChip(
                        selected = current.showUserApps,
                        onClick = {
                            onFiltersChanged(
                                false, current.showSystemApps, !current.showUserApps,
                                current.showConfiguredApps, current.sortMode, current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_filter_user)) },
                        leadingIcon = if (current.showUserApps && !current.showAllApps) {
                            {
                                Icon(
                                    painterResource(R.drawable.ic_check_24),
                                    contentDescription = null
                                )
                            }
                        } else null
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    FilterChip(
                        selected = current.showConfiguredApps,
                        onClick = {
                            onFiltersChanged(
                                current.showAllApps,
                                current.showSystemApps,
                                current.showUserApps,
                                !current.showConfiguredApps,
                                current.sortMode,
                                current.reverseOrder
                            )
                        },
                        leadingIcon = if (current.showConfiguredApps) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24),
                                    contentDescription = null
                                )
                            }
                        } else null,
                        label = { Text(stringResource(R.string.quick_template_targets_filter_configured)) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.quick_template_targets_filter_sort),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = current.sortMode == QuickTemplateTargetsPresentationController.SORT_NAME,
                        onClick = {
                            onFiltersChanged(
                                current.showAllApps,
                                current.showSystemApps,
                                current.showUserApps,
                                current.showConfiguredApps,
                                QuickTemplateTargetsPresentationController.SORT_NAME,
                                current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_sort_name)) }
                    )
                    FilterChip(
                        selected = current.sortMode == QuickTemplateTargetsPresentationController.SORT_UPDATED,
                        onClick = {
                            onFiltersChanged(
                                current.showAllApps,
                                current.showSystemApps,
                                current.showUserApps,
                                current.showConfiguredApps,
                                QuickTemplateTargetsPresentationController.SORT_UPDATED,
                                current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_sort_updated)) }
                    )
                    FilterChip(
                        selected = current.sortMode == QuickTemplateTargetsPresentationController.SORT_INSTALLED,
                        onClick = {
                            onFiltersChanged(
                                current.showAllApps,
                                current.showSystemApps,
                                current.showUserApps,
                                current.showConfiguredApps,
                                QuickTemplateTargetsPresentationController.SORT_INSTALLED,
                                current.reverseOrder
                            )
                        },
                        label = { Text(stringResource(R.string.quick_template_targets_sort_installed)) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (discardDialogVisible) {
        ModalDialog(onDismissRequest = { discardDialogVisible = false }) {
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
private fun TargetAppIcon(icon: Drawable?) {
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

