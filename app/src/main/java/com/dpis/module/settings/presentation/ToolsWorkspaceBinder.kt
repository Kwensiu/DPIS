package com.dpis.module.settings.presentation

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.dpis.module.R
import com.dpis.module.ui.WatchUiMode

class ToolsWorkspaceBinder(private val host: Host) {
    interface Host {
        fun activity(): Activity
        fun applyToolsToolbarInsets(toolbar: View?)
        fun bindPressHaptic(view: View?)
        fun openLogsWhenDiagnosticLogsEnabled()
    }

    private var fontScaleToolBinder: SystemFontScaleToolBinder? = null

    fun bind(workspaceView: View?) {
        if (workspaceView == null || fontScaleToolBinder != null) return
        val toolsToolbar = workspaceView.findViewById<View>(R.id.tools_toolbar)
        host.applyToolsToolbarInsets(toolsToolbar)
        if (WatchUiMode.shouldUseCompactUi(host.activity()) && toolsToolbar is LinearLayout) {
            toolsToolbar.gravity = Gravity.CENTER
        }
        bindLogEntry(workspaceView)
        fontScaleToolBinder = SystemFontScaleToolBinder(host.activity(), workspaceView, host).also {
            it.bind()
        }
    }

    private fun bindLogEntry(workspaceView: View) {
        workspaceView.findViewById<View>(R.id.tools_log_card)?.setOnClickListener {
            host.openLogsWhenDiagnosticLogsEnabled()
        }
    }

    fun onStart() {
        fontScaleToolBinder?.refreshFromSystem()
    }

    fun onResume() {
        fontScaleToolBinder?.refreshFromSystem()
    }

    fun onStop() = Unit

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fontScaleToolBinder?.refreshFromSystem()
    }

    fun onShown() {
        fontScaleToolBinder?.collapseAndRefreshFromSystem()
    }
}
