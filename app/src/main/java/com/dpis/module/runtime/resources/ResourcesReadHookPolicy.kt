package com.dpis.module.runtime.resources


data class ResourcesReadHookPolicy(
    @JvmField val viewportHandlingEnabled: Boolean,
    @JvmField val configurationFontOverrideEnabled: Boolean,
    @JvmField val metricsTargetFontOverrideEnabled: Boolean,
) {
    companion object {
        @JvmField
        val FULL = ResourcesReadHookPolicy(
            viewportHandlingEnabled = true,
            configurationFontOverrideEnabled = true,
            metricsTargetFontOverrideEnabled = false,
        )
    }
}
