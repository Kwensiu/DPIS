package com.dpis.module.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.dpis.module.home.presentation.installDonate
import com.dpis.module.settings.LocalizedActivity

class DonateActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installDonate()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent =
            Intent(context, DonateActivity::class.java)
    }
}
