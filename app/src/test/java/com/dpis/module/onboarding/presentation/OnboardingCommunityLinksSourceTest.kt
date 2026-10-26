package com.dpis.module.onboarding.presentation

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingCommunityLinksSourceTest {
    @Test
    fun communityLinksUseTaskSafeLaunchingAndIgnoreMissingHandlers() {
        val source = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/onboarding/presentation/OnboardingWelcomeContent.kt",
        )

        assertTrue(source.contains("private fun openExternalLink(context: Context, url: String)"))
        assertTrue(source.contains("Intent.FLAG_ACTIVITY_NEW_TASK"))
        assertTrue(source.contains("catch (_: ActivityNotFoundException)"))
        assertFalse(source.contains("context.startActivity(Intent(Intent.ACTION_VIEW"))
    }
}
