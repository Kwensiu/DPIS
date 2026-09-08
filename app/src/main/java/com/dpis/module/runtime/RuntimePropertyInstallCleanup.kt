package com.dpis.module.runtime

import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/** Clears per-package DPIS runtime mirrors once after a fresh app installation. */
object RuntimePropertyInstallCleanup {
    private const val PREFERENCES = "runtime_property_install_state"
    private const val INITIALIZED = "per_package_properties_initialized"

    private val CLEANUP_EXECUTOR = Executors.newSingleThreadExecutor(
        ThreadFactory { runnable ->
            Thread(runnable, "DPIS-runtime-property-install-cleanup").apply {
                isDaemon = true
            }
        }
    )

    private val PER_PACKAGE_PREFIXES = listOf(
        "debug.dpis.vp.",
        "debug.dpis.vptype.",
        "debug.dpis.vpscale.",
        "debug.dpis.vpcfg.",
        "debug.dpis.vpmode.",
        "persist.debug.dpis.vp.",
        "persist.debug.dpis.vptype.",
        "persist.debug.dpis.vpscale.",
        "persist.debug.dpis.vpcfg.",
        "persist.debug.dpis.vpmode.",
        "debug.dpis.font.",
        "debug.dpis.forcefont.",
        "debug.dpis.compatfont.",
        "debug.dpis.fontmode.",
        "debug.dpis.typeface.",
        "debug.dpis.rustbin.",
        "persist.debug.dpis.forcefont.",
        "persist.debug.dpis.compatfont.",
        "persist.debug.dpis.fontmode.",
        "persist.debug.dpis.typeface.",
        "debug.dpis.hookdomains.",
        "persist.debug.dpis.hookdomains.",
        "debug.dpis.wechat.dpi.",
        "persist.debug.dpis.wechat.dpi.",
    )

    @JvmStatic
    fun initializeAsync(context: Context?, onComplete: Runnable? = null) {
        if (context == null) {
            onComplete?.run()
            return
        }
        val applicationContext = context.applicationContext ?: context
        CLEANUP_EXECUTOR.execute {
            try {
                val preferences = applicationContext.getSharedPreferences(
                    PREFERENCES,
                    Context.MODE_PRIVATE
                )
                if (!preferences.getBoolean(INITIALIZED, false)) {
                    // This is install migration work, not an application-start prerequisite.
                    // Keep root authorization and the property pipeline outside the caller's
                    // lifecycle thread.
                    if (RootCommandRunner.run(buildCleanupCommand())) {
                        preferences.edit().putBoolean(INITIALIZED, true).apply()
                    }
                }
            } finally {
                onComplete?.run()
            }
        }
    }

    @JvmStatic
    fun buildCleanupCommandForTest(): String = buildCleanupCommand()

    private fun buildCleanupCommand(): String {
        // The pattern is embedded in awk, so use POSIX-compatible escaped dots rather than
        // Kotlin's \Q...\E regex quoting, which awk does not understand.
        val prefixPattern = PER_PACKAGE_PREFIXES.joinToString("|") { it.replace(".", "\\.") }
        return "getprop | awk -F'[][]' '$2 ~ /^($prefixPattern)/ {print $2}' | " +
            "while read -r property; do setprop \"\$property\" 0; done"
    }
}
