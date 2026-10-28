package com.dpis.module.onboarding.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.R
import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun welcomeContinuesDirectlyToTermsAndThenPermissions() {
        var acceptedTerms = false
        var completed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.WELCOME,
                        acceptTerms = { acceptedTerms = true; true },
                        onCompleted = { completed = true },
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_start),
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_terms_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.startup_disclaimer_accept_button),
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_permissions_title),
        ).assertIsDisplayed()
        composeRule.runOnIdle {
            assertTrue(acceptedTerms)
            assertFalse(completed)
        }
    }

    @Test
    fun termsPageExplainsRisksPermissionsConfigurationAndResponsibility() {
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.TERMS,
                        onCompleted = {},
                    ),
                )
            }
        }

        listOf(
            R.string.onboarding_terms_risk_title,
            R.string.onboarding_terms_privacy_title,
            R.string.onboarding_terms_open_source_title,
            R.string.onboarding_terms_permissions_title,
        ).forEach { resourceId ->
            composeRule.onNodeWithText(composeRule.activity.getString(resourceId))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun welcomeShowsInteractiveBrandAndCommunityLinks() {
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.WELCOME,
                        onCompleted = {},
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("onboarding-brand-mark").assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_welcome_message),
        ).assertIsDisplayed()
        listOf(
            R.string.home_feedback_github,
            R.string.home_feedback_qq,
            R.string.home_feedback_telegram,
        ).forEach { resourceId ->
            composeRule.onNodeWithText(composeRule.activity.getString(resourceId))
                .assertIsDisplayed()
        }
    }

    @Test
    fun permissionCardsUseWholeCardForActionAndKeepStaticChevron() {
        var rootResult = RootAccessProbe.Result.unavailable()
        var refreshCount = 0
        var appListRequests = 0
        var completed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.ROOT,
                        refreshRoot = {
                            refreshCount++
                            it(rootResult)
                        },
                        onAppListRequest = { appListRequests++ },
                        onCompleted = { completed = true },
                    ),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("onboarding-app_list-card").performClick()
        composeRule.runOnIdle { assertEquals(1, appListRequests) }
        composeRule.onNodeWithTag("onboarding-app_list-expand").assertDoesNotExist()

        composeRule.runOnIdle { rootResult = RootAccessProbe.Result.available("KernelSU") }
        composeRule.onNodeWithTag("onboarding-root-card").performClick()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_permission_available, "KernelSU"),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding-root-expand").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(2, refreshCount)
            assertFalse(completed)
        }
    }

    @Test
    fun configurationPageProvidesBackupRestoreAndThemeControlsBeforeCompletion() {
        var completed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.ROOT,
                        onCompleted = { completed = true },
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_continue),
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_configuration_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_configuration_import_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.settings_theme_section_appearance),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.settings_interface_scale_label),
        ).assertIsDisplayed()
        composeRule.runOnIdle { assertFalse(completed) }
    }

    @Test
    fun backNavigatesThroughOnboardingBeforeAllowingExit() {
        var exitRequests = 0
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.ROOT,
                        onCompleted = {},
                        onBack = { exitRequests++ },
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_permissions_title),
        ).assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_terms_title),
        ).assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_welcome_message),
        ).assertIsDisplayed()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.runOnIdle { assertEquals(0, exitRequests) }
    }

    @Test
    fun unavailableOptionalPermissionsDoNotBlockEnteringDpis() {
        var completed = false
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.ROOT,
                        onCompleted = { completed = true },
                    ),
                )
            }
        }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_continue),
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("onboarding-finish")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assertTrue(completed) }
    }

    @Test
    fun staleRootRefreshResultDoesNotOverwriteTheCurrentPermissionCheck() {
        val pendingRefreshes = ArrayDeque<(RootAccessProbe.Result) -> Unit>()
        composeRule.setContent {
            ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
                OnboardingScreen(
                    request(
                        currentStep = OnboardingStep.ROOT,
                        refreshRoot = { callback -> pendingRefreshes.addLast(callback) },
                        onCompleted = {},
                    ),
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, pendingRefreshes.size)

        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_continue),
        ).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        assertEquals(2, pendingRefreshes.size)

        pendingRefreshes.removeFirst()(RootAccessProbe.Result.available("stale"))
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.onboarding_permission_checking),
        ).assertIsDisplayed()
    }

    private fun request(
        currentStep: OnboardingStep,
        acceptTerms: () -> Boolean = { true },
        refreshRoot: (((RootAccessProbe.Result) -> Unit) -> Unit) = {
            it(RootAccessProbe.Result.unavailable())
        },
        onAppListRequest: () -> Unit = {},
        onCompleted: () -> Unit,
        onBack: () -> Unit = {},
    ) = OnboardingGate.Request(
        owner = Any(),
        currentStep = { currentStep },
        acceptTerms = acceptTerms,
        refreshRoot = refreshRoot,
        isAppListAccessAvailable = { false },
        requestAppListAccess = onAppListRequest,
        isUsageAccessAvailable = { false },
        requestUsageAccess = {},
        complete = { true },
        onCompleted = onCompleted,
        onBack = onBack,
    )
}
