package com.dpis.module.settings

import android.os.Bundle
import com.dpis.module.settings.presentation.installExperimentalSettings

class ExperimentalSettingsActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installExperimentalSettings()
    }
}
