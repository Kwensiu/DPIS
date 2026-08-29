package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.viewport.ViewportTargetSpec
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceXmlTest {
    @Test
    fun mirroredLegacySharedPrefsXmlContainsCommittedPackageConfig() {
        val mirror = Files.createTempFile("dpis-mirror", ".xml").toFile()
        val store = DpisConfigStore(FakePrefs(), mirror)

        assertTrue(store.setTargetViewportSpec("com.azure.authenticator", ViewportTargetSpec.relativeScale(150000)))
        assertTrue(store.setTargetFontScalePercent("com.azure.authenticator", 150))

        val xml = mirror.readText(StandardCharsets.UTF_8)
        assertTrue(xml.contains("package_config.com.azure.authenticator.viewport.target_type"))
        assertTrue(xml.contains("package_config.com.azure.authenticator.viewport.scale_permille"))
        assertTrue(xml.contains("package_config.com.azure.authenticator.font.scale_percent"))
        assertTrue(xml.contains("<string>com.azure.authenticator</string>"))
    }

    @Test
    fun mirroredLegacySharedPrefsXmlEscapesStringsAndSets() {
        val mirror = Files.createTempFile("dpis-mirror", ".xml").toFile()
        val entries = linkedMapOf<String, Any>(
            "name\"key" to "value<&>",
            "packages" to linkedSetOf("a&b", "c<d"),
        )

        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror)

        val xml = mirror.readText(StandardCharsets.UTF_8)
        assertTrue(xml.contains("name=\"name&quot;key\""))
        assertTrue(xml.contains(">value&lt;&amp;&gt;</string>"))
        assertTrue(xml.contains("<string>a&amp;b</string>"))
        assertTrue(xml.contains("<string>c&lt;d</string>"))
    }

    @Test
    fun sharedPreferencesXmlRoundTripsForLegacyImport() {
        val mirror = Files.createTempFile("dpis-mirror", ".xml").toFile()
        val entries = linkedMapOf<String, Any>(
            DpisConfigStore.KEY_TARGET_PACKAGES to linkedSetOf("com.example.one", "com.example.two"),
            "package_config.com.example.one.viewport.target_type" to "relative_scale",
            "package_config.com.example.one.viewport.scale_permille" to 1500,
            "package_config.com.example.two.font.scale_percent" to 120,
            "package_config.com.example.two.target.dpis_enabled" to false,
        )

        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror)
        val imported = DpisConfigStore.readSharedPreferencesXmlForTest(mirror)

        assertEquals(entries[DpisConfigStore.KEY_TARGET_PACKAGES], imported[DpisConfigStore.KEY_TARGET_PACKAGES])
        assertEquals("relative_scale", imported["package_config.com.example.one.viewport.target_type"])
        assertEquals(1500, imported["package_config.com.example.one.viewport.scale_permille"])
        assertEquals(120, imported["package_config.com.example.two.font.scale_percent"])
        assertEquals(false, imported["package_config.com.example.two.target.dpis_enabled"])
    }

    @Test
    fun importSharedPreferencesXmlReplacesPrimaryStore() {
        val mirror = Files.createTempFile("dpis-mirror", ".xml").toFile()
        val entries = linkedMapOf<String, Any>(
            DpisConfigStore.KEY_TARGET_PACKAGES to linkedSetOf("com.example.one", "com.example.two"),
            "package_config.com.example.one.viewport.target_type" to "relative_scale",
            "package_config.com.example.one.viewport.scale_permille" to 1500,
        )
        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror)
        val store = DpisConfigStore(FakePrefs(), mirror)

        assertTrue(store.importSharedPreferencesXml(mirror))
        assertTrue(store.getConfiguredPackages().contains("com.example.one"))
        assertEquals(ViewportTargetSpec.relativeScale(150000), store.getTargetViewportSpec("com.example.one"))
    }
}
