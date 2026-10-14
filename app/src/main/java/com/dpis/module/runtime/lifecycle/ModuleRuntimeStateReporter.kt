package com.dpis.module.runtime.lifecycle

import com.dpis.module.diagnostics.DpisLog

object ModuleRuntimeStateReporter {
    private const val KEY_SYSTEM_SERVER_LOADED_AT = "debug.dpis.module.system_server_loaded_at"

    @JvmStatic
    fun reportSystemServerLoaded() {
        setSystemProperty(KEY_SYSTEM_SERVER_LOADED_AT, System.currentTimeMillis().toString())
    }

    @JvmStatic
    fun getSystemServerLoadedAt(): Long {
        val value = getSystemProperty(KEY_SYSTEM_SERVER_LOADED_AT, "0")
        return value.toLongOrNull() ?: 0L
    }

    private fun setSystemProperty(key: String, value: String) {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val set =
                systemProperties.getDeclaredMethod("set", String::class.java, String::class.java)
            set.invoke(null, key, value)
        } catch (throwable: Throwable) {
            DpisLog.e("module runtime state report failed: key=$key", throwable)
        }
    }

    private fun getSystemProperty(key: String, fallback: String): String = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get = systemProperties.getDeclaredMethod("get", String::class.java, String::class.java)
        get.invoke(null, key, fallback) as? String ?: fallback
    } catch (_: Throwable) {
        fallback
    }
}
