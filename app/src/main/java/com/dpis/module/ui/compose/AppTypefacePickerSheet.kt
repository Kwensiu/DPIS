package com.dpis.module.ui.compose

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.SystemFontEntry
import com.dpis.module.fonts.SystemFontRegistry

/** Typeface selection for the Compose app editor; the font catalogue remains the shared source. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTypefacePickerSheet(
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    // Match the legacy editor: an already selected imported font opens its own catalogue.
    var selectedTab by remember(selectedTypefaceId) {
        mutableIntStateOf(
            if (!selectedTypefaceId.isNullOrBlank()
                    && !SystemFontRegistry.isSystemFontId(selectedTypefaceId)) 1 else 0
        )
    }
    val systemEntries = remember { SystemFontRegistry.listRecommendedFonts() }
    val importedEntries = remember {
        ConfigStoreFactory.createLocalUiFontLibraryStore(context, null).listFonts()
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
                    text = { Text(stringResource(R.string.dialog_typeface_tab_system)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.dialog_typeface_tab_imported)) }
                )
            }
            if (selectedTab == 0) {
                TypefaceOptionList(
                    options = listOf(TypefaceOption(null, context.getString(R.string.dialog_typeface_default)))
                        + systemEntries.map { TypefaceOption(it.id(), it.displayName()) },
                    selectedTypefaceId = selectedTypefaceId,
                    onTypefaceSelected = onTypefaceSelected
                )
            } else if (importedEntries.isEmpty()) {
                ListItem(
                    modifier = Modifier.clickable {
                        // The former dialog closes before the font library opens. Keeping this
                        // sheet alive would retain its imported-font snapshot after returning.
                        onDismissRequest()
                        context.startActivity(Intent(context, FontLibraryActivity::class.java))
                    }
                ) { Text(stringResource(R.string.dialog_typeface_imported_empty)) }
            } else {
                OutlinedButton(
                    onClick = {
                        onDismissRequest()
                        context.startActivity(Intent(context, FontLibraryActivity::class.java))
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.dialog_typeface_manage_action))
                }
                TypefaceOptionList(
                    options = listOf(TypefaceOption(null, context.getString(R.string.dialog_typeface_default)))
                        + importedEntries.map { TypefaceOption(it.id, it.displayName) },
                    selectedTypefaceId = selectedTypefaceId,
                    onTypefaceSelected = onTypefaceSelected
                )
            }
        }
    }
}

@Composable
private fun TypefaceOptionList(
    options: List<TypefaceOption>,
    selectedTypefaceId: String?,
    onTypefaceSelected: (String?) -> Unit
) {
    LazyColumn(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        items(options, key = { it.id ?: "default" }) { option ->
            ListItem(
                supportingContent = if (option.id == selectedTypefaceId) {
                    { Text(stringResource(R.string.dialog_typeface_selector_value)) }
                } else {
                    null
                },
                modifier = Modifier.clickable { onTypefaceSelected(option.id) }
            ) { Text(option.label) }
        }
    }
}

private data class TypefaceOption(val id: String?, val label: String)
