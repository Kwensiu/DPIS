package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** Shared portrait editor frame. Editor-specific fields and actions stay with each workflow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpisEditorBottomSheet(
    onDismissRequest: () -> Unit,
    skipPartiallyExpanded: Boolean = true,
    topChrome: @Composable () -> Unit = { DpisSheetVisualChrome() },
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        // The visual white line is content, not a Material drag-handle semantic.
        dragHandle = null
    ) {
        topChrome()
        content()
    }
}

@Composable
fun DpisSheetVisualChrome() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TemplateUiTokens.SheetTopChromeHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = TemplateUiTokens.SheetVisualIndicatorWidth,
                    height = TemplateUiTokens.SheetVisualIndicatorHeight
                )
                .offset(y = TemplateUiTokens.SheetVisualIndicatorOffset)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
