package com.dpis.displaytool.scene

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ComposeLazyListTextScene : DisplayScene {
    override fun id(): String = "compose_lazy_list_text"

    override fun supportsVariant(variant: String): Boolean {
        return supportsComposeVariant(variant)
    }

    override fun create(runtime: SceneRuntime, variant: String): ScenePresentation {
        var firstVisibleIndex = 0
        val probe = ComposeTextLayoutProbe(
            itemIndex = 0,
            lazyFirstVisibleIndex = { firstVisibleIndex },
            readyAfterNextFrame = true
        )
        val view = ComposeView(runtime.activity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                LazyListText(
                    probe = probe,
                    onFirstVisibleIndex = { index ->
                        firstVisibleIndex = index
                    }
                )
            }
        }
        return ScenePresentation.composeView(
            view,
            COMPOSE_BASE_SP,
            COMPOSE_VIEW_PRIMARY,
            EVENT_COMPOSE_LAZY_FIRST_SCREEN_STABLE,
            probe
        )
    }
}

@Composable
private fun LazyListText(
    probe: ComposeTextLayoutProbe,
    onFirstVisibleIndex: (Int) -> Unit
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    SideEffect {
        onFirstVisibleIndex(listState.firstVisibleItemIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(8) { index ->
            BasicText(
                text = "Compose lazy list item $index",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = TextStyle(fontSize = COMPOSE_BASE_SP.sp),
                onTextLayout = { result ->
                    if (index == 0) {
                        probe.capture(density, COMPOSE_BASE_SP, result)
                    }
                }
            )
        }
    }
}
