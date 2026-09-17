package com.dpis.module.applist

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreScopePromptPolicyTest {
    @Test
    fun candidatesIgnoreFilterVisibilityAndUninstalledOrInScopeApps() {
        val apps = listOf(
            app("com.example.savedmissing", configured = true, installed = true, inScope = false),
            app("com.example.savedinscope", configured = true, installed = true, inScope = true),
            app("com.example.saveduninstalled", configured = true, installed = false, inScope = false),
            app("com.example.scopeonly", configured = true, installed = true, inScope = true),
            app("com.example.notconfigured", configured = false, installed = true, inScope = false),
        )

        assertEquals(
            listOf("com.example.savedmissing", "com.example.notconfigured"),
            RestoreScopePromptPolicy.candidatePackages(
                apps,
                setOf("com.example.savedmissing", "com.example.notconfigured"),
            ),
        )
    }

    @Test
    fun unknownScopeIsNotReadableAndDoesNotCreateCandidates() {
        val apps = listOf(
            app(
                "com.example.savedmissing",
                configured = true,
                installed = true,
                inScope = false,
                scopeKnown = false,
            ),
        )

        assertFalse(RestoreScopePromptPolicy.restoredScopeReadable(apps, setOf("com.example.savedmissing")))
        assertTrue(
            RestoreScopePromptPolicy.candidatePackages(apps, setOf("com.example.savedmissing")).isEmpty(),
        )
    }

    @Test
    fun restoredScopeRemainsUnreadableUntilEachInstalledTargetHasScopeState() {
        val apps = listOf(
            app("com.example.known", configured = false, installed = true, inScope = false),
            app("com.example.unknown", configured = false, installed = true, inScope = false, scopeKnown = false),
        )

        assertFalse(
            RestoreScopePromptPolicy.restoredScopeReadable(
                apps,
                setOf("com.example.known", "com.example.unknown"),
            ),
        )
    }

    @Test
    fun cardShowsOnlyOnSettledConfiguredPageWithCandidates() {
        assertTrue(
            RestoreScopePromptPolicy.shouldShowCard(
                pending = true,
                modernFlavor = true,
                configuredPage = true,
                scopeReadable = true,
                catalogSettled = true,
                hasCandidates = true,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldShowCard(
                pending = true,
                modernFlavor = true,
                configuredPage = false,
                scopeReadable = true,
                catalogSettled = true,
                hasCandidates = true,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldShowCard(
                pending = true,
                modernFlavor = false,
                configuredPage = true,
                scopeReadable = true,
                catalogSettled = true,
                hasCandidates = true,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldShowCard(
                pending = true,
                modernFlavor = true,
                configuredPage = true,
                scopeReadable = false,
                catalogSettled = true,
                hasCandidates = true,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldShowCard(
                pending = true,
                modernFlavor = true,
                configuredPage = true,
                scopeReadable = true,
                catalogSettled = false,
                hasCandidates = true,
            ),
        )
    }

    @Test
    fun idleConsumeRequiresReadableSettledEmptyCandidates() {
        assertTrue(
            RestoreScopePromptPolicy.shouldConsumeIdle(
                pending = true,
                modernFlavor = true,
                scopeReadable = true,
                catalogSettled = true,
                hasCandidates = false,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldConsumeIdle(
                pending = true,
                modernFlavor = true,
                scopeReadable = false,
                catalogSettled = true,
                hasCandidates = false,
            ),
        )
        assertFalse(
            RestoreScopePromptPolicy.shouldConsumeIdle(
                pending = true,
                modernFlavor = true,
                scopeReadable = true,
                catalogSettled = false,
                hasCandidates = false,
            ),
        )
    }

    @Test
    fun requestClearsWhenStartedOrWhenNothingRemainsToRequest() {
        assertTrue(RestoreScopePromptPolicy.shouldClearAfterRequest(true, false, 2))
        assertTrue(RestoreScopePromptPolicy.shouldClearAfterRequest(false, false, 0))
        assertFalse(RestoreScopePromptPolicy.shouldClearAfterRequest(false, true, 2))
        assertFalse(RestoreScopePromptPolicy.shouldClearAfterRequest(false, false, 2))
    }

    private fun app(
        packageName: String,
        configured: Boolean,
        installed: Boolean,
        inScope: Boolean,
        scopeKnown: Boolean = true,
    ): AppListItem {
        return AppListItem(
            packageName,
            packageName,
            inScope,
            scopeKnown,
            null,
            null,
            ViewportApplyMode.OFF,
            null,
            ViewportTargetSpec.off(),
            null,
            FontApplyMode.OFF,
            null,
            configured,
            null,
            true,
            configured,
            installed,
            false,
            false,
            null,
        )
    }
}
