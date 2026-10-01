package com.dpis.module.viewport

object ViewportApplyMode {
    const val OFF = "off"
    const val AUTO = "auto"
    const val SYSTEM = "system"
    const val COMPAT = "compat"
    const val LEGACY_SYSTEM_EMULATION = "system_emulation"
    const val LEGACY_FIELD_REWRITE = "field_rewrite"

    @Deprecated("Use SYSTEM")
    const val SYSTEM_EMULATION = SYSTEM

    @Deprecated("Use COMPAT")
    const val FIELD_REWRITE = COMPAT

    @JvmStatic
    fun normalize(mode: String?): String = when (mode) {
        AUTO -> AUTO
        COMPAT, LEGACY_FIELD_REWRITE -> COMPAT
        SYSTEM, LEGACY_SYSTEM_EMULATION -> SYSTEM
        else -> OFF
    }

    @JvmStatic
    fun isEnabled(mode: String?): Boolean = when (normalize(mode)) {
        AUTO, SYSTEM, COMPAT -> true
        else -> false
    }
}

