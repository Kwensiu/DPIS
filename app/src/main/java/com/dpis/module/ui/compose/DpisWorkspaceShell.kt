package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.setValue
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults as WearButtonDefaults
import androidx.wear.compose.material3.CompactButton as WearCompactButton
import androidx.wear.compose.material3.Button as WearButton
import androidx.wear.compose.material3.Icon as WearIcon
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text as WearText
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.dpis.module.R

/** Mirrors MainUiState.WorkspaceMode without introducing a second mutable selection state. */
enum class DpisWorkspaceDestination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    // Keep this sequence aligned with main_workspace_navigation.xml so Compose
    // does not reorder or restyle established workspace navigation for users.
    APP(R.string.workspace_app, R.drawable.ic_apps_24),
    TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24),
    HOME(R.string.workspace_home, R.drawable.ic_home_24),
    TOOLS(R.string.workspace_tools, R.drawable.ic_build_24),
    SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24)
}

enum class DpisWorkspaceNavigationLayout {
    COMPACT_RADIAL,
    BOTTOM_BAR,
    NAVIGATION_RAIL,
    NAVIGATION_DRAWER
}

// Side navigation is allowed only when its explicit width leaves enough room for the shared
// App/Template list-detail contract. Keeping these equations centralized prevents shell navigation
// changes from silently forcing either workspace back into its compact Sheet presentation.
internal val WorkspaceTwoPaneMinWidth = 600.dp
internal val WorkspaceRailWidth = 80.dp
internal val WorkspaceDrawerWidth = 240.dp
internal val WorkspaceRailMinWindowWidth = WorkspaceTwoPaneMinWidth + WorkspaceRailWidth
internal val WorkspaceDrawerMinWindowWidth = WorkspaceTwoPaneMinWidth + WorkspaceDrawerWidth

/** Compact Wear navigation is the single owner of space reserved for its floating selector. */
internal val LocalWearWorkspaceContentPadding = compositionLocalOf { PaddingValues() }

/** Pure policy so adaptive navigation remains independently testable. */
fun resolveDpisWorkspaceNavigationLayout(
    maxWidth: Dp,
    isCompactUi: Boolean
): DpisWorkspaceNavigationLayout = when {
    // WatchUiMode owns classification. Its compact decision must win over width.
    isCompactUi -> DpisWorkspaceNavigationLayout.COMPACT_RADIAL
    maxWidth < WorkspaceRailMinWindowWidth -> DpisWorkspaceNavigationLayout.BOTTOM_BAR
    maxWidth < WorkspaceDrawerMinWindowWidth -> DpisWorkspaceNavigationLayout.NAVIGATION_RAIL
    else -> DpisWorkspaceNavigationLayout.NAVIGATION_DRAWER
}

/**
 * A stateless Compose shell for the five existing DPIS workspaces.
 *
 * [selectedDestination] and [onDestinationSelected] are supplied by the main
 * UI-state boundary. This keeps MainViewModel/MainUiAction authoritative while
 * each later migration can replace only its workspace content.
 */
@Composable
fun DpisWorkspaceShell(
    selectedDestination: DpisWorkspaceDestination,
    onDestinationSelected: (DpisWorkspaceDestination) -> Unit,
    isCompactUi: Boolean,
    modifier: Modifier = Modifier,
    showCompactNavigation: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Persistent navigation uses the brightest neutral surface role so it remains
        // visually distinct from the page container without introducing an accent color.
        val navigationContainerColor = MaterialTheme.colorScheme.surfaceBright
        when (resolveDpisWorkspaceNavigationLayout(maxWidth, isCompactUi)) {
            DpisWorkspaceNavigationLayout.COMPACT_RADIAL -> CompactWearWorkspaceNavigation(
                selectedDestination = selectedDestination,
                onDestinationSelected = onDestinationSelected,
                showNavigation = showCompactNavigation,
                content = content
            )

            DpisWorkspaceNavigationLayout.BOTTOM_BAR -> Scaffold(
                bottomBar = {
                    // Include horizontal display-cutout insets so the bar's colored surface
                    // reaches the camera-safe edge in landscape instead of being clipped.
                    NavigationBar(
                        containerColor = navigationContainerColor,
                        windowInsets = bottomNavigationSurfaceInsets()
                    ) {
                            DpisWorkspaceDestination.entries.forEach { destination ->
                                val label = stringResource(destination.labelRes)
                                val onDestinationClick = rememberDpisConfirmAction {
                                    if (destination != selectedDestination) {
                                        onDestinationSelected(destination)
                                    }
                                }
                                NavigationBarItem(
                                    selected = destination == selectedDestination,
                                    onClick = onDestinationClick,
                                    icon = {
                                        Icon(
                                            painter = painterResource(destination.iconRes),
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    // Match the established bottom-navigation density: only
                                    // the active destination exposes its text label.
                                    alwaysShowLabel = false
                                )
                            }
                    }
                },
                // The legacy workspace root still owns all system-bar and cutout
                // handling. Compose reserves only the bottom navigation it adds.
                content = { scaffoldPadding ->
                    content(legacyBottomNavigationPadding(scaffoldPadding))
                }
            )

            DpisWorkspaceNavigationLayout.NAVIGATION_RAIL -> {
                val layoutDirection = LocalLayoutDirection.current
                val startCutout = safeDrawingInsets().asPaddingValues()
                    .calculateStartPadding(layoutDirection)
                Row(Modifier.fillMaxSize()) {
                    // Keep the cutout outside the rail's fixed content width. Applying it as
                    // NavigationRail windowInsets shrinks the icon slot and clips the camera-side
                    // navigation icons on landscape devices.
                    Box(
                        modifier = Modifier
                            .width(WorkspaceRailWidth + startCutout)
                            .fillMaxHeight()
                            .background(navigationContainerColor)
                    ) {
                        NavigationRail(
                            modifier = Modifier
                                .padding(start = startCutout)
                                .width(WorkspaceRailWidth),
                            containerColor = Color.Transparent,
                            windowInsets = verticalNavigationSurfaceInsets()
                        ) {
                            Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                                DpisWorkspaceDestination.entries.forEach { destination ->
                                    val label = stringResource(destination.labelRes)
                                    val onDestinationClick = rememberDpisConfirmAction {
                                        if (destination != selectedDestination) {
                                            onDestinationSelected(destination)
                                        }
                                    }
                                    NavigationRailItem(
                                        selected = destination == selectedDestination,
                                        onClick = onDestinationClick,
                                        icon = {
                                            Icon(
                                                painter = painterResource(destination.iconRes),
                                                contentDescription = label
                                            )
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        content(legacyWorkspaceInsetsFor(selectedDestination))
                    }
                }
            }

            DpisWorkspaceNavigationLayout.NAVIGATION_DRAWER -> PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(WorkspaceDrawerWidth),
                        drawerContainerColor = navigationContainerColor,
                        windowInsets = navigationSurfaceInsets()
                    ) {
                        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                            Text(
                                text = stringResource(R.string.app_name),
                                modifier = Modifier.padding(LocalDpisTokens.current.spaceMd),
                                style = MaterialTheme.typography.titleLarge
                            )
                            DpisWorkspaceDestination.entries.forEach { destination ->
                                val label = stringResource(destination.labelRes)
                                val onDestinationClick = rememberDpisConfirmAction {
                                    if (destination != selectedDestination) {
                                        onDestinationSelected(destination)
                                    }
                                }
                                NavigationDrawerItem(
                                    selected = destination == selectedDestination,
                                    onClick = onDestinationClick,
                                    icon = {
                                        Icon(
                                            painter = painterResource(destination.iconRes),
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.padding(
                                        horizontal = LocalDpisTokens.current.spaceSm
                                    )
                                )
                            }
                        }
                    }
                }
            ) {
                Box(Modifier.fillMaxSize()) {
                    content(legacyWorkspaceInsetsFor(selectedDestination))
                }
            }
        }
    }
}

/** Dedicated Wear OS navigation surface; phone navigation components do not fit round screens. */
@Composable
private fun CompactWearWorkspaceNavigation(
    selectedDestination: DpisWorkspaceDestination,
    onDestinationSelected: (DpisWorkspaceDestination) -> Unit,
    showNavigation: Boolean,
    content: @Composable (PaddingValues) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }
    val phoneColors = MaterialTheme.colorScheme
    val wearColors = WearMaterialTheme.colorScheme.copy(
        primary = phoneColors.primary,
        primaryContainer = phoneColors.primaryContainer,
        onPrimary = phoneColors.onPrimary,
        onPrimaryContainer = phoneColors.onPrimaryContainer,
        secondary = phoneColors.secondary,
        secondaryContainer = phoneColors.secondaryContainer,
        onSecondary = phoneColors.onSecondary,
        onSecondaryContainer = phoneColors.onSecondaryContainer,
        tertiary = phoneColors.tertiary,
        tertiaryContainer = phoneColors.tertiaryContainer,
        onTertiary = phoneColors.onTertiary,
        onTertiaryContainer = phoneColors.onTertiaryContainer,
        surfaceContainerLow = phoneColors.surfaceContainerLow,
        surfaceContainer = phoneColors.surfaceContainer,
        surfaceContainerHigh = phoneColors.surfaceContainerHigh,
        onSurface = phoneColors.onSurface,
        onSurfaceVariant = phoneColors.onSurfaceVariant,
        outline = phoneColors.outline,
        outlineVariant = phoneColors.outlineVariant,
        background = phoneColors.background,
        onBackground = phoneColors.onBackground,
        error = phoneColors.error,
        errorContainer = phoneColors.errorContainer,
        onError = phoneColors.onError,
        onErrorContainer = phoneColors.onErrorContainer
    )

    WearMaterialTheme(colorScheme = wearColors) {
        AppScaffold(modifier = Modifier.background(wearColors.background)) {
            if (expanded) {
                val listState = rememberTransformingLazyColumnState()
                val transformationSpec = rememberTransformationSpec()
                ScreenScaffold(scrollState = listState) { contentPadding ->
                    TransformingLazyColumn(
                        state = listState,
                        contentPadding = contentPadding,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = DpisWorkspaceDestination.entries.size,
                            key = { index -> DpisWorkspaceDestination.entries[index].name }
                        ) { index ->
                            val destination = DpisWorkspaceDestination.entries[index]
                            val label = stringResource(destination.labelRes)
                            val select = rememberDpisConfirmAction {
                                expanded = false
                                if (destination != selectedDestination) {
                                    onDestinationSelected(destination)
                                }
                            }
                            WearButton(
                                onClick = select,
                                label = { WearText(label) },
                                icon = {
                                    WearIcon(
                                        painter = painterResource(destination.iconRes),
                                        contentDescription = label,
                                        modifier = Modifier.size(WearButtonDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(
                                        WearButtonDefaults.minimumVerticalListContentPadding
                                    ),
                                transformation = SurfaceTransformation(transformationSpec)
                            )
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    content(legacyWorkspaceInsetsFor(selectedDestination))
                    if (showNavigation) {
                        val openNavigation = rememberDpisConfirmAction { expanded = true }
                        WearCompactButton(
                        onClick = openNavigation,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp),
                        icon = {
                            WearIcon(
                                painter = painterResource(selectedDestination.iconRes),
                                contentDescription = stringResource(selectedDestination.labelRes),
                                modifier = Modifier.size(WearButtonDefaults.ExtraSmallIconSize)
                            )
                        }
                        )
                    }
                }
            }
        }
    }
}

/** The current Compose BOM exposes safe drawing as the system-bar and cutout union. */
@Composable
private fun safeDrawingInsets(): WindowInsets = WindowInsets.systemBars
    .union(WindowInsets.displayCutout)

/** The start-side rail owns only its own horizontal cutout and vertical bars. */
@Composable
private fun navigationSurfaceInsets(): WindowInsets = safeDrawingInsets().only(
    WindowInsetsSides.Start + WindowInsetsSides.Vertical
)

/** Rail content keeps vertical bars but receives the camera cutout from its outer surface. */
@Composable
private fun verticalNavigationSurfaceInsets(): WindowInsets = safeDrawingInsets().only(
    WindowInsetsSides.Vertical
)

/** Bottom navigation owns both the gesture boundary and horizontal camera cutouts. */
@Composable
private fun bottomNavigationSurfaceInsets(): WindowInsets = safeDrawingInsets().only(
    WindowInsetsSides.Start + WindowInsetsSides.End + WindowInsetsSides.Bottom
)

/**
 * The navigation rail/drawer owns the start-side cutout. Content only consumes
 * the end side so a left camera cutout cannot create a second empty gutter.
 * Every legacy workspace owns its own top system-bar inset; Compose only keeps
 * App and Template clear of the bottom gesture/navigation boundary. Compose
 * workspaces use the same end-side rule; compact Watch keeps its View route.
 */
@Composable
private fun legacyWorkspaceInsetsFor(destination: DpisWorkspaceDestination): PaddingValues {
    val safePadding = safeDrawingInsets().asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = safePadding.calculateEndPadding(layoutDirection)
    return when (destination) {
        DpisWorkspaceDestination.APP,
        DpisWorkspaceDestination.TEMPLATE -> PaddingValues(
            end = endPadding,
            bottom = safePadding.calculateBottomPadding()
        )

        DpisWorkspaceDestination.HOME,
        DpisWorkspaceDestination.TOOLS,
        DpisWorkspaceDestination.SETTINGS -> PaddingValues(end = endPadding)
    }
}

/** Keeps legacy content above the Compose NavigationBar without claiming status-bar ownership. */
private fun legacyBottomNavigationPadding(scaffoldPadding: PaddingValues): PaddingValues =
    PaddingValues(bottom = scaffoldPadding.calculateBottomPadding())
