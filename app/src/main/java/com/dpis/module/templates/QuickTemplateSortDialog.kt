package com.dpis.module.templates

import android.app.Activity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.ui.compose.ConfirmDialogUiTokens
import com.dpis.module.ui.compose.DialogColumn
import com.dpis.module.ui.compose.DialogTitle
import com.dpis.module.ui.compose.DpisTheme
import com.dpis.module.ui.compose.dpisDarkTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Compose-owned template ordering dialog; persistence remains in the Java host. */
object QuickTemplateSortDialog {
    // TODO: Migrate after Java callers stop retaining the AlertDialog for imperative dismissal.
    interface Host {
        /** Persists the current order. Returning false leaves the dialog at the previous order. */
        fun onOrderChanged(orderedIds: List<String>): Boolean
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
                    onOrderChanged = { orderedIds ->
                        val currentHost = host
                        if (currentHost == null) {
                            true
                        } else {
                            currentHost.onOrderChanged(orderedIds).also { saved ->
                                if (!saved) currentHost.showToast(R.string.quick_template_sort_failed)
                            }
                        }
                    },
                    onDone = dialog::dismiss
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
    onOrderChanged: (List<String>) -> Boolean,
    onDone: () -> Unit
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
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(ConfirmDialogUiTokens.ActionHeight),
                    shape = ConfirmDialogUiTokens.ActionShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.quick_template_sort_done))
                }
            }
        }
    ) {
        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = orderedItems.indexOfFirst { it.id == from.key }
            val toIndex = orderedItems.indexOfFirst { it.id == to.key }
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                val moved = orderedItems.removeAt(fromIndex)
                orderedItems.add(toIndex, moved)
                if (!onOrderChanged(orderedItems.map(QuickTemplateSortItem::id))) {
                    orderedItems.removeAt(toIndex)
                    orderedItems.add(fromIndex, moved)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(orderedItems, key = QuickTemplateSortItem::id) { item ->
                ReorderableItem(reorderableState, key = item.id) { isDragging ->
                    QuickTemplateSortRow(item, isDragging, with(this) { Modifier.longPressDraggableHandle() })
                }
            }
        }
    }
}

@Composable
private fun QuickTemplateSortRow(
    item: QuickTemplateSortItem,
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .shadow(if (isDragging) 12.dp else 0.dp, RoundedCornerShape(8.dp)),
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
            onOrderChanged = { true },
            onDone = {}
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
            onOrderChanged = { true },
            onDone = {}
        )
    }
}
