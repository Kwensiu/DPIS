package com.dpis.module.applist

import com.dpis.module.FakePrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreScopePromptStoreTest {
    @Test
    fun markPendingSurvivesANewStoreOnTheSamePreferences() {
        val prefs = FakePrefs()
        RestoreScopePromptStore(prefs).markPending()

        assertTrue(RestoreScopePromptStore(prefs).isPending())
    }

    @Test
    fun clearEndsTheOneShotPrompt() {
        val prefs = FakePrefs()
        val store = RestoreScopePromptStore(prefs)
        store.markPending()
        store.clear()

        assertFalse(store.isPending())
        assertFalse(RestoreScopePromptStore(prefs).isPending())
    }

    @Test
    fun restoredScopeReplacesPriorPromptIntent() {
        val store = RestoreScopePromptStore(FakePrefs())
        store.replacePendingScope(listOf("com.example.one", "com.example.two"))
        store.replacePendingScope(listOf("com.example.two"))

        assertTrue(store.isPending())
        assertTrue(store.scopePackages() == setOf("com.example.two"))
    }
}
