package com.dpis.module.onboarding

import com.dpis.module.FakePrefs
import com.dpis.module.root.RootAccessProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class OnboardingTest {
    @Test
    fun freshStateStartsAtWelcome() {
        assertEquals(
            OnboardingStep.WELCOME,
            OnboardingState(
                termsVersion = 0,
                termsAccepted = false,
                completed = false,
            ).currentStep(requiredTermsVersion = 1),
        )
    }

    @Test
    fun acceptingTermsMovesToRootInformationWithoutCompleting() {
        val coordinator = coordinator()

        assertEquals(OnboardingStep.WELCOME, coordinator.requiredStep())
        assertTrue(coordinator.acceptTerms())
        assertEquals(OnboardingStep.ROOT, coordinator.requiredStep())
        assertFalse(coordinator.state().completed)
    }

    @Test
    fun changedTermsVersionRequiresAcceptanceAgain() {
        assertEquals(
            OnboardingStep.TERMS,
            OnboardingState(
                termsVersion = 1,
                termsAccepted = true,
                completed = true,
                lastStep = OnboardingStep.WELCOME,
            ).currentStep(requiredTermsVersion = 2),
        )
    }

    @Test
    fun existingRiskConsentMigratesOnceToRootInformation() {
        val store = OnboardingStore(
            FakePrefs(),
            requiredTermsVersion = 1,
            legacyTermsAccepted = true,
        )
        val coordinator = OnboardingCoordinator(store)

        assertTrue(store.importedLegacyTerms)
        assertEquals(OnboardingStep.ROOT, coordinator.requiredStep())
    }

    @Test
    fun interruptedLegacyMigrationRestartsAtTermsOnTheNextSession() {
        val preferences = FakePrefs()
        val firstStore = OnboardingStore(
            preferences,
            requiredTermsVersion = 1,
            legacyTermsAccepted = true,
        )
        assertEquals(OnboardingStep.ROOT, OnboardingCoordinator(firstStore).requiredStep())

        val nextStore = OnboardingStore(
            preferences,
            requiredTermsVersion = 1,
            legacyTermsAccepted = true,
        )
        assertFalse(nextStore.importedLegacyTerms)
        assertEquals(OnboardingStep.TERMS, OnboardingCoordinator(nextStore).requiredStep())
    }

    @Test
    fun interruptedOnboardingRestartsAtTermsOnTheNextSession() {
        val preferences = FakePrefs()
        val first = coordinator(preferences)
        assertEquals(OnboardingStep.WELCOME, first.requiredStep())
        assertTrue(first.acceptTerms())
        assertEquals(OnboardingStep.ROOT, first.requiredStep())

        assertEquals(OnboardingStep.TERMS, coordinator(preferences).requiredStep())
    }

    @Test
    fun restartingAfterLeavingWelcomeRestoresWelcomeEvenWhenTermsWerePreviouslyAccepted() {
        val preferences = FakePrefs()
        val first = coordinator(preferences)
        assertTrue(first.acceptTerms())
        first.persistStep(OnboardingStep.WELCOME)

        assertEquals(OnboardingStep.WELCOME, coordinator(preferences).requiredStep())
    }

    @Test
    fun restartingOnboardingKeepsAcceptedTermsAndStartsAtWelcome() {
        val preferences = FakePrefs()
        val store = OnboardingStore(preferences, requiredTermsVersion = 1)

        assertTrue(store.acceptTerms())
        assertTrue(store.markCompleted())
        store.restart()

        val state = store.read()
        assertTrue(state.termsAccepted)
        assertEquals(1, state.termsVersion)
        assertFalse(state.completed)
        assertEquals(OnboardingStep.WELCOME, state.lastStep)
        assertEquals(OnboardingStep.WELCOME, state.currentStep(1))
    }

    @Test
    fun completionCannotBePersistedBeforeCurrentTermsAreAccepted() {
        val store = OnboardingStore(FakePrefs(), requiredTermsVersion = 1)

        assertFalse(store.markCompleted())
        assertEquals(OnboardingStep.WELCOME, store.read().currentStep(1))
    }

    @Test
    fun failedCommitDoesNotReportCompletion() {
        val preferences = FakePrefs()
        val store = OnboardingStore(preferences, requiredTermsVersion = 1)
        assertTrue(store.acceptTerms())
        preferences.setCommitResult(false)

        assertFalse(store.markCompleted())
        assertFalse(store.read().completed)
    }

    @Test
    fun separatePreferenceOwnersDoNotShareCompletion() {
        val first = OnboardingStore(FakePrefs(), requiredTermsVersion = 1)
        val second = OnboardingStore(FakePrefs(), requiredTermsVersion = 1)
        assertTrue(first.acceptTerms())
        assertTrue(first.markCompleted())

        assertEquals(OnboardingStep.WELCOME, second.read().currentStep(1))
    }

    @Test
    fun rootAvailabilityIsInformationalAndDoesNotBlockCompletion() {
        listOf(
            RootAccessProbe.Result.unknown(),
            RootAccessProbe.Result.unavailable(),
            RootAccessProbe.Result.available("KernelSU"),
        ).forEach { result ->
            val preferences = FakePrefs()
            val coordinator = coordinator(preferences) { callback -> callback(result) }
            assertTrue(coordinator.acceptTerms())

            var observed: RootAccessProbe.Result? = null
            coordinator.refreshRootAsync { observed = it }

            assertEquals(result.status, observed?.status)
            assertEquals(OnboardingStep.ROOT, coordinator.requiredStep())
            assertFalse(coordinator.state().completed)
            assertTrue(coordinator.complete())
            assertEquals(null, coordinator.requiredStep())
        }
    }

    @Test
    fun rootGrantMadeInManagerIsVisibleOnTheNextCapabilityRefresh() {
        val pendingRefreshes = ArrayDeque<(RootAccessProbe.Result?) -> Unit>()
        val coordinator = coordinator { callback -> pendingRefreshes.addLast(callback) }
        assertTrue(coordinator.acceptTerms())

        var observed: RootAccessProbe.Result? = null
        coordinator.refreshRootAsync { observed = it }
        pendingRefreshes.removeFirst()(RootAccessProbe.Result.unavailable())
        assertEquals(RootAccessProbe.Status.UNAVAILABLE, observed?.status)
        assertEquals(OnboardingStep.ROOT, coordinator.requiredStep())

        coordinator.refreshRootAsync { observed = it }
        pendingRefreshes.removeFirst()(RootAccessProbe.Result.available("KernelSU"))

        assertEquals(RootAccessProbe.Status.AVAILABLE, observed?.status)
        assertEquals(OnboardingStep.ROOT, coordinator.requiredStep())
        assertFalse(coordinator.state().completed)
    }

    private fun coordinator(
        preferences: FakePrefs = FakePrefs(),
        refresh: ((RootAccessProbe.Result?) -> Unit) -> Unit = { callback ->
            callback(RootAccessProbe.Result.unavailable())
        },
    ) = OnboardingCoordinator(
        OnboardingStore(preferences, requiredTermsVersion = 1),
        rootRefresh = refresh,
    )
}
