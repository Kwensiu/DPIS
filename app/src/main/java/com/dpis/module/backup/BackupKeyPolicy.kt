package com.dpis.module.backup

object BackupKeyPolicy {
    private val localOnlyPrefixes = arrayOf("font.library.", "font.debug.", "runtime.")
    private val packageConfigFields = setOf(
        "viewport.width_dp", "viewport.target_type", "viewport.scale_permille",
        "viewport.scale_milli_percent", "viewport.mode", "font.scale_percent",
        "font.typeface_id", "font.mode", "font.hook_domains",
        "target.dpis_enabled", "app.wechat_dpi",
    )
    private val templateConfigFields = setOf(
        "viewport.target_type", "viewport.width_dp", "viewport.scale_permille",
        "viewport.scale_milli_percent", "viewport.width_draft_dp",
        "viewport.scale_draft_permille", "viewport.scale_draft_milli_percent",
        "viewport.mode", "font.scale_percent", "font.mode",
        "font.typeface_id", "font.hook_domains",
    )
    private val fixedPortableKeys = setOf(
        "target_packages",
        "system_server.hooks_enabled",
        "system_server.safe_mode_enabled",
        "global.log_enabled",
        "font.flutter_hook_enabled",
        "font.flutter_settings_hook_enabled",
        "font.hyperos_flutter_hook_enabled",
        "ui.hide_launcher_icon",
        "ui.interface_scale_percent",
        "ui.startup_disclaimer_accepted",
        "fluid_cloud.hole_left",
        "fluid_cloud.hole_right",
    )
    private val legacyPackageFieldPatterns = setOf(
        "viewport" to "width_dp",
        "viewport" to "target_type",
        "viewport" to "scale_permille",
        "viewport" to "scale_milli_percent",
        "viewport" to "mode",
        "font" to "scale_percent",
        "font" to "typeface_id",
        "font" to "mode",
        "font" to "hook_domains",
        "target" to "dpis_enabled",
        "wechat" to "dpi",
    )

    @JvmStatic
    fun isLocalOnly(key: String?): Boolean = key.isNullOrEmpty() || localOnlyPrefixes.any(key::startsWith)

    @JvmStatic
    fun isImportable(key: String?): Boolean = isKnownPortable(key) && !isLocalOnly(key)

    @JvmStatic
    fun isKnownPortable(key: String?): Boolean {
        if (key.isNullOrEmpty()) return false
        return key in fixedPortableKeys ||
            hasKnownField(key, "package_config.", packageConfigFields) ||
            hasKnownLegacyPackageField(key) ||
            hasKnownField(key, "resolution.", setOf("width_px", "height_px", "mode")) ||
            key.removePrefix("default_config.").let(templateConfigFields::contains) ||
            hasKnownTemplateField(key)
    }

    private fun hasKnownLegacyPackageField(key: String): Boolean =
        legacyPackageFieldPatterns.any { (domain, suffix) ->
            key.startsWith("$domain.") && key.endsWith(".$suffix") &&
                key.length > domain.length + suffix.length + 2
        }

    private fun hasKnownField(key: String, prefix: String, fields: Set<String>): Boolean {
        if (!key.startsWith(prefix)) return false
        val remainder = key.removePrefix(prefix)
        return fields.any { field ->
            remainder.endsWith(".$field") && remainder.length > field.length + 1
        }
    }

    private fun hasKnownTemplateField(key: String): Boolean {
        if (key == "template.ids" || key == "template.order") return true
        if (!key.startsWith("template.")) return false
        val remainder = key.removePrefix("template.")
        val separator = remainder.indexOf('.')
        if (separator <= 0) return false
        val templateId = remainder.substring(0, separator)
        if (templateId.contains('.')) return false
        val field = remainder.substring(separator + 1)
        return field in setOf("name", "updated_at", "selected_packages") ||
            field.removePrefix("config.").let(templateConfigFields::contains)
    }
}
