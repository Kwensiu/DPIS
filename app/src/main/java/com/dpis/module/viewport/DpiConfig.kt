package com.dpis.module.viewport

/** Seed viewport settings for packages that need a stable compatibility baseline. */
object DpiConfig {
    const val SEED_TARGET_VIEWPORT_WIDTH_DP: Int = 360

    @JvmField
    val TARGET_PACKAGES: Array<String> = arrayOf(
        "bin.mt.plus.canary",
        "com.max.xiaoheihe",
    )

    @JvmStatic
    fun shouldHandlePackage(packageName: String?): Boolean =
        packageName != null && TARGET_PACKAGES.any { it == packageName }

    @JvmStatic
    fun getSeedTargetPackages(): Set<String> = linkedSetOf(*TARGET_PACKAGES)

    @JvmStatic
    fun getSeedViewportWidthDps(): MutableMap<String?, Int?> =
        linkedMapOf<String?, Int?>().apply {
            TARGET_PACKAGES.forEach { put(it, SEED_TARGET_VIEWPORT_WIDTH_DP) }
        }

    @JvmStatic
    fun getSeedViewportWidthDp(packageName: String?): Int? =
        SEED_TARGET_VIEWPORT_WIDTH_DP.takeIf { shouldHandlePackage(packageName) }

    @JvmStatic
    fun getTargetPackagesText(): String = TARGET_PACKAGES.joinToString(", ")
}
