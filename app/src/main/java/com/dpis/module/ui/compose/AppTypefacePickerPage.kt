package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.TypefaceCatalogCache
import com.dpis.module.fonts.SystemFontRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val TypefacePickerPagerTestTag = "typeface-picker-pager"
internal const val TypefacePickerManageTestTag = "typeface-picker-manage"
internal const val TypefacePickerTabRowTestTag = "typeface-picker-tab-row"
internal const val TypefacePickerSystemListTestTag = "typeface-picker-system-list"

/** Typeface selection rendered as a child page inside an existing configuration editor. */
@Composable
internal fun AppTypefacePickerPage(
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    TypefacePickerContent(
        selectedTypefaceId = selectedTypefaceId,
        onTypefaceSelected = onTypefaceSelected,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun TypefacePickerContent(
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultTypefaceLabel = stringResource(R.string.dialog_typeface_default)
    // Match the legacy editor: an already selected imported font opens its own catalogue.
    val initialPage = if (!selectedTypefaceId.isNullOrBlank()
            && !SystemFontRegistry.isSystemFontId(selectedTypefaceId)) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val pagerScope = rememberCoroutineScope()
    val catalog by produceState<TypefaceCatalogCache.Catalog?>(
        initialValue = TypefaceCatalogCache.cached(),
        key1 = context.applicationContext
    ) {
        value = withContext(Dispatchers.IO) {
            TypefaceCatalogCache.get(context.applicationContext)
        }
    }
    val systemEntries = catalog?.systemEntries.orEmpty()
    val importedEntries = catalog?.importedEntries.orEmpty()
    Column(
        modifier
            // Keep the typeface page at the same bounded height as the hook-chain page. The
            // list owns scrolling while the management action remains in the fixed footer.
            .fillMaxSize()
            .padding(
            top = 8.dp,
            bottom = 16.dp
        )
    ) {
        EditorSheetChildPageHeader(
            title = stringResource(R.string.dialog_typeface_dialog_title),
            onBack = onBack,
        )
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(TypefacePickerTabRowTestTag),
        ) {
            listOf(
                R.string.dialog_typeface_tab_system,
                R.string.dialog_typeface_tab_imported
            ).forEachIndexed { page, titleRes ->
                Tab(
                    selected = pagerState.currentPage == page,
                    onClick = {
                        pagerScope.launch { pagerState.animateScrollToPage(page) }
                    },
                    text = { Text(stringResource(titleRes)) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(TypefacePickerPagerTestTag),
            pageSpacing = TypefacePickerUiTokens.PageSpacing,
            // A page snap must end at rest. Android 12's stretch overscroll otherwise treats
            // residual horizontal fling as an edge pull and distorts the font cards on release.
            overscrollEffect = null,
            verticalAlignment = Alignment.Top
        ) { page ->
            if (catalog == null) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (page == 0) {
                TypefaceOptionList(
                    options = listOf(TypefaceOption(null, defaultTypefaceLabel, null)) +
                        systemEntries.map { TypefaceOption(it.id, it.displayName, it.preview) },
                    selectedTypefaceId = selectedTypefaceId,
                    onTypefaceSelected = onTypefaceSelected,
                    modifier = Modifier.fillMaxSize().padding(
                        horizontal = TypefacePickerUiTokens.HorizontalContentPadding
                    )
                        .testTag(TypefacePickerSystemListTestTag)
                )
            } else if (importedEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = TypefacePickerUiTokens.HorizontalContentPadding,
                            vertical = TypefacePickerUiTokens.TypefaceOptionRowPadding,
                        )
                        .height(TypefacePickerUiTokens.TypefaceOptionHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.dialog_typeface_imported_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                TypefaceOptionList(
                    options = listOf(TypefaceOption(null, defaultTypefaceLabel, null)) +
                        importedEntries.map { TypefaceOption(it.id, it.displayName, it.preview) },
                    selectedTypefaceId = selectedTypefaceId,
                    onTypefaceSelected = onTypefaceSelected,
                    modifier = Modifier.fillMaxSize().padding(
                        horizontal = TypefacePickerUiTokens.HorizontalContentPadding
                    )
                )
            }
        }
        Spacer(Modifier.height(TypefacePickerUiTokens.FooterTopGap))
        OutlinedButton(
            onClick = {
                onBack()
                context.startActivity(Intent(context, FontLibraryActivity::class.java))
            },
            modifier = Modifier.padding(
                    horizontal = TypefacePickerUiTokens.HorizontalContentPadding
                )
                .fillMaxWidth()
                .height(TypefacePickerUiTokens.FooterButtonHeight)
                .testTag(TypefacePickerManageTestTag),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 0.dp
            )
        ) { Text(stringResource(R.string.dialog_typeface_manage_action)) }
    }
}

@Composable
private fun TypefaceOptionList(
    options: List<TypefaceOption>,
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val listScrollConnection = androidx.compose.runtime.remember(listState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Once the list reaches its top, leave a downward drag for the parent sheet.
                // While rows can still consume it, keep the gesture inside the list.
                return if (available.y > 0f && !listState.canScrollBackward) {
                    Offset.Zero
                } else {
                    available
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (available.y > 0f && !listState.canScrollBackward) {
                    Velocity.Zero
                } else {
                    available
                }
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth()
            .nestedScroll(listScrollConnection)
            .dialogListContentFade(
            state = listState,
            edgeColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp),
    ) {
        items(options, key = { it.id ?: "default" }) { option ->
            Button(
                onClick = { onTypefaceSelected(option.id) },
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = TypefacePickerUiTokens.TypefaceOptionRowPadding)
                    .height(TypefacePickerUiTokens.TypefaceOptionHeight),
                shape = TypefacePickerUiTokens.TypefaceOptionShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (option.id == selectedTypefaceId) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceBright
                    },
                    contentColor = if (option.id == selectedTypefaceId) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = TypefacePickerUiTokens.TypefaceOptionPadding,
                    vertical = 0.dp
                )
            ) {
                Text(
                    option.label,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = option.preview?.let(::FontFamily),
                    maxLines = 1
                )
            }
        }
    }
}

private data class TypefaceOption(
    val id: String?,
    val label: String,
    val preview: android.graphics.Typeface?
)
