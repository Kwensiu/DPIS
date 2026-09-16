package com.dpis.module.ui.dialog

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.resolveDarkTheme

/**
 * Activity-owned composition host for Java `show()` dialogs. The overlay view is only a
 * composition owner; [ModalDialog] still creates the visible dialog window.
 */
class ComposeOverlay private constructor(
    private val composeView: ComposeView,
) {
    private var dismissed = false
    private var onDismissListener: Runnable? = null

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        (composeView.parent as? ViewGroup)?.removeView(composeView)
        onDismissListener?.run()
    }

    fun isShowing(): Boolean = !dismissed && composeView.parent != null

    fun setOnDismissListener(listener: Runnable?) {
        onDismissListener = listener
    }

    companion object {
        @JvmStatic
        fun show(
            activity: Activity,
            content: @Composable (dismiss: () -> Unit) -> Unit,
        ): ComposeOverlay {
            val parent = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val overlay = ComposeOverlay(composeView)
            parent.addView(composeView, ViewGroup.LayoutParams(1, 1))
            composeView.setContent {
                ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                    content(overlay::dismiss)
                }
            }
            return overlay
        }
    }
}
