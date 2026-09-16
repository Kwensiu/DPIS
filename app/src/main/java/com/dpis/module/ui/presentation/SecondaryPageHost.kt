package com.dpis.module.ui.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.dpis.module.MainActivity
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

fun interface SecondaryNavigation {
    fun open(destination: SecondaryDestination)
}

val LocalSecondaryNavigation = staticCompositionLocalOf<SecondaryNavigation?> { null }

/** Opens secondary pages as Activities so back uses the system window predictive gesture. */
class SecondaryPageHost(
    private val activity: MainActivity,
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

@Composable
internal fun ProvideSecondaryNavigation(
    navigation: SecondaryNavigation,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSecondaryNavigation provides navigation, content = content)
}
