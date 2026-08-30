package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.templates.TemplateConfigValue

class PackageConfigRepository(private val store: DpisConfigStore?) {
    fun hasRealPackageConfig(packageName: String?): Boolean =
        store?.hasRealPackageConfig(packageName) == true

    fun writePackageTemplateConfigValue(
        packageName: String?,
        value: TemplateConfigValue?,
    ): Boolean = store?.writePackageTemplateConfigValue(packageName, value) == true
}
