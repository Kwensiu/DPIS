package com.dpis.module.home

import android.os.Bundle
import com.dpis.module.home.presentation.installModeHelp
import com.dpis.module.settings.LocalizedActivity

class ModeHelpActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installModeHelp()
    }
}
