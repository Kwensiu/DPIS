package com.dpis.module.quirks

import android.os.Process
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import com.dpis.module.runtime.transport.RootCommandRunner

object WechatDpiPropertySyncer {
    @JvmStatic
    fun publishDpiAsync(packageName: String?, dpi: Int?) {
        if (!WechatDpiConfig.appliesTo(packageName)) return
        val userId = currentUserId()
        Thread(
            { RootCommandRunner.run(buildDpiCommand(packageName, dpi, userId)) },
            "DPIS-wechat-dpi-publisher"
        )
            .apply { isDaemon = true }
            .start()
    }

    @JvmStatic
    fun syncConfiguredTargetsAsync(store: DpisConfigStore?) {
        if (store == null) return
        val userId = currentUserId()
        val command = buildSyncCommand(store, userId)
        if (command.isEmpty()) return
        Thread({ RootCommandRunner.run(command) }, "DPIS-wechat-dpi-syncer")
            .apply { isDaemon = true }
            .start()
    }

    @JvmStatic
    fun buildSyncCommandForTest(store: DpisConfigStore?, userId: Int): String =
        buildSyncCommand(store, userId)

    @JvmStatic
    fun buildDpiCommandForTest(packageName: String?, dpi: Int?, userId: Int): String =
        buildDpiCommand(packageName, dpi, userId)

    private fun currentUserId(): Int = runCatching {
        PackageUidResolver.userIdFromUid(Process.myUid())
    }.getOrNull() ?: 0

    private fun buildSyncCommand(store: DpisConfigStore?, userId: Int): String {
        if (store == null) return ""
        val dpi = if (store.isTargetDpisEnabled(WechatDpiConfig.PACKAGE_NAME)) {
            store.getWechatDpi(WechatDpiConfig.PACKAGE_NAME)
        } else null
        return buildDpiCommand(WechatDpiConfig.PACKAGE_NAME, dpi, userId)
    }

    private fun buildDpiCommand(packageName: String?, dpi: Int?, userId: Int): String {
        val value = WechatDpiConfig.normalize(dpi)?.toString() ?: "0"
        return buildSetCommandPair(
            WechatDpiPropertyBridge.propertyNameForPackage(packageName, userId),
            WechatDpiPropertyBridge.persistentPropertyNameForPackage(packageName, userId),
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
