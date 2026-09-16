package com.dpis.module.tools.presentation

import android.content.Context
import android.provider.Settings
import com.dpis.module.tools.SystemFontScaleToolPresenter
import com.dpis.module.tools.SystemFontScaleToolState

class SystemFontScaleSettingsGateway(
    private val context: Context,
) : SystemFontScaleToolPresenter.Gateway {
    override fun readPercent(): Int? = try {
        val scale = Settings.System.getFloat(
            context.contentResolver,
            Settings.System.FONT_SCALE,
            SystemFontScaleToolState.scaleFromPercent(
                SystemFontScaleToolState.DEFAULT_PERCENT,
            ),
        )
        SystemFontScaleToolState.percentFromScale(scale)
    } catch (_: RuntimeException) {
        null
    }

    override fun canWrite(): Boolean = Settings.System.canWrite(context)

    override fun writePercent(percent: Int): Boolean = Settings.System.putFloat(
        context.contentResolver,
        Settings.System.FONT_SCALE,
        SystemFontScaleToolState.scaleFromPercent(percent),
    )
}
