package com.dpis.module.onboarding.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dpis.module.R
import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.ui.presentation.design.findActivity

@Composable
internal fun OnboardingScreen(request: OnboardingGate.Request) {
    val context = LocalContext.current
    var step by remember(request) { mutableStateOf(request.currentStep()) }
    val permissionSession = remember(request) { OnboardingPermissionSession(request) }
    var languageTag by remember(request) { mutableStateOf(AppLocaleManager.getLanguageTag(context)) }
    var lastBackAt by remember(request) { mutableLongStateOf(0L) }
    var showBackExitWarning by remember(request) { mutableStateOf(false) }
    var exiting by remember(request) { mutableStateOf(false) }
    val configuration = request.configuration
    val lifecycleOwner = LocalLifecycleOwner.current
    val localizedContext = remember(context, languageTag) {
        AppLocaleManager.wrap(context)
    }

    LaunchedEffect(configuration) {
        configuration?.refreshAppearance()
    }

    LaunchedEffect(showBackExitWarning) {
        if (showBackExitWarning) {
            Toast.makeText(
                context,
                R.string.onboarding_back_again_to_exit,
                Toast.LENGTH_SHORT,
            ).show()
            showBackExitWarning = false
        }
    }

    LaunchedEffect(step) {
        if (step == OnboardingStep.ROOT) {
            permissionSession.refreshRoot()
            permissionSession.refreshOptionalAccess()
        } else {
            permissionSession.invalidatePendingRootRequest()
        }
    }
    DisposableEffect(lifecycleOwner, request, step) {
        if (step != OnboardingStep.ROOT) {
            onDispose { }
        } else {
            var firstResume = true
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (firstResume) {
                        firstResume = false
                    } else {
                        permissionSession.refreshRoot(force = true)
                        permissionSession.refreshOptionalAccess()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    fun moveTo(nextStep: OnboardingStep) {
        request.persistStep(nextStep)
        step = nextStep
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
        OnboardingShell(
            step = step,
            onBack = {
                if (step != OnboardingStep.WELCOME) {
                    moveTo(
                        when (step) {
                            OnboardingStep.TERMS -> OnboardingStep.WELCOME
                            OnboardingStep.ROOT -> OnboardingStep.TERMS
                            OnboardingStep.CONFIGURATION -> OnboardingStep.ROOT
                            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                        },
                    )
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackAt <= BACK_TO_EXIT_WINDOW_MS) {
                        request.onBack()
                    } else {
                        lastBackAt = now
                        showBackExitWarning = true
                    }
                }
            },
            exiting = exiting,
            welcomeOverlay = {
                WelcomeLanguageOverlay(
                    onLanguageSelected = { selectedTag ->
                        val previousTag = AppLocaleManager.getLanguageTag(context)
                        if (selectedTag != previousTag &&
                            AppLocaleManager.setLanguageTag(context, selectedTag)
                        ) {
                            languageTag = selectedTag
                            context.findActivity()?.recreate()
                        }
                    },
                )
            },
            onNext = {
                when (step) {
                    OnboardingStep.WELCOME -> moveTo(OnboardingStep.TERMS)
                    OnboardingStep.TERMS -> if (request.acceptTerms()) moveTo(OnboardingStep.ROOT)
                    OnboardingStep.ROOT -> moveTo(OnboardingStep.CONFIGURATION)
                    OnboardingStep.CONFIGURATION -> if (request.complete()) exiting = true
                }
            },
        ) { targetStep ->
            when (targetStep) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.TERMS -> TermsStep()
                OnboardingStep.ROOT -> RootStep(
                    status = permissionSession.rootStatus,
                    provider = permissionSession.rootProvider,
                    checking = permissionSession.checkingRoot,
                    appListAccess = permissionSession.appListAccess,
                    usageAccess = permissionSession.usageAccess,
                    onPermissionAction = { permission ->
                        when (permission) {
                            PermissionKind.ROOT -> permissionSession.refreshRoot(force = true)
                            PermissionKind.APP_LIST -> request.requestAppListAccess()
                            PermissionKind.USAGE -> request.requestUsageAccess()
                        }
                    },
                )

                OnboardingStep.CONFIGURATION -> OnboardingConfigurationPage(
                    configuration = configuration,
                    request = request,
                )
            }
        }
    }

}

private const val BACK_TO_EXIT_WINDOW_MS = 2_000L
