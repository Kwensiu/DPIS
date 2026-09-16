package com.dpis.module.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.R
import com.dpis.module.about.presentation.OpenSourceLicenseItems
import com.dpis.module.about.presentation.installOpenSourceLicenses
import com.dpis.module.settings.LocalizedActivity

class OpenSourceLicenseActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installOpenSourceLicenses(
            OpenSourceLicenseItems.load(this),
            ::openUrl,
        )
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            if (isFinishing || isDestroyed) {
                return
            }
            Toast.makeText(this, R.string.about_link_open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
