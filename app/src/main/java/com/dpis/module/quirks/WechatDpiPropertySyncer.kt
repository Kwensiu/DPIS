package com.dpis.module.quirks

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.transport.RootCommandRunner

object WechatDpiPropertySyncer {
    @JvmStatic
    fun publishDpiAsync(packageName: String?, dpi: Int?) {
        if (!WechatDpiConfig.appliesTo(packageName)) return
        Thread(
            { RootCommandRunner.run(buildDpiCommand(packageName, dpi)) },
            "DPIS-wechat-dpi-publisher"
        )
            .apply { isDaemon = true }
            .start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        val command = buildSyncCommand(store)
        if (command.isEmpty()) return
        Thread({ RootCommandRunner.run(command) }, "DPIS-wechat-dpi-syncer")
            .apply { isDaemon = true }
            .start()
    }

    @JvmStatic
    fun buildSyncCommandForTest(store: DpisConfigStore?): String = buildSyncCommand(store)

    @JvmStatic
    fun buildDpiCommandForTest(packageName: String?, dpi: Int?): String =
        buildDpiCommand(packageName, dpi)

    private fun buildSyncCommand(store: DpisConfigStore?): String {
        if (store == null) return ""
        val dpi = if (store.isTargetDpisEnabled(WechatDpiConfig.PACKAGE_NAME)) {
            store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME)
        } else null
        return buildDpiCommand(WechatDpiConfig.PACKAGE_NAME, dpi)
    }

    private fun buildDpiCommand(packageName: String?, dpi: Int?): String {
        val value = WechatDpiConfig.normalize(dpi)?.toString() ?: "0"
        return buildSetCommandPair(
            WechatDpiPropertyBridge.propertyNameForPackage(packageName),
            WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName),
            value,
        )
    }

    private fun buildSetCommand(property: String, value: String): String =
        "setprop ${shellQuote(property)} ${shellQuote(value)}"

    private fun buildSetCommandPair(
        property: String,
        persistentProperty: String,
        value: String
    ): String =
        "${buildSetCommand(property, value)}; ${buildSetCommand(persistentProperty, value)}"

    private fun shellQuote(value: String?): String =
        if (value.isNullOrEmpty()) "''" else "'${value.replace("'", "'\\''")}'"
}
