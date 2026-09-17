package com.dpis.module.backup

import com.dpis.module.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleScopeSnapshotPolicyTest {
    @Test
    fun normalizeKeepsApplicationsAndDropsSpecialTargets() {
        assertEquals(
            listOf("com.android.settings", "com.example.app"),
            ModuleScopeSnapshotPolicy.normalize(
                listOf("android", "system_server", BuildConfig.APPLICATION_ID, "com.example.app", "com.android.settings"),
            ),
        )
    }
}
