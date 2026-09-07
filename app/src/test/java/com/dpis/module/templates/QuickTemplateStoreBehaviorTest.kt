package com.dpis.module.templates

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Protects the persisted catalog contract independently from the template editor UI. */
class QuickTemplateStoreBehaviorTest {
    @Test
    fun reorderKeepsKnownTemplatesOnceAndAppendsUnlistedTemplates() {
        val prefs = FakePrefs()
        val store = QuickTemplateStore(prefs)
        assertTrue(store.save(template("alpha", "Alpha", 1L)))
        assertTrue(store.save(template("beta", "Beta", 2L)))
        assertTrue(store.save(template("gamma", "Gamma", 3L)))

        assertTrue(store.reorder(listOf("beta", "missing", "beta")))

        assertEquals(
            listOf("beta", "alpha", "gamma"),
            store.readAll().map { it.id },
        )
        assertEquals("beta\nalpha\ngamma", prefs.getString(QuickTemplateStore.KEY_TEMPLATE_ORDER, null))
    }

    @Test
    fun reorderingAnEmptyCatalogClearsStaleOrderMetadata() {
        val prefs = FakePrefs()
        prefs.edit().putString(QuickTemplateStore.KEY_TEMPLATE_ORDER, "stale").commit()
        val store = QuickTemplateStore(prefs)

        assertTrue(store.reorder(emptyList()))

        assertNull(prefs.getString(QuickTemplateStore.KEY_TEMPLATE_ORDER, null))
        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun backupCopyIsIndependentFromLaterCallerMutationAndRestoresCatalog() {
        val source = QuickTemplateStore(FakePrefs())
        assertTrue(source.save(template("daily", "Daily", 5L, linkedSetOf("com.example.one"))))
        val backup = linkedMapOf<String, Any?>()

        source.copyToBackup(backup)
        @Suppress("UNCHECKED_CAST")
        (backup["template.daily.selected_packages"] as MutableSet<String>).add("com.example.mutated")

        assertEquals(setOf("com.example.one"), source.read("daily")!!.selectedPackages)
        val restored = QuickTemplateStore(FakePrefs())
        assertTrue(restored.restoreFromBackup(backup))
        assertEquals(
            setOf("com.example.one", "com.example.mutated"),
            restored.read("daily")!!.selectedPackages,
        )
    }

    @Test
    fun invalidIdentifiersNeverCreateOrMutateCatalogEntries() {
        val store = QuickTemplateStore(FakePrefs())

        assertFalse(store.save(template("invalid.id", "Invalid", 1L)))
        assertFalse(store.setSelectedPackages("invalid.id", linkedSetOf("com.example.one")))
        assertFalse(store.delete("invalid.id"))

        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun duplicateNamesRespectTheExcludedTemplate() {
        val store = QuickTemplateStore(FakePrefs())
        assertTrue(store.save(template("alpha", "  Shared name  ", 1L)))
        assertTrue(store.save(template("beta", "Other", 2L)))

        assertTrue(store.hasDuplicateName("shared name", null))
        assertFalse(store.hasDuplicateName("shared name", "alpha"))
        assertFalse(store.hasDuplicateName("missing", null))
    }

    @Test
    fun selectedPackagesAndDeleteUpdatePersistedTemplateState() {
        val store = QuickTemplateStore(FakePrefs())
        assertTrue(store.save(template("alpha", "Alpha", 1L)))

        assertTrue(store.setSelectedPackages("alpha", linkedSetOf("com.example.app")))
        assertEquals(setOf("com.example.app"), store.read("alpha")!!.selectedPackages)
        assertTrue(store.delete("alpha"))
        assertNull(store.read("alpha"))
        assertTrue(store.readAll().isEmpty())
    }

    private fun template(
        id: String,
        name: String,
        updatedAt: Long,
        selectedPackages: MutableSet<String?> = linkedSetOf(),
    ) = QuickTemplateStore.QuickTemplate(
        id,
        name,
        updatedAt,
        selectedPackages,
        TemplateConfigValue.EMPTY,
    )
}
