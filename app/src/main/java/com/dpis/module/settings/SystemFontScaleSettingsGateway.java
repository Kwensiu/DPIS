package com.dpis.module.settings;

import android.content.Context;
import android.provider.Settings;

public final class SystemFontScaleSettingsGateway {
    public Integer readPercent(Context context) {
        try {
            float scale = Settings.System.getFloat(
                    context.getContentResolver(),
                    Settings.System.FONT_SCALE,
                    SystemFontScaleToolState.scaleFromPercent(
                            SystemFontScaleToolState.DEFAULT_PERCENT));
            return SystemFontScaleToolState.percentFromScale(scale);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean canWrite(Context context) {
        return Settings.System.canWrite(context);
    }

    public boolean writePercent(Context context, int percent) {
        return Settings.System.putFloat(
                context.getContentResolver(),
                Settings.System.FONT_SCALE,
                SystemFontScaleToolState.scaleFromPercent(percent));
    }
}
