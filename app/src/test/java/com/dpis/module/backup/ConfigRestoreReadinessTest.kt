package com.dpis.module.backup

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigRestoreReadinessTest {
    @Test
    fun backupRestoreRechecksReadinessWhenQueuedWorkRuns() {
        val packageName = "com.example.app"
        val store = DpisConfigStore(FakePrefs())
        assertTrue(
            store.setTargetViewportSpec(
                packageName,
                ViewportTargetSpec.relativeScale(80000)
            )
        )
        var ready = true
        var restoreCalls = 0
        var publishCalls = 0
        var queuedTask: Runnable? = null

        val accepted = ConfigRestoreReadiness.runIfReady({ ready }) {
            queuedTask = Runnable {
                ConfigRestoreReadiness.runIfReady({ ready }) {
                    restoreCalls++
                    store.setTargetViewportSpec(
                        packageName,
                        ViewportTargetSpec.relativeScale(120000)
                    )
                    publishCalls++
                }
            }
        }
        assertTrue(accepted)

        ready = false
        queuedTask!!.run()

        assertEquals(
            ViewportTargetSpec.relativeScale(80000),
            store.getTargetViewportSpec(packageName)
        )
        assertEquals(0, restoreCalls)
        assertEquals(0, publishCalls)
    }
}
