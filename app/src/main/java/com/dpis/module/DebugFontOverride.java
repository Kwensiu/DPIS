package com.dpis.module;

public final class DebugFontOverride {
    public final boolean forceFlutterSettings;
    public final boolean flutterSettingsOnly;
    public final boolean disableTextViewAbsoluteRewrite;
    public final boolean disableActivityThreadFont;

    private DebugFontOverride(boolean forceFlutterSettings,
                              boolean flutterSettingsOnly,
                              boolean disableTextViewAbsoluteRewrite,
                              boolean disableActivityThreadFont) {
        this.forceFlutterSettings = forceFlutterSettings || flutterSettingsOnly;
        this.flutterSettingsOnly = flutterSettingsOnly;
        this.disableTextViewAbsoluteRewrite = disableTextViewAbsoluteRewrite;
        this.disableActivityThreadFont = disableActivityThreadFont;
    }

    public static DebugFontOverride none() {
        return new DebugFontOverride(false, false, false, false);
    }

    public static DebugFontOverride of(boolean forceFlutterSettings, boolean flutterSettingsOnly) {
        return new DebugFontOverride(forceFlutterSettings, flutterSettingsOnly, false, false);
    }

    public static DebugFontOverride of(boolean forceFlutterSettings,
                                boolean flutterSettingsOnly,
                                boolean disableTextViewAbsoluteRewrite) {
        return of(forceFlutterSettings, flutterSettingsOnly,
                disableTextViewAbsoluteRewrite, false);
    }

    public static DebugFontOverride of(boolean forceFlutterSettings,
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
