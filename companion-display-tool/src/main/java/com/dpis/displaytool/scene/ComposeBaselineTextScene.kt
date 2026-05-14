package com.dpis.displaytool.scene

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ComposeBaselineTextScene : DisplayScene {
    override fun id(): String = "compose_baseline_text"

    override fun supportsVariant(variant: String): Boolean {
        return supportsComposeVariant(variant)
    }

    override fun create(runtime: SceneRuntime, variant: String): ScenePresentation {
        val probe = ComposeTextLayoutProbe(styleSource = "baseline")
        val view = ComposeView(runtime.activity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                BaselineText(probe)
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
private fun BaselineText(probe: ComposeTextLayoutProbe) {
    val density = LocalDensity.current
    BasicText(
        text = "Compose baseline text",
        modifier = Modifier.padding(16.dp),
        style = TextStyle(fontSize = COMPOSE_BASE_SP.sp),
        onTextLayout = { result ->
            probe.capture(density, COMPOSE_BASE_SP, result)
        }
    )
}
