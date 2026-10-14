package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDecision
import com.dpis.module.runtime.transport.RootCommandRunner

object HyperOsNativeFontPropertySyncer {
    @JvmStatic
    fun publishForceFontTargetAsync(packageName: String?, fontScalePercent: Int) {
        if (packageName.isNullOrBlank() || fontScalePercent <= 0) return
        // Native proxies prefer forcefont so value-only changes can bypass stale
        // system_server RustProcess env values after a same-version module update.
        val fontProperty = HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName)
        Thread(
            { publishPropertyWithRoot(fontProperty, fontScalePercent) },
            "DPIS-hyperos-property-publisher",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearFontTargetAsync(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        HyperOsFlutterFontBridge.clearTarget(packageName)
        val fontProperty = HyperOsFlutterFontBridge.propertyNameForPackage(packageName)
        val forceFontProperty = HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName)
        val rustBinaryProperty =
            HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(packageName)
        Thread(
            { clearPropertiesWithRoot(fontProperty, forceFontProperty, rustBinaryProperty) },
            "DPIS-hyperos-property-cleaner",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun clearConfiguredFontTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        val packages = LinkedHashSet<String?>(store.getConfiguredPackages())
        if (packages.isEmpty()) return
        Thread(
            { clearFontTargets(store, packages) },
            "DPIS-hyperos-configured-property-cleaner",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun syncConfiguredFontTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        for (rawPackageName in store.getConfiguredPackages()) {
            val packageName = rawPackageName ?: continue
            val fontScalePercent = store.getTargetFontScalePercent(packageName)
            val fontMode = store.getTargetFontApplyMode(packageName)
            if (store.isTargetDpisEnabled(packageName) &&
                fontScalePercent != null && fontScalePercent > 0 &&
                shouldPublishForceFontOnRecovery(store, packageName, fontMode)
            ) {
                publishForceFontTargetAsync(packageName, fontScalePercent)
            }
        }
    }

    private fun publishPropertyWithRoot(fontProperty: String, fontScalePercent: Int) {
        runRootCommand(buildPublishCommand(fontProperty, fontScalePercent))
    }

    private fun clearPropertiesWithRoot(
        fontProperty: String,
        forceFontProperty: String,
        rustBinaryProperty: String,
    ) {
        runRootCommand(
            "setprop ${shellQuote(fontProperty)} 0; " +
                    "setprop ${shellQuote(forceFontProperty)} 0; " +
                    "setprop ${shellQuote(rustBinaryProperty)} 0",
        )
    }

    private fun clearFontTargets(store: DpisConfigStore, packages: LinkedHashSet<String?>) {
        val command = StringBuilder()
        for (rawPackageName in packages) {
            val packageName = rawPackageName ?: continue
            if (packageName.isBlank()) continue
            // forcefont.* is shared by HyperOS native font replacement and
            // legacy field-rewrite config. Disabling only the HyperOS
            // native path must not erase an active legacy field rewrite.
            val preserveForceFont = shouldPreserveCompatForceFont(store, packageName)
            if (!preserveForceFont) {
                HyperOsFlutterFontBridge.clearTarget(packageName)
            } else {
                HyperOsFlutterFontBridge.clearNativeTarget(packageName)
            }
            appendClearCommand(
                command,
                HyperOsFlutterFontBridge.propertyNameForPackage(packageName)
            )
            if (!preserveForceFont) {
                appendClearCommand(
                    command,
                    HyperOsFlutterFontBridge.forcePropertyNameForPackage(packageName)
                )
            }
            appendClearCommand(
                command,
                HyperOsFlutterFontBridge.rustBinaryPropertyNameForPackage(packageName)
            )
        }
        if (command.isNotEmpty()) runRootCommand(command.toString())
    }

    private fun appendClearCommand(command: StringBuilder, property: String) {
        if (command.isNotEmpty()) command.append("; ")
        command.append("setprop ").append(shellQuote(property)).append(" 0")
    }

    @JvmStatic
    fun buildPublishCommandForTest(fontProperty: String, fontScalePercent: Int): String =
        buildPublishCommand(fontProperty, fontScalePercent)

    @JvmStatic
    fun shouldPreserveCompatForceFontForTest(
        store: DpisConfigStore?,
        packageName: String?
    ): Boolean =
        shouldPreserveCompatForceFont(store, packageName)

    @JvmStatic
    fun shouldPublishForceFontOnRecoveryForTest(
        store: DpisConfigStore?,
        packageName: String?,
        fontMode: String?,
    ): Boolean = shouldPublishForceFontOnRecovery(store, packageName, fontMode)

    private fun shouldPublishForceFontOnRecovery(
        store: DpisConfigStore?,
        packageName: String?,
        fontMode: String?,
    ): Boolean {
        if (store == null) return false
        val normalizedMode = FontApplyMode.normalize(fontMode)
        if (FontApplyMode.FIELD_REWRITE == normalizedMode) return true
        return FontHookDomainDecision.isHyperOsNativeFlutterEnabled(store, packageName) &&
                FontApplyMode.isEnabled(normalizedMode)
    }

    private fun shouldPreserveCompatForceFont(
        store: DpisConfigStore?,
        packageName: String?,
    ): Boolean {
        if (store == null || packageName.isNullOrBlank() || !store.isTargetDpisEnabled(packageName)) {
            return false
        }
        val fontScalePercent = store.getTargetFontScalePercent(packageName)
        return fontScalePercent != null && fontScalePercent > 0 &&
                FontApplyMode.FIELD_REWRITE == store.getTargetFontApplyMode(packageName)
    }

    private fun buildPublishCommand(fontProperty: String, fontScalePercent: Int): String =
        "setprop ${shellQuote(fontProperty)} ${shellQuote(fontScalePercent.toString())}"

    private fun runRootCommand(command: String) = RootCommandRunner.run(command)

    @JvmStatic
    fun shellQuoteForTest(value: String?): String = shellQuote(value)

    private fun shellQuote(value: String?): String =
        if (value.isNullOrEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
