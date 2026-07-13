package com.dpis.module.ui.compose

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.dpis.module.home.ModeGuideActivity

/** Type-safe Compose entry points for Java-owned standalone Activity contracts. */
object SupportActivityContent {
    @JvmStatic
    fun installDonate(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                DonateSupportPage(onBack = activity::finish)
            }
        }
    }

    @JvmStatic
    fun installModeHelp(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ModeHelpPage(
                    onBack = activity::finish,
                    onOpenModeGuide = {
                        activity.startActivity(Intent(activity, ModeGuideActivity::class.java))
                    }
                )
            }
        }
    }

    @JvmStatic
    fun installModeGuide(activity: ComponentActivity) {
        activity.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ModeGuidePage(onBack = activity::finish)
            }
        }
    }
}
