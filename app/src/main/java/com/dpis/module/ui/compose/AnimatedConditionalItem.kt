package com.dpis.module.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Shared motion for an item whose presence follows a setting or capability.
 *
 * The size transition lets adjacent segmented rows move with the item instead of jumping when a
 * conditional action becomes available or is removed.
 */
@Composable
internal fun AnimatedConditionalItem(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = conditionalItemEnterTransition(),
        exit = conditionalItemExitTransition()
    ) {
        content()
    }
}

private fun conditionalItemEnterTransition(): EnterTransition =
    fadeIn(tween(180)) +
        slideInVertically(tween(180)) { -it / 5 } +
        expandVertically(tween(180), expandFrom = Alignment.Top)

private fun conditionalItemExitTransition(): ExitTransition =
    fadeOut(tween(140)) +
        slideOutVertically(tween(140)) { -it / 5 } +
        shrinkVertically(tween(140), shrinkTowards = Alignment.Top)
