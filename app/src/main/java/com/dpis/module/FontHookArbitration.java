package com.dpis.module;

public final class FontHookArbitration {
    private FontHookArbitration() {
    }

    public static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled) {
        return resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled, false, false);
    }

    public static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled,
                                            boolean hyperOsNativeFlutterEnabled) {
        return resolveDomainPlan(fontScaleEnabled, fieldRewriteEnabled,
                false, hyperOsNativeFlutterEnabled);
    }

    public static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
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

    public static final class FontDomainPlan {
        public final boolean resourcesFontEnabled;
        public final boolean webViewTextZoomEnabled;
        public final boolean textViewHooksEnabled;
        public final boolean textViewSpRewriteEnabled;
        public final boolean textViewAbsoluteRewriteEnabled;
        public final boolean textViewCurrentPxFallbackEnabled;
        public final boolean paintFallbackEnabled;
        public final boolean flutterSettingsEnabled;
        public final boolean hyperOsNativeFlutterEnabled;
        public final boolean genericNativeFlutterEnabled;
        public final String reason;

        public FontDomainPlan(boolean resourcesFontEnabled,
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

        public static FontDomainPlan fontScaleDisabled() {
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

        public static FontDomainPlan semanticFontDomainPlan(boolean flutterSettingsEnabled,
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

        public static FontDomainPlan fieldRewriteDomainPlan(boolean flutterSettingsEnabled,
                                                     boolean hyperOsNativeFlutterEnabled) {
            return new FontDomainPlan(
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
                    "field-rewrite-domain-plan");
        }
    }
}
