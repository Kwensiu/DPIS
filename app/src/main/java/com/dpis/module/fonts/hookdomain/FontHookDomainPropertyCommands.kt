package com.dpis.module.fonts.hookdomain

/** Builds shell commands that publish per-package font hook-domain overrides. */
internal object FontHookDomainPropertyCommands {
    @JvmStatic
    fun buildPublish(packageName: String, enabledKnownDomains: Set<String>): String {
        val value =
            FontHookDomainPropertyBridge.encodeOverrideValue(packageName, enabledKnownDomains)
        return buildSetCommand(
            FontHookDomainPropertyBridge.propertyNameForPackage(packageName),
            value
        ) +
                "; " + buildSetCommand(
            FontHookDomainPropertyBridge.persistentPropertyNameForPackage(
                packageName
            ), value
        )
    }

    @JvmStatic
    fun buildClear(packageName: String): String =
        buildSetCommand(FontHookDomainPropertyBridge.propertyNameForPackage(packageName), "0") +
                "; " + buildSetCommand(
            FontHookDomainPropertyBridge.persistentPropertyNameForPackage(
                packageName
            ), "0"
        )

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun shellQuote(value: String): String =
        if (value.isEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
