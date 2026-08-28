package com.dpis.module.backup

object BackupKeyPolicy {
    private val localOnlyPrefixes = arrayOf("font.library.", "font.debug.", "runtime.")

    @JvmStatic
    fun isLocalOnly(key: String?): Boolean = key.isNullOrEmpty() || localOnlyPrefixes.any(key::startsWith)

    @JvmStatic
    fun isImportable(key: String?): Boolean = isKnownPortable(key) && !isLocalOnly(key)

    @JvmStatic
    fun isKnownPortable(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        if (key in setOf("target_packages", "system_server.hooks_enabled",
                "system_server.safe_mode_enabled", "global.log_enabled")) return true
        return arrayOf("package_config.", "viewport.", "font.", "target.", "wechat.",
            "resolution.", "default_config.", "template.", "fluid_cloud.", "global.",
            "system_server.", "ui.").any(key::startsWith)
    }
}
