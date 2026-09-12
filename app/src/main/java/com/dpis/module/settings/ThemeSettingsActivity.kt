package com.dpis.module.settings

import android.os.Bundle
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.presentation.installThemeSettings

class ThemeSettingsActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installThemeSettings()
    }
}
