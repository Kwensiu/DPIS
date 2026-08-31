package com.dpis.module.appconfig

import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView

/** Bound view handles for one app-config dialog instance. */
open class AppConfigDialogViews(
    @JvmField val iconView: ImageView,
    @JvmField val titleView: MaterialTextView,
    @JvmField val packageView: MaterialTextView,
    @JvmField val statusView: MaterialTextView,
    @JvmField val viewportInputLayout: TextInputLayout,
    @JvmField val viewportInputView: TextInputEditText,
    @JvmField val fontInputLayout: TextInputLayout,
    @JvmField val fontInputView: TextInputEditText,
    @JvmField val viewportModeToggle: AppConfigDialogBinder.ModeToggle,
    @JvmField val fontModeToggle: AppConfigDialogBinder.ModeToggle,
    @JvmField val typefaceSelectorButton: MaterialButton,
    @JvmField val scopeButton: MaterialButton,
    @JvmField val startButton: MaterialButton,
    @JvmField val restartButton: MaterialButton,
    @JvmField val stopButton: MaterialButton,
    @JvmField val dpisToggleButton: MaterialButton,
    @JvmField val fontHookDomainsButton: MaterialButton,
    @JvmField val disableButton: MaterialButton,
    @JvmField val saveButton: MaterialButton,
    @JvmField val feedbackDiagnosticButton: MaterialButton,
)
