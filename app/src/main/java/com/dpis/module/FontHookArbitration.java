package com.dpis.module;

final class FontHookArbitration {
    private FontHookArbitration() {
    }

    static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled) {
        return resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled, false, false);
    }

    static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled,
                                            boolean hyperOsNativeFlutterEnabled) {
        return resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled,
                false, hyperOsNativeFlutterEnabled);
    }

    static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled,
                                            boolean flutterSettingsEnabled,
                                            boolean hyperOsNativeFlutterEnabled) {
        if (!fontScaleEnabled) {
            return FontDomainPlan.fontScaleDisabled();
        }
        if (!fieldRewriteEnabled) {
            return FontDomainPlan.semanticFontDomainPlan(
                    flutterSettingsEnabled,
                    hyperOsNativeFlutterEnabled);
        }
        return FontDomainPlan.fieldRewriteDomainPlan(
                flutterSettingsEnabled,
                hyperOsNativeFlutterEnabled);
    }

    static final class FontDomainPlan {
        final boolean resourcesFontEnabled;
        final boolean webViewTextZoomEnabled;
        final boolean textViewHooksEnabled;
        final boolean textViewSpRewriteEnabled;
        final boolean textViewAbsoluteRewriteEnabled;
        final boolean textViewCurrentPxFallbackEnabled;
        final boolean paintFallbackEnabled;
        final boolean flutterSettingsEnabled;
        final boolean hyperOsNativeFlutterEnabled;
        final boolean genericNativeFlutterEnabled;
        final String reason;

        FontDomainPlan(boolean resourcesFontEnabled,
                       boolean webViewTextZoomEnabled,
                       boolean textViewHooksEnabled,
                       boolean textViewSpRewriteEnabled,
                       boolean textViewAbsoluteRewriteEnabled,
                       boolean textViewCurrentPxFallbackEnabled,
                       boolean paintFallbackEnabled,
                       boolean flutterSettingsEnabled,
                       boolean hyperOsNativeFlutterEnabled,
                       boolean genericNativeFlutterEnabled,
                       String reason) {
            this.resourcesFontEnabled = resourcesFontEnabled;
            this.webViewTextZoomEnabled = webViewTextZoomEnabled;
            this.textViewHooksEnabled = textViewHooksEnabled;
            this.textViewSpRewriteEnabled = textViewSpRewriteEnabled;
            this.textViewAbsoluteRewriteEnabled = textViewAbsoluteRewriteEnabled;
            this.textViewCurrentPxFallbackEnabled = textViewCurrentPxFallbackEnabled;
            this.paintFallbackEnabled = paintFallbackEnabled;
            this.flutterSettingsEnabled = flutterSettingsEnabled;
            this.hyperOsNativeFlutterEnabled = hyperOsNativeFlutterEnabled;
            this.genericNativeFlutterEnabled = genericNativeFlutterEnabled;
            this.reason = reason;
        }

        static FontDomainPlan fontScaleDisabled() {
            return new FontDomainPlan(
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
                    "font-scale-disabled");
        }

        static FontDomainPlan semanticFontDomainPlan(boolean flutterSettingsEnabled,
                                                     boolean hyperOsNativeFlutterEnabled) {
            return new FontDomainPlan(
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
                    "semantic-font-domain-plan");
        }

        static FontDomainPlan fieldRewriteDomainPlan(boolean flutterSettingsEnabled,
                                                     boolean hyperOsNativeFlutterEnabled) {
            return new FontDomainPlan(
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
                    "field-rewrite-domain-plan");
        }
    }
}
