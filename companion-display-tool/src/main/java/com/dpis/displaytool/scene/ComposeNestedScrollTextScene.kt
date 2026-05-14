package com.dpis.displaytool.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ComposeNestedScrollTextScene : DisplayScene {
    override fun id(): String = "compose_nested_scroll_text"

    override fun supportsVariant(variant: String): Boolean {
        return supportsComposeVariant(variant)
    }

    override fun create(runtime: SceneRuntime, variant: String): ScenePresentation {
        val probe = ComposeTextLayoutProbe(container = "vertical_scroll")
        val view = ComposeView(runtime.activity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                NestedScrollText(probe)
            }
        }
        return ScenePresentation.composeView(
            view,
            COMPOSE_BASE_SP,
            COMPOSE_VIEW_PRIMARY,
            EVENT_COMPOSE_FIRST_TEXT_LAYOUT,
            probe
        )
    }
}

@Composable
private fun NestedScrollText(probe: ComposeTextLayoutProbe) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = "Compose nested scroll text",
                    style = TextStyle(fontSize = COMPOSE_BASE_SP.sp),
                    onTextLayout = { result ->
                        probe.capture(density, COMPOSE_BASE_SP, result)
                    }
                )
            }
        }
    }
}
