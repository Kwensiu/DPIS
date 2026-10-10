package com.dpis.module.fonts.hookdomain

object FontHookArbitration {
    @JvmStatic
    fun resolveDomainPlan(fontScaleEnabled: Boolean, fieldRewriteEnabled: Boolean): FontDomainPlan =
        resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled, false, false)

    @JvmStatic
    fun resolveDomainPlan(
        fontScaleEnabled: Boolean,
        fieldRewriteEnabled: Boolean,
        hyperOsNativeFlutterEnabled: Boolean
    ): FontDomainPlan =
        resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled, false, hyperOsNativeFlutterEnabled)

    @JvmStatic
    fun resolveDomainPlan(
        fontScaleEnabled: Boolean,
        fieldRewriteEnabled: Boolean,
        flutterSettingsEnabled: Boolean,
        hyperOsNativeFlutterEnabled: Boolean
    ): FontDomainPlan {
        if (!fontScaleEnabled) return FontDomainPlan.fontScaleDisabled()
        if (!fieldRewriteEnabled) return FontDomainPlan.semanticFontDomainPlan(
            flutterSettingsEnabled,
            hyperOsNativeFlutterEnabled
        )
        return FontDomainPlan.fieldRewriteDomainPlan(
            flutterSettingsEnabled,
            hyperOsNativeFlutterEnabled
        )
    }

    class FontDomainPlan(
        @JvmField val resourcesFontEnabled: Boolean,
        @JvmField val webViewTextZoomEnabled: Boolean,
        @JvmField val textViewHooksEnabled: Boolean,
        @JvmField val textViewSpRewriteEnabled: Boolean,
        @JvmField val textViewAbsoluteRewriteEnabled: Boolean,
        @JvmField val textViewCurrentPxFallbackEnabled: Boolean,
        @JvmField val paintFallbackEnabled: Boolean,
        @JvmField val flutterSettingsEnabled: Boolean,
        @JvmField val hyperOsNativeFlutterEnabled: Boolean,
        @JvmField val genericNativeFlutterEnabled: Boolean,
        @JvmField val reason: String,
    ) {
        companion object {
            @JvmStatic
            fun fontScaleDisabled() = FontDomainPlan(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "font-scale-disabled"
            )

            @JvmStatic
            fun semanticFontDomainPlan(
                flutterSettingsEnabled: Boolean,
                hyperOsNativeFlutterEnabled: Boolean
            ) = FontDomainPlan(
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                flutterSettingsEnabled,
                hyperOsNativeFlutterEnabled,
                false,
                "semantic-font-domain-plan"
            )

            @JvmStatic
            fun fieldRewriteDomainPlan(
                flutterSettingsEnabled: Boolean,
                hyperOsNativeFlutterEnabled: Boolean
            ) = FontDomainPlan(
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                flutterSettingsEnabled,
                hyperOsNativeFlutterEnabled,
                false,
                "field-rewrite-domain-plan"
            )
        }
    }
}
