package com.dpis.module.ui

/** Secondary pages opened from the live workspace through [com.dpis.module.ui.presentation.SecondaryNavigation]. */
sealed interface SecondaryDestination {
    data object Theme : SecondaryDestination
    data object About : SecondaryDestination
    data object Licenses : SecondaryDestination
    data object Donate : SecondaryDestination
    data object FontLibrary : SecondaryDestination
    data class FontDetail(val fontId: String) : SecondaryDestination
    data object Experimental : SecondaryDestination
    data object Logs : SecondaryDestination
    data object ModeHelp : SecondaryDestination
    data object ModeGuide : SecondaryDestination
}
