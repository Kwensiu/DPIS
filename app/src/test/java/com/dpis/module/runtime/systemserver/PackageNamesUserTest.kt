package com.dpis.module.runtime.systemserver

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import com.dpis.module.runtime.systemserver.selection.PackageNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackageNamesUserTest {
    @Test
    fun missingApplicationUidCannotProduceATargetIdentity() {
        val activityInfo = ActivityInfo().apply {
            packageName = "com.android.chrome"
            applicationInfo = ApplicationInfo().also {
                it.packageName = "com.android.chrome"
                it.uid = 0
            }
        }

        assertNull(PackageNames.findUid("com.android.chrome", null, listOf(activityInfo)))
    }

    @Test
    fun ownerUidWithoutPackageAssociationCannotProduceATargetIdentity() {
        val unrelatedOwner = OwnerUidCarrier(999 * 100000 + 20001)

        assertNull(PackageNames.findUid("com.android.chrome", null, listOf(unrelatedOwner)))
    }

    @Test
    fun resolvesUidOnlyFromActivityInfoForTheRequestedPackage() {
        val applicationInfo = ApplicationInfo().apply {
            packageName = "com.android.chrome"
            uid = 999 * 100000 + 10123
        }
        val activityInfo = ActivityInfo().apply {
            packageName = "com.android.chrome"
            this.applicationInfo = applicationInfo
        }

        val evidence = PackageNames.findUidEvidence(
            "com.android.chrome",
            null,
            listOf(activityInfo),
        )

        assertEquals(applicationInfo.uid, evidence?.uid)
        assertEquals("activity_info.application_info", evidence?.source)
        assertNull(PackageNames.findUid("com.example.other", null, listOf(activityInfo)))
    }

    private class OwnerUidCarrier(@JvmField val ownerUid: Int)
}
