package com.dpis.module.appconfig

object WechatDpiConfig {
    @JvmField
    val PACKAGE_NAME = "com.tencent.mm"

    @JvmField
    val MIN_DPI = 200

    @JvmField
    val MAX_DPI = 1000

    @JvmStatic
    fun appliesTo(packageName: String?): Boolean = PACKAGE_NAME == packageName

    @JvmStatic
    fun parseOrNull(raw: String?): Int? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return raw.trim().toIntOrNull()?.let(::normalize)
    }

    @JvmStatic
    fun isInputValid(raw: String?): Boolean = raw.isNullOrBlank() || parseOrNull(raw) != null

    @JvmStatic
    fun normalize(value: Int?): Int? =
        value?.takeIf { it in MIN_DPI..MAX_DPI }
}
