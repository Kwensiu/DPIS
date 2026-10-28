package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.hooks.HookRuntimePolicy
import java.util.Collections
import java.util.WeakHashMap

/** Reuses an already-applied compat Configuration for the same Resources scope. */
object ResourcesConfigurationReadReuse {
    private val entries = Collections.synchronizedMap(WeakHashMap<Any, Entry>())

    @JvmStatic
    fun matches(
        scope: Any?,
        packageName: String?,
        config: Configuration,
        fontScale: Float,
        viewportHandlingEnabled: Boolean,
        fontConfigurationOverrideEnabled: Boolean,
        policy: HookRuntimePolicy?,
    ): Boolean {
        if (scope == null) return false
        val entry = entries[scope] ?: return false
        return entry.packageName == packageName &&
                entry.widthDp == config.screenWidthDp &&
                entry.heightDp == config.screenHeightDp &&
                entry.smallestWidthDp == config.smallestScreenWidthDp &&
                entry.densityDpi == config.densityDpi &&
                entry.fontScaleBits == fontScale.toBits() &&
                entry.viewportHandlingEnabled == viewportHandlingEnabled &&
                entry.fontConfigurationOverrideEnabled == fontConfigurationOverrideEnabled &&
                entry.systemServerHooksEnabled == (policy?.systemServerHooksEnabled ?: true)
    }

    @JvmStatic
    fun remember(
        scope: Any?,
        packageName: String?,
        config: Configuration,
        fontScale: Float,
        viewportHandlingEnabled: Boolean,
        fontConfigurationOverrideEnabled: Boolean,
        policy: HookRuntimePolicy?,
    ) {
        if (scope == null) return
        entries[scope] = Entry(
            packageName = packageName,
            widthDp = config.screenWidthDp,
            heightDp = config.screenHeightDp,
            smallestWidthDp = config.smallestScreenWidthDp,
            densityDpi = config.densityDpi,
            fontScaleBits = fontScale.toBits(),
            viewportHandlingEnabled = viewportHandlingEnabled,
            fontConfigurationOverrideEnabled = fontConfigurationOverrideEnabled,
            systemServerHooksEnabled = policy?.systemServerHooksEnabled ?: true,
        )
    }

    @JvmStatic
    fun invalidate() {
        entries.clear()
    }

    private data class Entry(
        val packageName: String?,
        val widthDp: Int,
        val heightDp: Int,
        val smallestWidthDp: Int,
        val densityDpi: Int,
        val fontScaleBits: Int,
        val viewportHandlingEnabled: Boolean,
        val fontConfigurationOverrideEnabled: Boolean,
        val systemServerHooksEnabled: Boolean,
    )
}
