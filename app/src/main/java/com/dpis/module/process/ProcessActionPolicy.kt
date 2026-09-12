package com.dpis.module.process

import com.dpis.module.R
import com.dpis.module.process.presentation.ProcessActionHandler

/** Root and system-app confirmation rules for process actions. */
internal object ProcessActionPolicy {
    fun requiresRoot(action: ProcessActionHandler.Action): Boolean =
        action == ProcessActionHandler.Action.RESTART ||
            action == ProcessActionHandler.Action.STOP

    fun requiresSystemAppConfirmation(
        systemApp: Boolean,
        action: ProcessActionHandler.Action,
    ): Boolean = systemApp && action != ProcessActionHandler.Action.START

    fun rootRequiredMessageResId(action: ProcessActionHandler.Action): Int =
        if (action == ProcessActionHandler.Action.STOP) {
            R.string.dialog_process_stop_requires_root
        } else {
            R.string.dialog_process_restart_requires_root
        }
}
