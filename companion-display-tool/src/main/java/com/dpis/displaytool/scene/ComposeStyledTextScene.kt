package com.dpis.displaytool.scene

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LocalDisplayToolTextStyle = staticCompositionLocalOf { TextStyle.Default }

class ComposeStyledTextScene : DisplayScene {
    override fun id(): String = "compose_styled_text"

    override fun supportsVariant(variant: String): Boolean {
        return supportsComposeVariant(variant)
    }

    override fun create(runtime: SceneRuntime, variant: String): ScenePresentation {
        val probe = ComposeTextLayoutProbe(styleSource = "local_text_style")
        val view = ComposeView(runtime.activity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                StyledText(probe)
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
private fun StyledText(probe: ComposeTextLayoutProbe) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDisplayToolTextStyle provides TextStyle(fontSize = COMPOSE_BASE_SP.sp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BasicText(
                text = "Compose styled text",
                style = LocalDisplayToolTextStyle.current,
                onTextLayout = { result ->
                    probe.capture(density, COMPOSE_BASE_SP, result)
                }
            )
        }
    }
}
