package com.dpis.module.appconfig

import android.view.View
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogBinder.ModeToggle
import com.google.android.material.textview.MaterialTextView

/** Landscape detail pane viewport/font mode-toggle view groups. */
internal object LandAppDetailModeToggles {
    fun viewport(root: View): ModeToggle = ModeToggle(
        root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
        root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
        root.findViewById<MaterialTextView>(R.id.land_detail_viewport_mode_scale_label),
        root.findViewById<MaterialTextView>(R.id.land_detail_viewport_mode_width_label),
    )

    fun font(root: View): ModeToggle = ModeToggle(
        root.findViewById(R.id.land_detail_font_mode_toggle_button),
        root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
        root.findViewById<MaterialTextView>(R.id.land_detail_font_mode_system_label),
        root.findViewById<MaterialTextView>(R.id.land_detail_font_mode_compat_label),
    )
}
