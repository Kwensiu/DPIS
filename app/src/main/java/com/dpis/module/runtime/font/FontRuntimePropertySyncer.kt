package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDecision
import com.dpis.module.runtime.transport.RootCommandRunner

object FontRuntimePropertySyncer {
    @JvmStatic
    fun publishTargetAsync(
        packageName: String?,
        fontScalePercent: Int,
        mode: String?,
        hyperOsNativeFontHookEnabled: Boolean,
        userId: Int,
    ) {
        if (packageName.isNullOrBlank() || fontScalePercent <= 0) return
        Thread(
            {
                runRootCommand(
                    buildTargetCommand(
                        packageName,
                        fontScalePercent,
                        mode,
                        hyperOsNativeFontHookEnabled,
                        userId,
                    ),
                )
            },
            "DPIS-font-runtime-property-publisher",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun publishTypefaceTargetAsync(packageName: String?, typefaceId: String?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(buildTypefaceCommand(packageName, typefaceId, userId)) },
            "DPIS-typeface-runtime-property-publisher",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearTargetAsync(packageName: String?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(buildClearTargetCommand(packageName, userId)) },
            "DPIS-font-runtime-property-cleaner",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearFontScaleTargetAsync(packageName: String?, userId: Int) {
        if (packageName.isNullOrBlank()) return
        Thread(
            { runRootCommand(buildClearFontScaleCommand(packageName, userId)) },
            "DPIS-font-scale-runtime-property-cleaner",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        Thread({ syncConfiguredTargets(store) }, "DPIS-font-runtime-property-syncer")
            .apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredTargets(store: DpisConfigStore?) {
        if (store == null) return
        buildConfiguredTargetsCommand(store).takeIf { it.isNotEmpty() }?.let(::runRootCommand)
    }

    @JvmStatic
    fun syncTarget(packageName: String?, store: DpisConfigStore?) {
        if (packageName.isNullOrBlank() || store == null) return
        runRootCommand(buildTargetCommands(packageName, store))
    }

    @JvmStatic
    fun buildTargetCommandForTest(
        packageName: String,
        fontScalePercent: Int,
        mode: String?,
        hyperOsNativeFontHookEnabled: Boolean,
        userId: Int,
    ): String =
        buildTargetCommand(
            packageName,
            fontScalePercent,
            mode,
            hyperOsNativeFontHookEnabled,
            userId
        )

    @JvmStatic
    fun buildClearTargetCommandForTest(packageName: String, userId: Int): String =
        buildClearTargetCommand(packageName, userId)

    @JvmStatic
    fun buildClearFontScaleCommandForTest(packageName: String, userId: Int): String =
        buildClearFontScaleCommand(packageName, userId)

    @JvmStatic
    fun buildTypefaceCommandForTest(packageName: String, typefaceId: String?, userId: Int): String =
        buildTypefaceCommand(packageName, typefaceId, userId)

    private fun buildTargetCommand(
        packageName: String,
        fontScalePercent: Int,
        mode: String?,
        hyperOsNativeFontHookEnabled: Boolean,
        userId: Int,
    ): String {
        val normalizedMode = FontApplyMode.normalize(mode)
        val enabled = fontScalePercent > 0 && FontApplyMode.isEnabled(normalizedMode)
        val systemEmulationValue =
            if (enabled && FontApplyMode.SYSTEM_EMULATION == normalizedMode) {
                fontScalePercent
            } else 0
        // forcefont.* is the live override consumed by HyperOS native proxies and by
        // legacy field rewrite. It must be written once with the final semantic value.
        val forceFontValue = if (enabled &&
            (FontApplyMode.FIELD_REWRITE == normalizedMode || hyperOsNativeFontHookEnabled)
        ) fontScalePercent else 0
        val compatMode = if (enabled) normalizedMode else FontApplyMode.OFF
        return buildSetCommand(
            HyperOsFlutterFontBridge.compatFontPropertyNameForPackage(packageName, userId),
            systemEmulationValue,
        ) + "; " + buildSetCommand(
            HyperOsFlutterFontBridge.persistentCompatFontPropertyNameForPackage(
                packageName,
                userId
            ),
            systemEmulationValue,
        ) + "; " + buildSetCommand(
            HyperOsFlutterFontBridge.compatFontModePropertyNameForPackage(packageName, userId),
            compatMode,
        ) + "; " + buildSetCommand(
            HyperOsFlutterFontBridge.persistentCompatFontModePropertyNameForPackage(
                packageName,
                userId
            ),
            compatMode,
        ) + "; " + buildSetCommand(
            HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName, userId),
            forceFontValue,
        ) + "; " + buildSetCommand(
            HyperOsFlutterFontBridge.persistentForcePropertyNameForPackage(packageName, userId),
            forceFontValue,
        )
    }

    private fun buildClearTargetCommand(packageName: String, userId: Int) =
        buildClearFontScaleCommand(packageName, userId) + "; " + buildTypefaceCommand(
            packageName,
            null,
            userId
        )

    private fun buildClearFontScaleCommand(packageName: String, userId: Int) =
        buildSetCommand(HyperOsFlutterFontBridge.propertyNameForPackage(packageName, userId), 0) +
                "; " + buildSetCommand(
            HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(
                packageName, userId
            ), 0
        ) +
                "; " + buildTargetCommand(packageName, 0, FontApplyMode.OFF, false, userId)

    private fun buildTypefaceCommand(
        packageName: String,
        typefaceId: String?,
        userId: Int
    ): String {
        val value = if (typefaceId.isNullOrBlank()) "0" else typefaceId.trim()
        return buildSetCommand(
            HyperOsFlutterFontBridge.typefacePropertyNameForPackage(packageName, userId),
            value
        ) +
                "; " + buildSetCommand(
            HyperOsFlutterFontBridge.persistentTypefacePropertyNameForPackage(packageName, userId),
            value,
        )
    }

    private fun buildConfiguredTargetsCommand(store: DpisConfigStore): String {
        val packages = LinkedHashSet<String?>(store.getConfiguredPackages())
        if (packages.isEmpty()) return ""
        val command = StringBuilder()
        for (rawPackageName in packages) {
            val packageName = rawPackageName ?: continue
            appendCommand(command, buildTargetCommands(packageName, store))
        }
        return command.toString()
    }

    private fun buildTargetCommands(packageName: String, store: DpisConfigStore): String {
        val userId = store.packageConfigOwnerUserId()
        val fontScalePercent = store.getTargetFontScalePercent(packageName)
        val mode = store.getTargetFontApplyMode(packageName)
        val typefaceId = if (store.isTargetDpisEnabled(packageName)) {
            store.getTargetTypefaceId(packageName)
        } else null
        val value = if (store.isTargetDpisEnabled(packageName) &&
            fontScalePercent != null && fontScalePercent > 0
        ) fontScalePercent else 0
        val command = StringBuilder()
        appendCommand(
            command,
            buildTargetCommand(
                packageName,
                value,
                mode,
                FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName),
                userId,
            ),
        )
        appendCommand(command, buildTypefaceCommand(packageName, typefaceId, userId))
        return command.toString()
    }

    private fun appendCommand(command: StringBuilder, fragment: String?) {
        if (fragment.isNullOrEmpty()) return
        if (command.isNotEmpty()) command.append("; ")
        command.append(fragment)
    }

    private fun buildSetCommand(property: String, value: Int) =
        buildSetCommand(property, value.toString())

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun runRootCommand(command: String) = RootCommandRunner.run(command)

    private fun shellQuote(value: String?): String =
        if (value.isNullOrEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
