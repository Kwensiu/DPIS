package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitTest {
    @Test
    fun commitFailureLeavesCurrentStoreUnchanged() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        prefs.setCommitResult(false)

        assertFalse(store.setHyperOsFlutterFontHookEnabled(true))

        prefs.setCommitResult(true)
        assertFalse(store.isHyperOsFlutterFontHookEnabled)
        assertTrue(prefs.getAll().isEmpty())
    }
}
