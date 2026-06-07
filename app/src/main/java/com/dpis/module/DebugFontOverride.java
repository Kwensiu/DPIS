package com.dpis.module;

final class DebugFontOverride {
    final boolean forceFlutterSettings;
    final boolean flutterSettingsOnly;
    final boolean disableTextViewAbsoluteRewrite;
    final boolean disableActivityThreadFont;

    private DebugFontOverride(boolean forceFlutterSettings,
                              boolean flutterSettingsOnly,
                              boolean disableTextViewAbsoluteRewrite,
                              boolean disableActivityThreadFont) {
        this.forceFlutterSettings = forceFlutterSettings || flutterSettingsOnly;
        this.flutterSettingsOnly = flutterSettingsOnly;
        this.disableTextViewAbsoluteRewrite = disableTextViewAbsoluteRewrite;
        this.disableActivityThreadFont = disableActivityThreadFont;
    }

    static DebugFontOverride none() {
        return new DebugFontOverride(false, false, false, false);
    }

    static DebugFontOverride of(boolean forceFlutterSettings, boolean flutterSettingsOnly) {
        return new DebugFontOverride(forceFlutterSettings, flutterSettingsOnly, false, false);
    }

    static DebugFontOverride of(boolean forceFlutterSettings,
                                boolean flutterSettingsOnly,
                                boolean disableTextViewAbsoluteRewrite) {
        return of(forceFlutterSettings, flutterSettingsOnly,
                disableTextViewAbsoluteRewrite, false);
    }

    static DebugFontOverride of(boolean forceFlutterSettings,
                                boolean flutterSettingsOnly,
                                boolean disableTextViewAbsoluteRewrite,
                                boolean disableActivityThreadFont) {
        return new DebugFontOverride(
                forceFlutterSettings,
                flutterSettingsOnly,
                disableTextViewAbsoluteRewrite,
                disableActivityThreadFont);
    }
}
