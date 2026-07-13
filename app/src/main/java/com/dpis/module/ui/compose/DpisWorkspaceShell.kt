package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
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
    isCompactUi -> DpisWorkspaceNavigationLayout.BOTTOM_BAR
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
            DpisWorkspaceNavigationLayout.BOTTOM_BAR -> Scaffold(
                bottomBar = {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        DpisWorkspaceDestination.entries.forEach { destination ->
                            val label = stringResource(destination.labelRes)
                            NavigationBarItem(
                                selected = destination == selectedDestination,
                                onClick = { onDestinationSelected(destination) },
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
                        NavigationRailItem(
                            selected = destination == selectedDestination,
                            onClick = { onDestinationSelected(destination) },
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
                            NavigationDrawerItem(
                                selected = destination == selectedDestination,
                                onClick = { onDestinationSelected(destination) },
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
 * App and Template clear of the bottom gesture/navigation boundary. Settings
 * already owns both horizontal safe sides through its legacy View listener.
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
        DpisWorkspaceDestination.TOOLS -> PaddingValues(end = endPadding)

        DpisWorkspaceDestination.SETTINGS -> PaddingValues() // TODO(Compose Theme 3): Remove
        // this bridge exception when Settings content is Compose-native and owns both safe sides.
    }
}

/** Keeps legacy content above the Compose NavigationBar without claiming status-bar ownership. */
private fun legacyBottomNavigationPadding(scaffoldPadding: PaddingValues): PaddingValues =
    PaddingValues(bottom = scaffoldPadding.calculateBottomPadding())
