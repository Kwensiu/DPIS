package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/** Pure policy so adaptive navigation remains independently testable. */
fun resolveDpisWorkspaceNavigationLayout(
    maxWidth: Dp,
    isCompactUi: Boolean
): DpisWorkspaceNavigationLayout = when {
    // WatchUiMode owns classification. Its compact decision must win over width.
    isCompactUi -> DpisWorkspaceNavigationLayout.COMPACT_RADIAL
    maxWidth < 600.dp -> DpisWorkspaceNavigationLayout.BOTTOM_BAR
    maxWidth < 840.dp -> DpisWorkspaceNavigationLayout.NAVIGATION_RAIL
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
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (resolveDpisWorkspaceNavigationLayout(maxWidth, isCompactUi)) {
            DpisWorkspaceNavigationLayout.COMPACT_RADIAL -> CompactRadialWorkspaceNavigation(
                selectedDestination = selectedDestination,
                onDestinationSelected = onDestinationSelected,
                content = content
            )

            DpisWorkspaceNavigationLayout.BOTTOM_BAR -> Scaffold(
                bottomBar = {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
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

            DpisWorkspaceNavigationLayout.NAVIGATION_RAIL -> Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    windowInsets = navigationSurfaceInsets()
                ) {
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
                Box(Modifier.weight(1f)) {
                    content(legacyWorkspaceInsetsFor(selectedDestination))
                }
            }

            DpisWorkspaceNavigationLayout.NAVIGATION_DRAWER -> PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        windowInsets = navigationSurfaceInsets()
                    ) {
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
                                modifier = Modifier.padding(horizontal = LocalDpisTokens.current.spaceSm)
                            )
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

private const val COMPACT_MAIN_BUTTON_SIZE_DP = 56
private const val COMPACT_MENU_BUTTON_SIZE_DP = 48
private const val COMPACT_MENU_ARC_RADIUS_DP = 104
private const val COMPACT_MENU_START_ANGLE_DEGREES = 210
private const val COMPACT_MENU_ANGLE_STEP_DEGREES = 30
private const val COMPACT_MAIN_BUTTON_MARGIN_DP = 12

/** Keeps the established compact-watch radial workspace selector and its touch targets. */
@Composable
private fun CompactRadialWorkspaceNavigation(
    selectedDestination: DpisWorkspaceDestination,
    onDestinationSelected: (DpisWorkspaceDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        content(legacyWorkspaceInsetsFor(selectedDestination))

        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
                    .clickable { expanded = false }
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = navigationBarPadding)
        ) {
            if (expanded) {
                DpisWorkspaceDestination.entries.forEachIndexed { index, destination ->
                    val angle = Math.toRadians(
                        (COMPACT_MENU_START_ANGLE_DEGREES
                            + index * COMPACT_MENU_ANGLE_STEP_DEGREES).toDouble()
                    )
                    val horizontalOffset = (kotlin.math.cos(angle)
                        * COMPACT_MENU_ARC_RADIUS_DP).toInt().dp
                    val verticalOffset = (kotlin.math.sin(angle)
                        * COMPACT_MENU_ARC_RADIUS_DP).toInt().dp
                    val bottomMargin = (
                        COMPACT_MAIN_BUTTON_MARGIN_DP + COMPACT_MAIN_BUTTON_SIZE_DP / 2
                            - verticalOffset.value - COMPACT_MENU_BUTTON_SIZE_DP / 2
                        ).dp
                    val select = rememberDpisConfirmAction {
                        expanded = false
                        onDestinationSelected(destination)
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = horizontalOffset, y = -bottomMargin)
                            .size(COMPACT_MENU_BUTTON_SIZE_DP.dp)
                            .shadow(3.dp, CircleShape)
                            .clickable(onClick = select),
                        shape = CircleShape,
                        color = if (destination == selectedDestination) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = stringResource(destination.labelRes),
                            tint = if (destination == selectedDestination) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            val toggleMenu = rememberDpisConfirmAction { expanded = !expanded }
            FloatingActionButton(
                onClick = toggleMenu,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -COMPACT_MAIN_BUTTON_MARGIN_DP.dp)
                    .size(COMPACT_MAIN_BUTTON_SIZE_DP.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    painter = painterResource(selectedDestination.iconRes),
                    contentDescription = stringResource(selectedDestination.labelRes)
                )
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
