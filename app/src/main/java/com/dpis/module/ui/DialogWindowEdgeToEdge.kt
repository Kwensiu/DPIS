package com.dpis.module.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Build

/** Applies the shared window contract for dialogs and bottom sheets. */
object DialogWindowEdgeToEdge {
    @JvmStatic
    fun apply(dialog: Dialog) {
        val window = dialog.window ?: return
        window.setNavigationBarColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }
    }
}
