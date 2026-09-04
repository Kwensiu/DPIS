package com.dpis.module.ui.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.flow.distinctUntilChanged

/** Session positions for stable workspace lists, with Compose saved-state support. */
class PageScrollPositionStore {
    data class Position(val index: Int = 0, val offset: Int = 0)

    private val positions = mutableMapOf<String, Position>()

    fun positionFor(key: String): Position = positions[key] ?: Position()

    fun update(key: String, index: Int, offset: Int) {
        positions[key] = Position(index.coerceAtLeast(0), offset.coerceAtLeast(0))
    }

    fun reset(key: String) {
        positions.remove(key)
    }

    fun topBarCollapsedFor(key: String): Boolean = positions["@topbar:$key"]?.index == 1

    fun updateTopBar(key: String, collapsed: Boolean) {
        positions["@topbar:$key"] = Position(if (collapsed) 1 else 0, 0)
    }

    companion object {
        /**
         * Keeps positions through configuration changes and system task restoration, while a
         * fresh launch still starts with an empty store because no saved instance exists.
         */
        val Saver = listSaver<PageScrollPositionStore, Any>(
            save = { store ->
                store.positions.flatMap { (key, position) ->
                    listOf(key, position.index, position.offset)
                }
            },
            restore = { values ->
                PageScrollPositionStore().also { store ->
                    values.chunked(3).forEach { entry ->
                        val key = entry.getOrNull(0) as? String ?: return@forEach
                        val index = entry.getOrNull(1) as? Int ?: return@forEach
                        val offset = entry.getOrNull(2) as? Int ?: return@forEach
                        store.update(key, index, offset)
                    }
                }
            },
        )
    }
}

@Composable
internal fun rememberRestorableLazyListState(
    key: String,
    store: PageScrollPositionStore,
    enabled: Boolean = true,
): LazyListState {
    val initial = remember(key, store, enabled) {
        if (enabled) store.positionFor(key) else PageScrollPositionStore.Position()
    }
    val state = rememberLazyListState(initial.index, initial.offset)
    LaunchedEffect(key, store, enabled, state) {
        if (!enabled) {
            store.reset(key)
            state.scrollToItem(0)
            return@LaunchedEffect
        }
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> store.update(key, index, offset) }
    }
    return state
}
