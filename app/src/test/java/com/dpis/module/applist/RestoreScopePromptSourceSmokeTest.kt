package com.dpis.module.applist

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreScopePromptSourceSmokeTest {
    @Test
    fun restorePromptIsOneShotForBackupScopeAndReusesBatchScopeRequest() {
        val workspace = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/applist/presentation/AppWorkspaceContent.kt",
        )
        val hostWiring = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt",
        )
        val settings = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt",
        )
        val session = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt",
        )

        assertTrue(workspace.contains("RestoreScopePromptDialog("))
        assertTrue(workspace.contains("FloatingActionButton("))
        assertTrue(workspace.contains("AnimatedVisibility("))
        assertTrue(workspace.contains("enter = fadeIn("))
        assertTrue(workspace.contains("exit = fadeOut("))
        assertTrue(workspace.contains("scaleIn("))
        assertTrue(workspace.contains("scaleOut("))
        assertTrue(workspace.contains("val listBottomPadding = bottomPadding + 12.dp"))
        assertFalse(workspace.contains("RestoreScopeFabClearance"))
        assertTrue(workspace.contains("R.drawable.ic_sparkles_24"))
        assertTrue(workspace.contains("R.string.restore_scope_prompt_body"))
        assertTrue(workspace.contains("page == AppListPage.CONFIGURED_APPS && restoreScopePromptVisible"))
        assertTrue(workspace.contains("onDismissDialog = { restoreScopeDialogVisible = false }"))
        assertFalse(workspace.contains("filterState.injectedOnly()"))
        assertTrue(hostWiring.contains("RestoreScopePromptPolicy.candidatePackages("))
        assertTrue(hostWiring.contains("restoreScopePromptStore.scopePackages()"))
        assertTrue(hostWiring.contains("BatchScopeRequestCoordinator.RESTORE_BACKUP_SCOPE_SOURCE"))
        assertTrue(settings.contains("RestoreScopePromptStore(activity).replacePendingScope(moduleScope)"))
        assertTrue(session.contains("RestoreScopePromptPolicy.shouldShowCard("))
        assertTrue(session.contains("RestoreScopePromptPolicy.shouldConsumeIdle("))
    }
}
