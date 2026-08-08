package com.dpis.module.settings

import android.os.Bundle
import com.dpis.module.LocalizedActivity
import com.dpis.module.ui.compose.SupportActivityContent

class ThemeSettingsActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupportActivityContent.installThemeSettings(this)
    }
}
