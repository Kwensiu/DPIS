package com.dpis.module.viewport

data class PerAppDisplayEnvironment @JvmOverloads constructor(
    @JvmField val widthDp: Int,
    @JvmField val heightDp: Int,
    @JvmField val smallestWidthDp: Int,
    @JvmField val densityDpi: Int,
    @JvmField val widthPx: Int,
    @JvmField val heightPx: Int,
)
