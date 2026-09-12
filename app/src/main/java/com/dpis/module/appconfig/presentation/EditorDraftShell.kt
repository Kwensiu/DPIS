package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.ui.MainViewModel

/** Wires XML editor draft tracking to MainActivity platform capabilities. */
class EditorDraftShell(
    private val activity: MainActivity,
) : EditorDraftSession.Shell {
    override fun activity(): Activity = activity

    override fun viewModel(): MainViewModel? = activity.editorViewModel()

    override fun landDetailContent(): FrameLayout? = activity.landDetailContent()
}
