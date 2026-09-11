package com.dpis.module.process

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.root.RootAppProcessLauncher

class ProcessActionHandler @JvmOverloads constructor(
    private val activity: Activity,
    private val beforeTargetLaunch: BeforeTargetLaunch? = null,
    private val confirmSystemApp: ConfirmSystemApp? = null,
) {
    enum class Action {
        START,
        RESTART,
        STOP,
    }

    fun interface BeforeTargetLaunch {
        fun run(packageName: String)
    }

    fun interface ConfirmSystemApp {
        fun confirm(actionLabel: String, appLabel: String, onConfirm: Runnable)
    }

    private val rootLauncher = RootAppProcessLauncher(activity)

    fun execute(item: AppListItem, action: Action) {
        if (requiresRoot(action) && !hasRootAccess()) {
            showToast(rootRequiredMessageResId(action))
            return
        }
        if (item.systemApp && action != Action.START) {
            showSystemAppActionConfirmation(item, action)
            return
        }
        runProcessAction(item.packageName, item.label, action)
    }

    private fun showSystemAppActionConfirmation(item: AppListItem, action: Action) {
        val actionLabel = resolveActionLabel(action)
        val onConfirm = Runnable { runProcessAction(item.packageName, item.label, action) }
        val host = confirmSystemApp
        if (host != null) {
            host.confirm(actionLabel, item.label, onConfirm)
            return
        }
        com.dpis.module.ui.dialog.ConfirmDialog.show(
            activity,
            activity.getString(R.string.dialog_process_action_confirm_title),
            activity.getString(
                R.string.dialog_process_action_confirm_message,
                actionLabel,
                item.label,
            ),
            onConfirm,
            Runnable {},
        )
    }

    private fun resolveActionLabel(action: Action): String = when (action) {
        Action.START -> activity.getString(R.string.dialog_process_action_start)
        Action.RESTART -> activity.getString(R.string.dialog_process_action_restart)
        Action.STOP -> activity.getString(R.string.dialog_process_action_stop)
    }

    private fun runProcessAction(packageName: String, appLabel: String, action: Action) {
        val actionLabel = resolveActionLabel(action)
        Thread({
            val result = when (action) {
                Action.START -> startPackageWithRoot(packageName)
                Action.STOP -> rootLauncher.forceStop(packageName)
                Action.RESTART -> {
                    val stopped = rootLauncher.forceStop(packageName)
                    if (stopped.code() == 0) startPackageWithRoot(packageName) else stopped
                }
            }
            activity.runOnUiThread {
                if (!isActivityAlive()) return@runOnUiThread
                if (result.code() == 0) {
                    showToast(R.string.dialog_process_action_success, actionLabel, appLabel)
                    return@runOnUiThread
                }
                val reason = result.output().ifEmpty { "unknown error" }
                showToast(R.string.dialog_process_action_failed, actionLabel, appLabel, reason)
            }
        }, "dpis-process-action").start()
    }

    private fun startPackageWithRoot(packageName: String): RootAppProcessLauncher.ShellResult {
        syncBeforeTargetLaunch(packageName)
        val started = rootLauncher.start(packageName)
        return if (started.code() != 0) startPackage(packageName) else started
    }

    private fun syncBeforeTargetLaunch(packageName: String) {
        beforeTargetLaunch?.run(packageName)
    }

    private fun requiresRoot(action: Action): Boolean =
        action == Action.RESTART || action == Action.STOP

    private fun rootRequiredMessageResId(action: Action): Int =
        if (action == Action.STOP) {
            R.string.dialog_process_stop_requires_root
        } else {
            R.string.dialog_process_restart_requires_root
        }

    private fun startPackage(packageName: String): RootAppProcessLauncher.ShellResult {
        val launchIntent = activity.packageManager.getLaunchIntentForPackage(packageName)
            ?: return RootAppProcessLauncher.ShellResult(-1, "launcher activity not found")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.runOnUiThread {
            if (isActivityAlive()) {
                activity.startActivity(launchIntent)
            }
        }
        return RootAppProcessLauncher.ShellResult(0, "")
    }

    private fun hasRootAccess(): Boolean {
        val result = RootAccessProbe.probe()
        return result.status == RootAccessProbe.Status.AVAILABLE
    }

    private fun showToast(messageResId: Int, vararg formatArgs: Any?) {
        if (!isActivityAlive()) return
        Toast.makeText(
            activity,
            activity.getString(messageResId, *formatArgs),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun isActivityAlive(): Boolean = !activity.isFinishing && !activity.isDestroyed
}
