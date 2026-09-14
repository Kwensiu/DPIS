package com.dpis.module.hyperos

import com.dpis.module.FakePrefs
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperOsNativeProxyApplyPolicyTest {
    @Test
    fun isCandidateUsesListFlagOrMetadataLookup() {
        assertFalse(HyperOsNativeProxyApplyPolicy.isCandidate(null) { true })
        assertTrue(HyperOsNativeProxyApplyPolicy.isCandidate(item(candidate = true)) { false })
        assertTrue(HyperOsNativeProxyApplyPolicy.isCandidate(item(candidate = false)) { true })
        assertFalse(HyperOsNativeProxyApplyPolicy.isCandidate(item(candidate = false)) { false })
    }

    @Test
    fun hasActiveStoredConfigUsesViewportFontOrWechatNotTypeface() {
        val store = DpisConfigStore(FakePrefs())
        val packageName = "com.example.app"

        assertFalse(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, packageName))

        assertTrue(store.setTargetTypefaceId(packageName, "serif"))
        assertFalse(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, packageName))

        assertTrue(store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(90000)))
        assertTrue(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, packageName))
        assertTrue(store.clearTargetViewportValue(packageName))

        assertTrue(store.setTargetFontScalePercent(packageName, 140))
        assertTrue(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, packageName))
        assertTrue(store.clearTargetFontScalePercent(packageName))

        assertTrue(store.setWechatDpi(packageName, 480))
        assertFalse(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, packageName))

        val wechatPackageName = WechatDpiConfig.PACKAGE_NAME
        assertTrue(store.setWechatDpi(wechatPackageName, 480))
        assertTrue(HyperOsNativeProxyApplyPolicy.hasActiveStoredConfig(store, wechatPackageName))
    }

    @Test
    fun shouldApplyRequiresEnabledDpisAndActiveStoredConfig() {
        val store = DpisConfigStore(FakePrefs())
        val packageName = "com.example.app"
        assertTrue(store.setTargetDpisEnabled(packageName, false))
        assertTrue(store.setTargetFontScalePercent(packageName, 140))

        assertFalse(HyperOsNativeProxyApplyPolicy.shouldApply(null, packageName))
        assertFalse(HyperOsNativeProxyApplyPolicy.shouldApply(store, null))
        assertFalse(HyperOsNativeProxyApplyPolicy.shouldApply(store, packageName))

        assertTrue(store.setTargetDpisEnabled(packageName, true))
        assertTrue(HyperOsNativeProxyApplyPolicy.shouldApply(store, packageName))
    }

    @Test
    fun shouldPrepareForRestartRequiresCandidateAndApply() {
        val store = DpisConfigStore(FakePrefs())
        val packageName = "com.example.app"
        assertTrue(store.setTargetFontScalePercent(packageName, 140))
        assertTrue(store.setTargetDpisEnabled(packageName, true))

        assertFalse(
            HyperOsNativeProxyApplyPolicy.shouldPrepareForRestart(
                item(candidate = false),
                store,
            ) { false },
        )
        assertTrue(
            HyperOsNativeProxyApplyPolicy.shouldPrepareForRestart(
                item(candidate = true),
                store,
            ) { false },
        )
        assertTrue(
            HyperOsNativeProxyApplyPolicy.shouldPrepareForRestart(
                item(candidate = false),
                store,
            ) { true },
        )

        assertTrue(store.setTargetDpisEnabled(packageName, false))
        assertFalse(
            HyperOsNativeProxyApplyPolicy.shouldPrepareForRestart(
                item(candidate = true),
                store,
            ) { false },
        )
    }

    private fun item(candidate: Boolean) = AppListItem(
        "Example",
        "com.example.app",
        false,
        true,
        null,
        ViewportApplyMode.OFF,
        null,
        FontApplyMode.OFF,
        null,
        true,
        false,
        candidate,
        null,
    )
}
