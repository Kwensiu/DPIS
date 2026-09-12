package com.dpis.module.runtime.presentation

import android.app.Activity
import com.dpis.module.MainActivity
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.process.presentation.ProcessActionConfirm
import com.dpis.module.process.presentation.ProcessActionHandler

/** Wires runtime launch session to MainActivity platform capabilities. */
class RuntimeLaunchShell(
    private val activity: MainActivity,
) : RuntimeLaunchSession.Shell {
    override fun activity(): Activity = activity

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun requestAppsLoad() = activity.requestAppsLoad()

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)

    override fun confirmSystemApp(): ProcessActionHandler.ConfirmSystemApp =
        ProcessActionConfirm(activity, activity::composeShell)
}
