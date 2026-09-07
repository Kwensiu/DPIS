package com.dpis.module.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import com.dpis.module.R

/**
 * Applies the small amount of shared workspace chrome that is unique to compact watches.
 *
 * Page content continues to own its own layout. This binder only centers the two
 * standalone workspace headings, which otherwise look visually offset inside a round window.
 */
object WatchWorkspaceChromeBinder {
    @JvmStatic
    fun applyIfSupported(context: Context?, settingsWorkspace: View?) {
        if (!WatchUiMode.shouldUseCompactUi(context)) {
            return
        }
        centerTitle(settingsWorkspace, R.id.settings_workspace_title)
    }

    /** Keeps the shared app/template search toolbar inside a compact round display's safe area.  */
    @JvmStatic
    fun applyTopContainerInsets(topContainer: View?) {
        val compactWatch = topContainer != null
                && WatchUiMode.shouldUseCompactUi(topContainer.context)
        WindowInsetsBinder.applySafeDrawingPadding(
            topContainer,
            compactWatch,
            true,
            compactWatch,
            false
        )
    }

    private fun centerTitle(workspace: View?, @IdRes titleId: Int) {
        if (workspace == null) {
            return
        }
        val title = workspace.findViewById<TextView?>(titleId) ?: return
        title.gravity = Gravity.CENTER
        val params = title.layoutParams
        if (params != null && params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            title.layoutParams = params
        }
    }
}
