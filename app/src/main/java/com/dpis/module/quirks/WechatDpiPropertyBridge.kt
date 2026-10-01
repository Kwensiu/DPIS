package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig
import java.lang.reflect.Method
import java.util.Locale

object WechatDpiPropertyBridge {
    private const val PROPERTY_PREFIX = "debug.dpis.wechat.dpi."
    private const val PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.wechat.dpi."

    @JvmStatic
    fun propertyNameForPackage(packageName: String?): String =
        PROPERTY_PREFIX + suffixForPackage(packageName)

    @JvmStatic
    fun persistentPropertyNameForPackage(packageName: String?): String =
        PERSIST_PROPERTY_PREFIX + suffixForPackage(packageName)

    @JvmStatic
    fun readDpi(packageName: String?): Int = readDpi(packageName, ::readSystemProperty)

    @JvmStatic
    fun readDpiForTest(packageName: String?, volatileValue: String?, persistValue: String?): Int =
        readDpi(packageName) { name ->
            when (name) {
                propertyNameForPackage(packageName) -> volatileValue
                persistentPropertyNameForPackage(packageName) -> persistValue
                else -> ""
            }
        }

    private fun readDpi(packageName: String?, reader: (String) -> String?): Int {
        if (packageName.isNullOrBlank()) return 0
        val suffix = suffixForPackage(packageName)
        return parseDpi(
            readFirstProperty(
                reader,
                PROPERTY_PREFIX + suffix,
                PERSIST_PROPERTY_PREFIX + suffix
            )
        )
    }

    private fun suffixForPackage(packageName: String?): String =
        String.format(Locale.US, "%08x", packageName?.hashCode() ?: 0)

    private fun readFirstProperty(
        reader: (String) -> String?,
        vararg propertyNames: String
    ): String {
        propertyNames.forEach { name ->
            val value = reader(name)
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    private fun readSystemProperty(propertyName: String): String = try {
        if (propertyName.isBlank()) return ""
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get: Method =
            systemProperties.getDeclaredMethod("get", String::class.java, String::class.java)
        get.invoke(null, propertyName, "") as? String ?: ""
    } catch (_: Throwable) {
        ""
    }

    private fun parseDpi(value: String?): Int {
        val parsed = value?.trim()?.toIntOrNull() ?: return 0
        return if (WechatDpiConfig.normalize(parsed) != null) parsed else 0
    }
}
