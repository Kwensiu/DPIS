package com.dpis.module.process

import com.dpis.module.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.process.presentation.ProcessActionHandler

class ProcessActionPolicyTest {
    @Test
    fun startDoesNotRequireRootOrSystemConfirmation() {
        assertFalse(ProcessActionPolicy.requiresRoot(ProcessActionHandler.Action.START))
        assertFalse(
            ProcessActionPolicy.requiresSystemAppConfirmation(
                systemApp = true,
                ProcessActionHandler.Action.START,
            ),
        )
        assertFalse(
            ProcessActionPolicy.requiresSystemAppConfirmation(
                systemApp = false,
                ProcessActionHandler.Action.RESTART,
            ),
        )
        assertEquals(
            R.string.dialog_process_restart_requires_root,
            ProcessActionPolicy.rootRequiredMessageResId(ProcessActionHandler.Action.START),
        )
    }

    @Test
    fun restartAndStopRequireRootAndSystemConfirmation() {
        assertTrue(ProcessActionPolicy.requiresRoot(ProcessActionHandler.Action.RESTART))
        assertTrue(ProcessActionPolicy.requiresRoot(ProcessActionHandler.Action.STOP))
        assertTrue(
            ProcessActionPolicy.requiresSystemAppConfirmation(
                systemApp = true,
                ProcessActionHandler.Action.RESTART,
            ),
        )
        assertTrue(
            ProcessActionPolicy.requiresSystemAppConfirmation(
                systemApp = true,
                ProcessActionHandler.Action.STOP,
            ),
        )
        assertEquals(
            R.string.dialog_process_restart_requires_root,
            ProcessActionPolicy.rootRequiredMessageResId(ProcessActionHandler.Action.RESTART),
        )
        assertEquals(
            R.string.dialog_process_stop_requires_root,
            ProcessActionPolicy.rootRequiredMessageResId(ProcessActionHandler.Action.STOP),
        )
    }
}
