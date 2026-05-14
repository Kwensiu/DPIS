package com.dpis.displaytool.scene

import android.view.Choreographer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.dpis.displaytool.CompanionContract
import com.dpis.displaytool.ComposeRunFields

internal const val COMPOSE_BASE_SP = 14f
internal const val COMPOSE_VIEW_PRIMARY = "compose_text_primary"
internal const val EVENT_COMPOSE_FIRST_TEXT_LAYOUT = "compose_first_text_layout"
internal const val EVENT_COMPOSE_LAZY_FIRST_SCREEN_STABLE = "compose_lazy_first_screen_stable"

internal fun supportsComposeVariant(variant: String): Boolean {
    return variant == CompanionContract.VARIANT_NORMAL
}

internal fun runAfterNextFrame(block: () -> Unit) {
    Choreographer.getInstance().postFrameCallback { block() }
}

internal class ComposeTextLayoutProbe(
    private val styleSource: String? = null,
    private val container: String? = null,
    private val itemIndex: Int = -1,
    private val lazyFirstVisibleIndex: (() -> Int)? = null,
    private val readyAfterNextFrame: Boolean = false
) : ScenePresentation.ComposeFieldsProvider {
    @Volatile
    private var snapshot: Snapshot? = null

    @Volatile
    private var nextFrameObserved = !readyAfterNextFrame

    private var nextFrameRequested = false

    fun capture(density: Density, textSp: Float, result: TextLayoutResult) {
        snapshot = Snapshot(
            composeDensity = density.density,
            composeFontScale = density.fontScale,
            composeTextSp = textSp,
            composeTextPx = with(density) { textSp.sp.toPx() },
            composeLineCount = result.lineCount,
            composeLayoutW = result.size.width,
            composeLayoutH = result.size.height
        )
        if (readyAfterNextFrame && !nextFrameRequested) {
            nextFrameRequested = true
            runAfterNextFrame {
                nextFrameObserved = true
            }
        }
    }

    override fun isReady(): Boolean {
        if (snapshot == null || !nextFrameObserved) {
            return false
        }
        val lazyIndex = lazyFirstVisibleIndex?.invoke()
        return lazyIndex == null || lazyIndex == 0
    }

    override fun fields(androidScaledDensity: Float): ComposeRunFields {
        val current = snapshot ?: Snapshot.empty()
        val denominator = current.composeTextSp * androidScaledDensity
        val renderedScale = if (denominator > 0f) current.composeTextPx / denominator else 0f
        return ComposeRunFields(
            current.composeDensity,
            current.composeFontScale,
            current.composeTextSp,
            current.composeTextPx,
            current.composeLineCount,
            current.composeLayoutW,
            current.composeLayoutH,
            renderedScale,
            itemIndex,
            lazyFirstVisibleIndex?.invoke() ?: -1,
            styleSource,
            container
        )
    }

    private data class Snapshot(
        val composeDensity: Float,
        val composeFontScale: Float,
        val composeTextSp: Float,
        val composeTextPx: Float,
        val composeLineCount: Int,
        val composeLayoutW: Int,
        val composeLayoutH: Int
    ) {
        companion object {
            fun empty(): Snapshot {
                return Snapshot(0f, 0f, COMPOSE_BASE_SP, 0f, 0, 0, 0)
            }
        }
    }
}
