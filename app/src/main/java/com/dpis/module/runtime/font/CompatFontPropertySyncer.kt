package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.transport.RootCommandRunner

object CompatFontPropertySyncer {
    @JvmStatic
    fun publishTargetAsync(
        packageName: String?,
        fontScalePercent: Int,
        mode: String?,
        userId: Int
    ) {
        if (packageName.isNullOrBlank() || fontScalePercent <= 0) return
        Thread(
            {
                runRootCommand(
                    buildCompatConfigCommand(
                        packageName,
                        fontScalePercent,
                        mode,
                        userId
                    )
                )
            },
            "DPIS-compat-font-property-publisher",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(buildClearCommand(packageName, userId)) },
            "DPIS-compat-font-property-cleaner",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        val packages = LinkedHashSet<String?>(store.getConfiguredPackages())
        if (packages.isEmpty()) return
        Thread({
            val command = StringBuilder()
            for (rawPackageName in packages) {
                val packageName = rawPackageName ?: continue
                val fontScalePercent = store.getTargetFontScalePercent(packageName)
                val mode = store.getTargetFontApplyMode(packageName)
                val value = if (store.isTargetDpisEnabled(packageName) &&
                    fontScalePercent != null && fontScalePercent > 0
                ) fontScalePercent else 0
                if (command.isNotEmpty()) command.append("; ")
                command.append(
                    buildCompatConfigCommand(
                        packageName,
                        value,
                        mode,
                        store.packageConfigOwnerUserId()
                    )
                )
            }
            if (command.isNotEmpty()) runRootCommand(command.toString())
        }, "DPIS-compat-font-property-syncer").apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun buildSetCommandForTest(property: String, fontScalePercent: Int): String =
        buildSetCommand(property, fontScalePercent)

    @JvmStatic
    fun buildCompatConfigCommandForTest(
        packageName: String,
        fontScalePercent: Int,
        mode: String?,
        userId: Int,
    ): String = buildCompatConfigCommand(packageName, fontScalePercent, mode, userId)

    private fun buildCompatConfigCommand(
        packageName: String,
        fontScalePercent: Int,
        mode: String?,
        userId: Int,
    ): String {
        val normalizedMode = FontApplyMode.normalize(mode)
        val enabled = fontScalePercent > 0 && FontApplyMode.isEnabled(normalizedMode)
        // compatfont.* keeps legacy system-emulation behavior. Field rewrite
        // reads forcefont.* plus fontmode.*, so compatfont.* must stay 0.
        val systemEmulationValue =
            if (enabled && FontApplyMode.SYSTEM_EMULATION == normalizedMode) {
                fontScalePercent
            } else 0
        val forceFontValue = if (enabled && FontApplyMode.FIELD_REWRITE == normalizedMode) {
            fontScalePercent
        } else 0
        val compatMode = if (enabled) normalizedMode else FontApplyMode.OFF
        return buildSetCommandPair(
            HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName, userId),
            HyperOsFlutterFontBridge.persistentCompatFontPropertyNameForPackage(
                packageName,
                userId
            ),
            systemEmulationValue,
        ) + "; " + buildSetCommandPair(
            HyperOsFlutterFontBridge.compatFontModePropertyNameForPackage(packageName, userId),
            HyperOsFlutterFontBridge.persistentCompatFontModePropertyNameForPackage(
                packageName,
                userId
            ),
            compatMode,
        ) + "; " + buildSetCommandPair(
            HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName, userId),
            HyperOsFlutterFontBridge.persistentForcePropertyNameForPackage(packageName, userId),
            forceFontValue,
        )
    }

    private fun buildClearCommand(packageName: String, userId: Int) =
        buildCompatConfigCommand(packageName, 0, FontApplyMode.OFF, userId)

    private fun buildSetCommand(property: String, fontScalePercent: Int) =
        buildSetCommand(property, fontScalePercent.toString())

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun buildSetCommandPair(property: String, persistentProperty: String, value: Int) =
        buildSetCommandPair(property, persistentProperty, value.toString())

    private fun buildSetCommandPair(property: String, persistentProperty: String, value: String) =
        buildSetCommand(property, value) + "; " + buildSetCommand(persistentProperty, value)

    private fun runRootCommand(command: String) = RootCommandRunner.run(command)

    private fun shellQuote(value: String?): String =
        if (value.isNullOrEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
