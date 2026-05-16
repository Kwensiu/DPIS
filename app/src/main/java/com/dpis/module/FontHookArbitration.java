package com.dpis.module;

final class FontHookArbitration {
    private FontHookArbitration() {
    }

    static FontDomainPlan resolveDomainPlan(boolean fontScaleEnabled,
                                            boolean fieldRewriteEnabled) {
        if (!fontScaleEnabled) {
            return new FontDomainPlan(
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "font-scale-disabled");
        }
        if (!fieldRewriteEnabled) {
            return new FontDomainPlan(
                    false,
                    true,
                    false,
                    false,
                    false,
                    false,
                    true,
                    "semantic-font-domain-plan");
        }
        return new FontDomainPlan(
                true,
                true,
                true,
                false,
                false,
                false,
                true,
                "field-rewrite-domain-plan");
    }

    static final class FontDomainPlan {
        final boolean resourcesFontEnabled;
        final boolean webViewTextZoomEnabled;
        final boolean textViewHooksEnabled;
        final boolean textViewSpRewriteEnabled;
        final boolean textViewAbsoluteRewriteEnabled;
        final boolean paintFallbackEnabled;
        final boolean flutterSettingsEnabled;
        final String reason;

        FontDomainPlan(boolean resourcesFontEnabled,
                       boolean webViewTextZoomEnabled,
                       boolean textViewHooksEnabled,
                       boolean textViewSpRewriteEnabled,
                       boolean textViewAbsoluteRewriteEnabled,
                       boolean paintFallbackEnabled,
                       boolean flutterSettingsEnabled,
                       String reason) {
            this.resourcesFontEnabled = resourcesFontEnabled;
            this.webViewTextZoomEnabled = webViewTextZoomEnabled;
            this.textViewHooksEnabled = textViewHooksEnabled;
            this.textViewSpRewriteEnabled = textViewSpRewriteEnabled;
            this.textViewAbsoluteRewriteEnabled = textViewAbsoluteRewriteEnabled;
            this.paintFallbackEnabled = paintFallbackEnabled;
            this.flutterSettingsEnabled = flutterSettingsEnabled;
            this.reason = reason;
        }
    }
}
