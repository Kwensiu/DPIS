package com.dpis.module.navigation

import android.app.Activity
import android.content.Intent
import com.dpis.module.about.AboutActivity
import com.dpis.module.about.OpenSourceLicenseActivity
import com.dpis.module.diagnostics.LogActivity
import com.dpis.module.fonts.FontDetailActivity
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.home.DonateActivity
import com.dpis.module.home.ModeGuideActivity
import com.dpis.module.home.ModeHelpActivity
import com.dpis.module.settings.ExperimentalSettingsActivity
import com.dpis.module.settings.ThemeSettingsActivity
import com.dpis.module.ui.SecondaryDestination
import com.dpis.module.ui.presentation.SecondaryNavigation

/** App-shell adapter that resolves secondary destinations to concrete Activities. */
class SecondaryActivityNavigator(
    private val activity: Activity,
) : SecondaryNavigation {
    override fun open(destination: SecondaryDestination) {
        activity.startActivity(intentFor(destination))
    }

    private fun intentFor(destination: SecondaryDestination): Intent = when (destination) {
        SecondaryDestination.Theme -> Intent(activity, ThemeSettingsActivity::class.java)
        SecondaryDestination.About -> Intent(activity, AboutActivity::class.java)
        SecondaryDestination.Licenses -> Intent(activity, OpenSourceLicenseActivity::class.java)
        SecondaryDestination.Donate -> DonateActivity.createIntent(activity)
        SecondaryDestination.FontLibrary -> Intent(activity, FontLibraryActivity::class.java)
        is SecondaryDestination.FontDetail -> Intent(activity, FontDetailActivity::class.java)
            .putExtra(FontDetailActivity.EXTRA_FONT_ID, destination.fontId)
        SecondaryDestination.Experimental ->
            Intent(activity, ExperimentalSettingsActivity::class.java)
        SecondaryDestination.Logs -> Intent(activity, LogActivity::class.java)
        SecondaryDestination.ModeHelp -> Intent(activity, ModeHelpActivity::class.java)
        SecondaryDestination.ModeGuide -> Intent(activity, ModeGuideActivity::class.java)
    }
}
