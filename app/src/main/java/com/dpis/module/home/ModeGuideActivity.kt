package com.dpis.module.home

import android.os.Bundle
import com.dpis.module.home.presentation.installModeGuide
import com.dpis.module.settings.LocalizedActivity

class ModeGuideActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installModeGuide()
    }
}
