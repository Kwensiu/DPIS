package com.dpis.module.runtime.font

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import kotlin.concurrent.Volatile

/** Field-rewrite factor helper retained for installers that do not hook Paint.setTextSize. */
object PaintTextSizeFallbackHookInstaller {
    @Volatile
    private var installedPid = -1

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @JvmStatic
    fun resolveFieldRewriteFactor(store: DpisConfigStore?, packageName: String): Float {
        if (store == null) {
            return 1.0f
        }
        val mode = store.getTargetFontApplyMode(packageName)
        if (FontApplyMode.FIELD_REWRITE != mode) {
            return 1.0f
        }
        val percent = store.getTargetFontScalePercent(packageName)
        if (percent == null || percent <= 0 || percent == 100) {
            return 1.0f
        }
        return percent / 100.0f
    }
}
