package com.dpis.module.runtime.probe

import com.dpis.module.BuildConfig

object DebugPackageOverride {
    @JvmStatic
    fun matches(propertyName: String?, packageName: String?): Boolean =
        matches(propertyName, packageName) { readSystemProperty(propertyName) }

    @JvmStatic
    fun matchesForTest(
        propertyName: String?,
        packageName: String?,
        propertyValue: String?,
    ): Boolean = matches(propertyName, packageName) { propertyValue }

    private fun matches(
        propertyName: String?,
        packageName: String?,
        reader: () -> String?,
    ): Boolean {
        if (!BuildConfig.DEBUG || packageName.isNullOrBlank()) {
            return false
        }
        val normalized = reader()?.trim() ?: return false
        return normalized == "*" || packageName == normalized
    }

    private fun readSystemProperty(key: String?): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            systemProperties
                .getMethod("get", String::class.java, String::class.java)
                .invoke(null, key, "") as? String
        } catch (_: ReflectiveOperationException) {
            ""
        } catch (_: RuntimeException) {
            ""
        }
    }
}
