package com.dpis.module.fonts.hookdomain

/** Builds shell commands that publish per-package font hook-domain overrides. */
internal object FontHookDomainPropertyCommands {
    @JvmStatic
    fun buildPublish(packageName: String, enabledKnownDomains: Set<String>, userId: Int): String {
        val value =
            FontHookDomainPropertyBridge.encodeOverrideValue(packageName, enabledKnownDomains)
        return buildSetCommand(
            FontHookDomainPropertyBridge.propertyNameForPackage(packageName, userId),
            value
        ) +
                "; " + buildSetCommand(
            FontHookDomainPropertyBridge.persistentPropertyNameForPackage(
                packageName, userId
            ), value
        )
    }

    @JvmStatic
    fun buildClear(packageName: String, userId: Int): String =
        buildSetCommand(
            FontHookDomainPropertyBridge.propertyNameForPackage(packageName, userId),
            "0"
        ) +
                "; " + buildSetCommand(
            FontHookDomainPropertyBridge.persistentPropertyNameForPackage(
                packageName, userId
            ), "0"
        )

    private fun buildSetCommand(property: String, value: String) =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun shellQuote(value: String): String =
        if (value.isEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
