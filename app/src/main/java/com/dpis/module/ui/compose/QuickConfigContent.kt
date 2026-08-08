package com.dpis.module.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.AppConfigEditorPresentation
import com.dpis.module.ConfigEditorDestination

class QuickConfigPresentation {
    var state: AppConfigEditorPresentation.State? by mutableStateOf(null)
        private set

    fun show(state: AppConfigEditorPresentation.State) {
        this.state = state
    }
}

/** Bottom-anchored editor surface for the translucent Quick Settings Activity. */
@Composable
fun QuickConfigContent(
    presentation: QuickConfigPresentation,
    onDismiss: () -> Unit
) {
    val state = presentation.state ?: return
    BackHandler(onBack = onDismiss)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = null,
            indication = null,
            onClick = onDismiss
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.94f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Keep the same visual-only sheet indicator and header spacing as the main editor.
                DpisSheetVisualChrome()
                if (state.destination == ConfigEditorDestination.MAIN) {
                    AppConfigEditorContent(state = state)
                } else {
                    AppHookChainEditorPage(state = state)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0x66000000)
@Composable
private fun QuickConfigContentPreview() {
    DpisTheme(darkTheme = false) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {}
    }
}
