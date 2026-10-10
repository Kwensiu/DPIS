package com.dpis.module.fonts

object FontApplyMode {
    const val OFF = "off"

    // Persisted/runtime value. UI labels this as "System mode".
    const val SYSTEM_EMULATION = "system_emulation"

    // Persisted/runtime value. UI labels this as "Compat mode" to avoid
    // confusion with future font family/style replacement features.
    const val FIELD_REWRITE = "field_rewrite"

    @JvmStatic
    fun normalize(raw: String?): String = when (raw) {
        SYSTEM_EMULATION, FIELD_REWRITE, OFF -> raw
        else -> OFF
    }

    @JvmStatic
    fun isEnabled(mode: String?): Boolean = normalize(mode) != OFF
}
