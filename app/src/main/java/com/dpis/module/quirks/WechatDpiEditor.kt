package com.dpis.module.quirks

import com.dpis.module.DpisApplication
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore

/** Persists and publishes WeChat DPI without owning the editor View. */
object WechatDpiEditor {
    @JvmStatic
    fun isInputValid(rawValue: String?): Boolean = WechatDpiConfig.isInputValid(rawValue)

    @JvmStatic
    fun save(
        rawValue: String?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return true
        if (store == null || !isInputValid(rawValue)) return false
        val dpi = WechatDpiConfig.parseOrNull(rawValue)
        val saved = store.setWechatDpi(packageName, dpi)
        if (saved) {
            WechatDpiPropertySyncer.publishDpiAsync(packageName, if (dpisEnabled) dpi else null)
        }
        return saved
    }

    @JvmStatic
    fun publishForDpisState(packageName: String?, dpisEnabled: Boolean) {
        if (!WechatDpiConfig.appliesTo(packageName)) return
        val store = DpisApplication.getActiveHookConfigStore(null)
        val dpi = if (store != null && dpisEnabled) store.getWechatDpi(packageName) else null
        WechatDpiPropertySyncer.publishDpiAsync(packageName, dpi)
    }
}
