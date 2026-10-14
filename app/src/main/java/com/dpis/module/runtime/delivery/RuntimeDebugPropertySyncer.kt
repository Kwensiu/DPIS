package com.dpis.module.runtime.delivery

import com.dpis.module.runtime.transport.RootCommandRunner

object RuntimeDebugPropertySyncer {
    @JvmStatic
    fun publishAsync(globalLogEnabled: Boolean, fontDebugOverlayEnabled: Boolean) {
        val publisherThread = Thread(
            { runRootCommand(buildPublishCommand(globalLogEnabled, fontDebugOverlayEnabled)) },
            "DPIS-runtime-debug-property-publisher",
        )
        publisherThread.isDaemon = true
        publisherThread.start()
    }

    @JvmStatic
    fun buildPublishCommandForTest(
        globalLogEnabled: Boolean,
        fontDebugOverlayEnabled: Boolean,
    ): String = buildPublishCommand(globalLogEnabled, fontDebugOverlayEnabled)

    private fun buildPublishCommand(
        globalLogEnabled: Boolean,
        fontDebugOverlayEnabled: Boolean,
    ): String {
        val globalLogValue = if (globalLogEnabled) "1" else "0"
        val overlayValue = if (fontDebugOverlayEnabled) "1" else "0"
        return buildSetCommand(
            RuntimeDebugPropertyBridge.globalLogPropertyName(),
            globalLogValue,
        ) + "; " + buildSetCommand(
            RuntimeDebugPropertyBridge.persistentGlobalLogPropertyName(),
            globalLogValue,
        ) + "; " + buildSetCommand(
            RuntimeDebugPropertyBridge.fontDebugOverlayPropertyName(),
            overlayValue,
        ) + "; " + buildSetCommand(
            RuntimeDebugPropertyBridge.persistentFontDebugOverlayPropertyName(),
            overlayValue,
        )
    }

    private fun buildSetCommand(property: String, value: String): String =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun runRootCommand(command: String) {
        RootCommandRunner.run(command)
    }

    private fun shellQuote(value: String?): String {
        if (value.isNullOrEmpty()) {
            return "''"
        }
        return "'${value.replace("'", "'\\''")}'"
    }
}
