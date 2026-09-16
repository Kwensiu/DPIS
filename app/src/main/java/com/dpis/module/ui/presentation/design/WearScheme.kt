package com.dpis.module.ui.compose

import androidx.compose.material3.ColorScheme as PhoneColorScheme
import androidx.wear.compose.material3.ColorScheme as WearColorScheme

/** Copies the generated phone scheme onto Wear Material 3 roles Wear actually has. */
internal fun PhoneColorScheme.toWearColorScheme(base: WearColorScheme): WearColorScheme = base.copy(
    primary = primary,
    primaryContainer = primaryContainer,
    onPrimary = onPrimary,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    secondaryContainer = secondaryContainer,
    onSecondary = onSecondary,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiary = onTertiary,
    onTertiaryContainer = onTertiaryContainer,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    background = background,
    onBackground = onBackground,
    error = error,
    errorContainer = errorContainer,
    onError = onError,
    onErrorContainer = onErrorContainer,
)
