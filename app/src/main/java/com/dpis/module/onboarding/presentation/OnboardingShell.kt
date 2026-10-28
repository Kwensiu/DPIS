package com.dpis.module.onboarding.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.onboarding.OnboardingStep
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.Spacing
import com.dpis.module.ui.presentation.editor.FeedbackButton
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun OnboardingShell(
    step: OnboardingStep,
    onNext: () -> Unit,
    onBack: () -> Unit,
    exiting: Boolean = false,
    onExitFinished: () -> Unit = {},
    welcomeOverlay: @Composable (() -> Unit)? = null,
    content: @Composable (OnboardingStep) -> Unit,
) {
    val spacing = LocalSpacing.current
    LaunchedEffect(exiting) {
        if (exiting) {
            delay(ONBOARDING_EXIT_DURATION)
            onExitFinished()
        }
    }
    BackHandler(enabled = !exiting, onBack = onBack)

    AnimatedVisibility(
        visible = !exiting,
        enter = EnterTransition.None,
        exit = slideOutHorizontally(
            animationSpec = tween(ONBOARDING_EXIT_DURATION.inWholeMilliseconds.toInt()),
            targetOffsetX = { -it },
        ) + fadeOut(animationSpec = tween(ONBOARDING_EXIT_DURATION.inWholeMilliseconds.toInt())),
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {},
            bottomBar = { OnboardingFooter(step, onNext) },
        ) { insets ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
            ) {
                OnboardingPageTransition(
                    step = step,
                    spacing = spacing,
                    welcomeOverlay = welcomeOverlay,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageTransition(
    step: OnboardingStep,
    spacing: Spacing,
    welcomeOverlay: @Composable (() -> Unit)?,
    content: @Composable (OnboardingStep) -> Unit,
) {
    AnimatedContent(
        targetState = step,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val direction = if (forward) 1 else -1
            (
                    fadeIn(
                        animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 420f,
                        ),
                    ) +
                            scaleIn(
                                initialScale = 0.98f,
                                animationSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = 420f,
                                ),
                            ) +
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.86f,
                                    stiffness = 480f,
                                ),
                                initialOffsetX = { direction * it / 10 },
                            )
                    ) togetherWith (
                    fadeOut(
                        animationSpec = spring(
                            dampingRatio = 1f,
                            stiffness = 500f,
                        ),
                    ) +
                            scaleOut(
                                targetScale = 0.985f,
                                animationSpec = spring(
                                    dampingRatio = 1f,
                                    stiffness = 500f,
                                ),
                            ) +
                            slideOutHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 1f,
                                    stiffness = 500f,
                                ),
                                targetOffsetX = { -direction * it / 14 },
                            )
                    ) using SizeTransform(clip = false)
        },
        label = "onboarding-page-transition",
    ) { targetStep ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            OnboardingScrollableContent(
                spacing = spacing,
                content = { content(targetStep) },
            )
            if (targetStep == OnboardingStep.WELCOME) {
                OnboardingWelcomeOverlay(
                    spacing = spacing,
                    content = welcomeOverlay,
                )
            }
        }
    }
}

@Composable
private fun OnboardingScrollableContent(
    spacing: Spacing,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = spacing.xl,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + spacing.lg,
                end = spacing.xl,
                bottom = spacing.lg,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun OnboardingWelcomeOverlay(
    spacing: Spacing,
    content: @Composable (() -> Unit)?,
) {
    content?.let { overlay ->
        Box(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopEnd,
        ) {
            Box(
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    end = spacing.xl,
                ),
            ) {
                overlay()
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    step: OnboardingStep,
    onNext: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = spacing.xl, vertical = spacing.md),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.md),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(OnboardingStep.entries.size) { index ->
                        val active = index == step.ordinal
                        val width by animateDpAsState(
                            targetValue = if (active) 32.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 500f,
                            ),
                            label = "onboarding-page-dot-$index",
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(width)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                    }
                }
                FeedbackButton(
                    onClick = onNext,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(
                            if (step == OnboardingStep.CONFIGURATION) {
                                Modifier.testTag("onboarding-finish")
                            } else {
                                Modifier
                            },
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.md),
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith
                                    fadeOut(animationSpec = spring())
                        },
                        label = "onboarding-action-label",
                    ) { targetStep ->
                        Text(
                            text = stringResource(targetStep.actionLabel()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private val ONBOARDING_EXIT_DURATION = 420.milliseconds

private fun OnboardingStep.actionLabel(): Int = when (this) {
    OnboardingStep.WELCOME -> R.string.onboarding_start
    OnboardingStep.TERMS -> R.string.startup_disclaimer_accept_button
    OnboardingStep.ROOT -> R.string.onboarding_continue
    OnboardingStep.CONFIGURATION -> R.string.onboarding_finish
}
