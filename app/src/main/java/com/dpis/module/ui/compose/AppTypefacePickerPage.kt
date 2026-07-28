package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.SystemFontRegistry

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
    var selectedTab by remember(selectedTypefaceId) {
        mutableIntStateOf(
            if (!selectedTypefaceId.isNullOrBlank()
                    && !SystemFontRegistry.isSystemFontId(selectedTypefaceId)) 1 else 0
        )
    }
    val systemEntries = remember {
        SystemFontRegistry.listRecommendedFonts().map {
            TypefaceOption(it.id(), it.displayName(), SystemFontRegistry.loadTypeface(it.id()))
        }
    }
    val importedEntries = remember {
        val store = ConfigStoreFactory.createLocalUiFontLibraryStore(context, null)
        store.listFonts().map { entry ->
            TypefaceOption(
                entry.id,
                entry.displayName,
                store.resolveFontFile(entry.id)?.let { file ->
                    com.dpis.module.fonts.FontTypefaceLoader.load(file, entry.ttcIndex)
                }
            )
        }
    }
    Column(
        modifier.fillMaxWidth().padding(
            start = TypefacePickerUiTokens.TypefaceSurfacePadding,
            top = 8.dp,
            end = TypefacePickerUiTokens.TypefaceSurfacePadding,
            bottom = 16.dp
        )
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back_24),
                    contentDescription = stringResource(R.string.system_settings_back)
                )
            }
            Text(
                stringResource(R.string.dialog_typeface_dialog_title),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge
            )
        }
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.dialog_typeface_tab_system)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.dialog_typeface_tab_imported)) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                val listHeight = typefaceListHeight()
                if (selectedTab == 0) {
                    TypefaceOptionList(
                        options = listOf(TypefaceOption(null, defaultTypefaceLabel, null)) + systemEntries,
                        selectedTypefaceId = selectedTypefaceId,
                        onTypefaceSelected = onTypefaceSelected,
                        modifier = Modifier.height(listHeight)
                    )
                } else if (importedEntries.isEmpty()) {
                    Box(Modifier.height(listHeight)) {
                        ListItem(
                            modifier = Modifier.clickable {
                                onBack()
                                context.startActivity(Intent(context, FontLibraryActivity::class.java))
                            }
                        ) { Text(stringResource(R.string.dialog_typeface_imported_empty)) }
                    }
                } else {
                    TypefaceOptionList(
                        options = listOf(TypefaceOption(null, defaultTypefaceLabel, null)) + importedEntries,
                        selectedTypefaceId = selectedTypefaceId,
                        onTypefaceSelected = onTypefaceSelected,
                        modifier = Modifier.height(listHeight)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = TypefacePickerUiTokens.FooterTopGap),
                    horizontalArrangement = Arrangement.spacedBy(TypefacePickerUiTokens.FooterButtonGap)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(TypefacePickerUiTokens.FooterButtonHeight),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) { Text(stringResource(R.string.dialog_typeface_done_action)) }
                    OutlinedButton(
                        onClick = {
                            onBack()
                            context.startActivity(Intent(context, FontLibraryActivity::class.java))
                        },
                        modifier = Modifier.height(TypefacePickerUiTokens.FooterButtonHeight),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) { Text(stringResource(R.string.dialog_typeface_manage_action)) }
                }
    }
}

@Composable
private fun TypefaceOptionList(
    options: List<TypefaceOption>,
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxWidth()) {
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
                        MaterialTheme.colorScheme.surfaceVariant
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

@Composable
private fun typefaceListHeight(): androidx.compose.ui.unit.Dp {
    // The legacy dialog reduced its fixed 360dp list on short landscape windows so the footer
    // remained visible. Keep that contract while reserving space for the tab row and actions.
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return (screenHeight * 0.82f - 220.dp).coerceIn(120.dp, TypefacePickerUiTokens.TypefaceListHeight)
}
