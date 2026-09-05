package com.dpis.module.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import com.dpis.module.LocalizedActivity
import com.dpis.module.LogActivity
import com.dpis.module.diagnostics.LogGate
import com.dpis.module.ui.TouchFeedbackBinder
import com.dpis.module.ui.WindowInsetsBinder

/** Owns the tools page's legacy binding, Compose state, and platform callbacks. */
class ToolsWorkspace(
    private val activity: LocalizedActivity,
    private val onComposeStateChanged: Runnable,
    private val onWriteFailed: Runnable,
) {
    private val binder = ToolsWorkspaceBinder(object : ToolsWorkspaceBinder.Host {
        override fun activity(): Activity = this@ToolsWorkspace.activity

        override fun applyToolsToolbarInsets(toolbar: View?) {
            WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false)
        }

        override fun bindPressHaptic(view: View?) {
            TouchFeedbackBinder.bindPressHaptic(view)
        }

        override fun openLogsWhenDiagnosticLogsEnabled() {
            val openLogs = Runnable { activity.startActivity(Intent(activity, LogActivity::class.java)) }
            if (LogGate.ensureEnabled(activity, openLogs, null)) {
                openLogs.run()
            }
        }
    })

    private val presenter = SystemFontScaleToolPresenter(
        activity,
        object : SystemFontScaleToolPresenter.Listener {
            override fun onStateChanged(state: SystemFontScaleToolState?) {
                onComposeStateChanged.run()
            }

            override fun onWriteFailed() {
                onWriteFailed.run()
            }
        },
    )

    fun state(): SystemFontScaleToolState? = presenter.state()

    fun changePending(percent: Int) {
        presenter.selectPendingPercent(percent)
    }

    fun apply() {
        presenter.apply()
    }

    fun restore() {
        presenter.restoreDefault()
    }

    fun requestPermission() {
        activity.startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${activity.packageName}"))
        )
    }

    fun bind(root: View?) {
        binder.bind(root)
    }

    fun onStart() {
        presenter.refresh()
        binder.onStart()
    }

    fun onResume() {
        presenter.refresh()
        binder.onResume()
    }

    fun onStop() {
        binder.onStop()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter.refresh()
        binder.onActivityResult(requestCode, resultCode, data)
    }

    fun onShown() {
        binder.onShown()
    }
}
