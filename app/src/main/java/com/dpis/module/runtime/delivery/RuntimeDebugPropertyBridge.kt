package com.dpis.module.runtime.delivery

import java.lang.reflect.Method

object RuntimeDebugPropertyBridge {
    private const val PROP_GLOBAL_LOG_ENABLED = "debug.dpis.global_log_enabled"
    private const val PERSIST_PROP_GLOBAL_LOG_ENABLED = "persist.debug.dpis.global_log_enabled"
    private const val PROP_FONT_DEBUG_OVERLAY_ENABLED = "debug.dpis.font_debug_overlay_enabled"
    private const val PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED =
        "persist.debug.dpis.font_debug_overlay_enabled"

    @JvmStatic
    internal fun globalLogPropertyName(): String = PROP_GLOBAL_LOG_ENABLED

    @JvmStatic
    internal fun persistentGlobalLogPropertyName(): String = PERSIST_PROP_GLOBAL_LOG_ENABLED

    @JvmStatic
    internal fun fontDebugOverlayPropertyName(): String = PROP_FONT_DEBUG_OVERLAY_ENABLED

    @JvmStatic
    internal fun persistentFontDebugOverlayPropertyName(): String =
        PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED

    @JvmStatic
    @JvmOverloads
    fun readGlobalLogEnabled(reader: (String) -> String = ::readSystemProperty): Boolean =
        readBooleanWithPersistentFallback(
            PROP_GLOBAL_LOG_ENABLED,
            PERSIST_PROP_GLOBAL_LOG_ENABLED,
            false,
            reader,
        )

    @JvmStatic
    @JvmOverloads
    fun readFontDebugOverlayEnabled(reader: (String) -> String = ::readSystemProperty): Boolean =
        readBooleanWithPersistentFallback(
            PROP_FONT_DEBUG_OVERLAY_ENABLED,
            PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED,
            false,
            reader,
        )

    private fun readBooleanWithPersistentFallback(
        key: String,
        persistentKey: String,
        defaultValue: Boolean,
        reader: (String) -> String,
    ): Boolean {
        var value = reader(key)
        if (value.isNullOrBlank()) {
            value = reader(persistentKey)
        }
        if (value.isNullOrBlank()) {
            return defaultValue
        }
        return value.trim() == "1" || value.trim().equals("true", ignoreCase = true)
    }

    private fun readSystemProperty(key: String): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemProperties.getDeclaredMethod(
                "get",
                String::class.java,
                String::class.java,
            )
            (getMethod.invoke(null, key, "") as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }
}
