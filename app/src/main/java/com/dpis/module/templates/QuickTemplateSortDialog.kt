package com.dpis.module.templates

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.ui.compose.DpisConfirmDialogUiTokens
import com.dpis.module.ui.compose.DialogColumn
import com.dpis.module.ui.compose.DialogTitle
import com.dpis.module.ui.compose.dialogListContentFade
import com.dpis.module.ui.compose.DpisTheme
import com.dpis.module.ui.compose.dpisDarkTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs
import kotlin.math.sign

/** Compose-owned template ordering dialog; persistence remains in the Java host. */
object QuickTemplateSortDialog {
    // TODO: Migrate after Java callers stop retaining the AlertDialog for imperative dismissal.
    interface Host {
        fun saveOrder(orderedIds: List<String>): Boolean
        fun onOrderSaved()
        fun showToast(@StringRes messageResId: Int)
    }

    @JvmStatic
    fun show(
        activity: Activity?,
        templates: List<QuickTemplateStore.QuickTemplate>?,
        host: Host?
    ) {
        if (activity == null || templates.isNullOrEmpty()) return

        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(composeView)
            .create()
        composeView.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                QuickTemplateSortContent(
                    initialItems = templates.map { QuickTemplateSortItem(it.id, it.name) },
                    onCancel = dialog::dismiss,
                    onSave = { orderedIds ->
                        if (host == null || host.saveOrder(orderedIds)) {
                            dialog.dismiss()
                            host?.onOrderSaved()
                        } else {
                            host.showToast(R.string.quick_template_sort_failed)
                        }
                    }
                )
            }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
        DialogWindowSizer.applyLargeWidth(dialog, activity)
    }
}

internal data class QuickTemplateSortItem(val id: String, val name: String)

@Composable
internal fun QuickTemplateSortContent(
    initialItems: List<QuickTemplateSortItem>,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val orderedItems = remember(initialItems) { mutableStateListOf(*initialItems.toTypedArray()) }
    DialogColumn(
        title = { DialogTitle(stringResource(R.string.quick_template_sort_title)) },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    8.dp
                )
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(DpisConfirmDialogUiTokens.ActionHeight),
                    shape = DpisConfirmDialogUiTokens.ActionShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.dialog_process_action_confirm_negative))
                }
                OutlinedButton(
                    onClick = { onSave(orderedItems.map(QuickTemplateSortItem::id)) },
                    modifier = Modifier.weight(1f).height(DpisConfirmDialogUiTokens.ActionHeight),
                    shape = DpisConfirmDialogUiTokens.ActionShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.quick_template_sort_save))
                }
            }
        }
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                .dialogListContentFade(
                    state = listState,
                    edgeColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(orderedItems, key = QuickTemplateSortItem::id) { item ->
                QuickTemplateSortRow(
                    item = item,
                    onMove = { direction ->
                        val from = orderedItems.indexOfFirst { it.id == item.id }
                        val to = (from + direction).coerceIn(0, orderedItems.lastIndex)
                        if (from >= 0 && from != to) {
                            val displaced = orderedItems[to]
                            orderedItems[to] = orderedItems[from]
                            orderedItems[from] = displaced
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickTemplateSortRow(
    item: QuickTemplateSortItem,
    onMove: (Int) -> Unit
) {
    val moveThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    var dragOffset by remember(item.id) { mutableFloatStateOf(0f) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_indicator_24),
                contentDescription = stringResource(R.string.quick_template_sort_drag_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .graphicsLayer { translationY = dragOffset }
                    .pointerInput(item.id, moveThreshold) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = { dragOffset = 0f },
                            onDragCancel = { dragOffset = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            if (abs(dragOffset) >= moveThreshold) {
                                val direction = dragOffset.sign.toInt()
                                onMove(direction)
                                dragOffset -= direction * moveThreshold
                            }
                        }
                    }
            )
            Text(
                text = item.name,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickTemplateSortContentPreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
        QuickTemplateSortContent(
            initialItems = listOf(
                QuickTemplateSortItem("one", "Reading"),
                QuickTemplateSortItem("two", "Compact UI")
            ),
            onCancel = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QuickTemplateSortContentDarkPreview() {
    DpisTheme(darkTheme = true, dynamicColor = false) {
        QuickTemplateSortContent(
            initialItems = listOf(
                QuickTemplateSortItem("one", "Reading"),
                QuickTemplateSortItem("two", "Compact UI")
            ),
            onCancel = {},
            onSave = {}
        )
    }
}
