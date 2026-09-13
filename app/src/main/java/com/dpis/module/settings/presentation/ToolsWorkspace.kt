package com.dpis.module.settings.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.SystemFontScaleToolPresenter
import com.dpis.module.settings.SystemFontScaleToolState

/** Owns the tools page's Compose state and platform callbacks. */
class ToolsWorkspace(
    private val activity: LocalizedActivity,
    private val onComposeStateChanged: Runnable,
    private val onWriteFailed: Runnable,
) {
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

    fun onStart() {
        presenter.refresh()
    }

    fun onResume() {
        presenter.refresh()
    }

    fun onStop() = Unit

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter.refresh()
    }
}
