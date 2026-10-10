package com.dpis.module.fonts

class FontLibraryConfigStore(private val delegate: Delegate?) {
    interface Delegate {
        fun clearTargetTypefaceId(packageName: String?): Boolean

        fun getConfiguredPackages(): Set<String>

        fun getTargetTypefaceId(packageName: String?): String?

        fun setTargetTypefaceId(packageName: String?, typefaceId: String?): Boolean
    }

    fun clearTargetTypefaceId(packageName: String?): Boolean =
        delegate?.clearTargetTypefaceId(packageName) == true

    val configuredPackages: Set<String>
        get() = delegate?.getConfiguredPackages() ?: emptySet()

    fun getTargetTypefaceId(packageName: String?): String? =
        delegate?.getTargetTypefaceId(packageName)

    fun setTargetTypefaceId(packageName: String?, typefaceId: String?): Boolean =
        delegate?.setTargetTypefaceId(packageName, typefaceId) == true
}
