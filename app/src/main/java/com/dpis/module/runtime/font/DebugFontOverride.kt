package com.dpis.module.runtime.font

class DebugFontOverride private constructor(
    @JvmField val forceFlutterSettings: Boolean,
    @JvmField val flutterSettingsOnly: Boolean,
    @JvmField val disableTextViewAbsoluteRewrite: Boolean,
    @JvmField val disableActivityThreadFont: Boolean,
) {
    companion object {
        @JvmStatic
        fun none() = DebugFontOverride(false, false, false, false)

        @JvmStatic
        fun of(forceFlutterSettings: Boolean, flutterSettingsOnly: Boolean) =
            DebugFontOverride(
                forceFlutterSettings || flutterSettingsOnly,
                flutterSettingsOnly,
                false,
                false,
            )

        @JvmStatic
        fun of(
            forceFlutterSettings: Boolean,
            flutterSettingsOnly: Boolean,
            disableTextViewAbsoluteRewrite: Boolean,
        ) = of(
            forceFlutterSettings,
            flutterSettingsOnly,
            disableTextViewAbsoluteRewrite,
            false,
        )

        @JvmStatic
        fun of(
            forceFlutterSettings: Boolean,
            flutterSettingsOnly: Boolean,
            disableTextViewAbsoluteRewrite: Boolean,
            disableActivityThreadFont: Boolean,
        ) = DebugFontOverride(
            forceFlutterSettings || flutterSettingsOnly,
            flutterSettingsOnly,
            disableTextViewAbsoluteRewrite,
            disableActivityThreadFont,
        )
    }
}
