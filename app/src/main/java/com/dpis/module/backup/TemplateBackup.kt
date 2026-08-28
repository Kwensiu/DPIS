package com.dpis.module.backup

import com.dpis.module.templates.TemplateConfigValue

data class TemplateBackup(
    @JvmField val id: String,
    @JvmField val name: String,
    @JvmField val updatedAt: Long,
    @JvmField val selectedPackages: Set<String>,
    @JvmField val configValue: TemplateConfigValue
)
