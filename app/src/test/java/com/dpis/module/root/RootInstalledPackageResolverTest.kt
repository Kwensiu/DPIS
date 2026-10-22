package com.dpis.module.root

import org.junit.Assert.assertEquals
import org.junit.Test

class RootInstalledPackageResolverTest {
    @Test
    fun parsesThePackagesReportedForAnExplicitAndroidUser() {
        assertEquals(
            setOf("android", "com.android.chrome", "com.tencent.mm"),
            RootInstalledPackageResolver.parsePackageNames(
                "package:android\npackage:com.android.chrome\npackage:com.tencent.mm\n",
            ),
        )
    }
}
