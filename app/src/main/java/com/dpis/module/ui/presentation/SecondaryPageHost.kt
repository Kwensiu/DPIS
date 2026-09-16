package com.dpis.module.ui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.dpis.module.ui.SecondaryDestination

fun interface SecondaryNavigation {
    fun open(destination: SecondaryDestination)
}

val LocalSecondaryNavigation = staticCompositionLocalOf<SecondaryNavigation?> { null }

@Composable
internal fun ProvideSecondaryNavigation(
    navigation: SecondaryNavigation,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSecondaryNavigation provides navigation, content = content)
}
