package com.dpis.module.quirks

import android.os.Process
import com.dpis.module.appconfig.WechatDpiConfig
import java.lang.reflect.Method
import java.util.Locale

object WechatDpiPropertyBridge {
    private const val PROPERTY_PREFIX = "debug.dpis.wechat.dpi."
    private const val PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.wechat.dpi."

    @JvmStatic
    fun propertyNameForPackage(packageName: String?, userId: Int): String =
        PROPERTY_PREFIX + suffixForPackage(packageName, userId)

    @JvmStatic
    fun persistentPropertyNameForPackage(packageName: String?, userId: Int): String =
        PERSIST_PROPERTY_PREFIX + suffixForPackage(packageName, userId)

    @JvmStatic
    fun readDpi(packageName: String?): Int {
        if (packageName.isNullOrBlank()) return 0
        return readDpi(packageName, currentUserId())
    }

    @JvmStatic
    fun readDpi(packageName: String?, userId: Int): Int =
        readDpi(packageName, userId, ::readSystemProperty)

    @JvmStatic
    fun readDpiForTest(
        packageName: String?,
        userId: Int,
        reader: (String) -> String?,
    ): Int = readDpi(packageName, userId, reader)

    private fun readDpi(
        packageName: String?,
        userId: Int,
        reader: (String) -> String?,
    ): Int {
        if (packageName.isNullOrBlank()) return 0
        val suffix = suffixForPackage(packageName, userId)
        return parseDpi(
            readFirstProperty(
                reader,
                PROPERTY_PREFIX + suffix,
                PERSIST_PROPERTY_PREFIX + suffix,
            ),
        )
    }

    private fun suffixForPackage(packageName: String?, userId: Int): String =
        "$userId." + String.format(Locale.US, "%08x", packageName?.hashCode() ?: 0)

    private fun currentUserId(): Int = userIdFromUid(Process.myUid())

    private fun userIdFromUid(uid: Int): Int = uid / PER_USER_RANGE

    private const val PER_USER_RANGE = 100000

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
