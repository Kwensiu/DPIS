package com.dpis.module;

final class DebugFontOverride {
    final boolean forceFlutterSettings;
    final boolean flutterSettingsOnly;
    final boolean disableTextViewAbsoluteRewrite;

    private DebugFontOverride(boolean forceFlutterSettings,
                              boolean flutterSettingsOnly,
                              boolean disableTextViewAbsoluteRewrite) {
        this.forceFlutterSettings = forceFlutterSettings || flutterSettingsOnly;
        this.flutterSettingsOnly = flutterSettingsOnly;
        this.disableTextViewAbsoluteRewrite = disableTextViewAbsoluteRewrite;
    }

    static DebugFontOverride none() {
        return new DebugFontOverride(false, false, false);
    }

    static DebugFontOverride of(boolean forceFlutterSettings, boolean flutterSettingsOnly) {
        return new DebugFontOverride(forceFlutterSettings, flutterSettingsOnly, false);
    }

    static DebugFontOverride of(boolean forceFlutterSettings,
                                boolean flutterSettingsOnly,
                                boolean disableTextViewAbsoluteRewrite) {
        return new DebugFontOverride(
                forceFlutterSettings,
                flutterSettingsOnly,
                disableTextViewAbsoluteRewrite);
    }
}
