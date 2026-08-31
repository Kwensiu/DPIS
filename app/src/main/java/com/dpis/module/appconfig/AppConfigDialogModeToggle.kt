package com.dpis.module.appconfig

import android.view.View
import com.google.android.material.textview.MaterialTextView

/** View handles shared by the viewport and font mode toggles. */
open class AppConfigDialogModeToggle(
    @JvmField val container: View,
    @JvmField val thumb: View,
    @JvmField val emulationLabel: MaterialTextView,
    @JvmField val replaceLabel: MaterialTextView,
) {
    @JvmField var emulationActive: Boolean = false
}
