package com.dpis.module.viewport

class ViewportRuntimeRecord(
    @JvmField val packageName: String?,
    targetSpec: ViewportTargetSpec?,
    @JvmField val sourceSignature: String?,
    @JvmField val effectiveSmallestWidthDp: Int,
    @JvmField val viewportResult: ViewportOverride.Result?,
    @JvmField val virtualDisplayResult: VirtualDisplayOverride.Result?,
    @JvmField val resultSignature: String?,
    provenance: String?,
    @JvmField val createdElapsedRealtime: Long,
    scope: String?,
) {
    companion object {
        const val PROVENANCE_SYSTEM_SERVER = "s"
        const val PROVENANCE_APP_PROCESS = "a"
    }

    @JvmField
    val targetSpec: ViewportTargetSpec = targetSpec ?: ViewportTargetSpec.off()
    @JvmField
    val targetFingerprint: String = this.targetSpec.fingerprint()
    @JvmField
    val provenance: String = provenance ?: PROVENANCE_APP_PROCESS
    @JvmField
    val scope: String = scope ?: ViewportSourceSnapshot.SCOPE_UNKNOWN

    fun matchesPackageAndTarget(packageName: String?, targetSpec: ViewportTargetSpec?): Boolean =
        this.packageName != null && this.packageName == packageName &&
                targetFingerprint == (targetSpec?.fingerprint() ?: "off")

    fun displayScoped(): Boolean = ViewportSourceSnapshot.SCOPE_DISPLAY == scope
}
