package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dpis.module.R

/** Shared portrait editor frame. Editor-specific fields and actions stay with each workflow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpisEditorBottomSheet(
    onDismissRequest: () -> Unit,
    skipPartiallyExpanded: Boolean = true,
    topChrome: @Composable () -> Unit = { DpisSheetVisualChrome() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        androidx.compose.material3.BottomSheetDefaults.modalWindowInsets
    },
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = if (skipPartiallyExpanded) {
            setOf(SheetValue.Hidden, SheetValue.Expanded)
        } else {
            setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded)
        }
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        contentWindowInsets = contentWindowInsets,
        // The visual white line is content, not a Material drag-handle semantic.
        dragHandle = null
    ) {
        topChrome()
        content()
    }
}

@Composable
fun DpisSheetVisualChrome(showUnsaved: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppConfigSheetUiTokens.TopChromeHeight),
        contentAlignment = Alignment.Center
    ) {
        if (showUnsaved) {
            androidx.compose.material3.Surface(
                shape = AppConfigSheetUiTokens.UnsavedBadgeShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                androidx.compose.material3.Text(
                    stringResource(R.string.sheet_unsaved_badge),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(
                        width = AppConfigSheetUiTokens.TopChromeIndicatorWidth,
                        height = AppConfigSheetUiTokens.TopChromeIndicatorHeight
                    )
                    .offset(y = 0.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
