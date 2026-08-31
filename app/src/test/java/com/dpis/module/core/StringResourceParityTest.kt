package com.dpis.module

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.InputStream
import java.util.LinkedHashSet
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceParityTest {
    @Test
    fun localizedStringsExposeOnlyTranslatableNames() {
        val defaultNames = readStringNames("src/main/res/values/strings.xml", true)
        val defaultNonTranslatableNames = readStringNames("src/main/res/values/strings.xml", false)

        assertLocalizedStringNames(defaultNames, defaultNonTranslatableNames, "src/main/res/values-zh-rCN/strings.xml")
        assertLocalizedStringNamesAllowingFallback(defaultNames, defaultNonTranslatableNames, "src/main/res/values-ja-rJP/strings.xml")
        assertLocalizedStringNamesAllowingFallback(defaultNames, defaultNonTranslatableNames, "src/main/res/values-ru-rRU/strings.xml")
    }

    @Test
    fun defaultStringsAreEnglishFallback() {
        val defaultStrings = read("src/main/res/values/strings.xml")
        defaultStrings.assertContainsAll(
            "<string name=\"settings_language_label\">Language</string>",
            "<string name=\"module_description\">Per-app DPI &amp; text size</string>",
        )
        assertEquals("\u7b80\u4f53\u4e2d\u6587", readStringValue("src/main/res/values/strings.xml", "settings_language_simplified_chinese"))
        assertEquals("\u65e5\u672c\u8a9e\uff08\u672a\u6821\u6b63\uff09", readStringValue("src/main/res/values/strings.xml", "settings_language_japanese"))
        assertEquals("\u0420\u0443\u0441\u0441\u043a\u0438\u0439", readStringValue("src/main/res/values/strings.xml", "settings_language_russian"))
        assertEquals("\u8bed\u8a00", readStringValue("src/main/res/values-zh-rCN/strings.xml", "settings_language_label"))
    }

    @Test
    fun settingsScreenWiresLanguageSelector() {
        val layout = read("src/main/res/layout/view_system_server_settings_content.xml")
        val source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java")
        val dialogs = read("src/main/java/com/dpis/module/settings/presentation/SettingsComposeDialogs.kt")
        val dialogLayout = read("src/main/java/com/dpis/module/ui/dialog/DialogLayout.kt")
        val localeManager = read("src/main/java/com/dpis/module/settings/AppLocaleManager.java")
        layout.assertContainsAll("android:id=\"@+id/row_language\"")
        dialogs.assertContainsAll("LanguageDialogContent(")
        assertTrue(dialogs.indexOf("dismiss()", dialogs.indexOf("onSelected = {")) >= 0)
        assertTrue(dialogs.indexOf("onSelected.accept(selectedTag)") > dialogs.indexOf("dismiss()", dialogs.indexOf("onSelected = {")))
        dialogLayout.assertContainsAll("R.dimen.dialog_surface_padding_horizontal", "R.dimen.dialog_action_spacing_top")
        source.assertContainsAll("R.id.row_language", "bindLanguageRow()", "showLanguageDialog", "AppLocaleManager.supportedLanguages()", "new LanguageDialogOption(", "AppLocaleManager.setLanguageTag", "SettingsComposeDialogs.showLanguage", "updateLanguageEntrySubtitle()", "AppLocaleManager.selectedLabelResId(activity)")
        source.assertNotContainsAll("settings_language_hint")
        localeManager.assertContainsAll("SUPPORTED_LANGUAGES = List.of(", "TAG_JAPANESE", "R.string.settings_language_japanese", "TAG_RUSSIAN", "R.string.settings_language_russian", "static List<LanguageOption> supportedLanguages()")
    }

    @Test
    fun languageSwitchDoesNotUseSavedInstanceStateForPersistedSwitches() {
        read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java").assertNotContainsAll("STATE_HOOKS_SWITCH_CHECKED", "protected void onSaveInstanceState(Bundle outState)", "restoreSwitchStates(savedInstanceState)")
        read("src/main/java/com/dpis/module/settings/AppLocaleManager.java").assertContainsAll("boolean setLanguageTag", ".commit()")
    }

    @Test
    fun localeSwitchUsesWrappedBaseContextAndExplicitRecreate() {
        val settings = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java")
        val localized = read("src/main/java/com/dpis/module/LocalizedActivity.kt")
        val localeManager = read("src/main/java/com/dpis/module/settings/AppLocaleManager.java")
        val main = read("src/main/java/com/dpis/module/MainActivity.java")
        val about = read("src/main/java/com/dpis/module/about/AboutActivity.kt")
        val license = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")
        val manifest = read("src/main/AndroidManifest.xml")
        localized.assertContainsAll(": ComponentActivity()", "attachBaseContext(", "override fun onResume()", "AppLocaleManager.getLanguageTag(this)", "recreate()", "ThemeModeStore.getAppearance(this)")
        localeManager.assertContainsAll("Context wrap(Context context)", "createConfigurationContext(configuration)", "Context.MODE_PRIVATE")
        main.assertContainsAll("extends LocalizedActivity", "new SystemServerSettingsPageController(")
        about.assertContainsAll(": LocalizedActivity()")
        license.assertContainsAll("extends LocalizedActivity")
        manifest.assertContainsAll("android:supportsRtl=\"false\"")
        manifest.assertNotContainsAll("AppLocalesMetadataHolderService")
        settings.assertContainsAll("SystemServerSettingsPageController(LocalizedActivity activity")
    }

    private fun assertLocalizedStringNames(defaultNames: Set<String>, defaultsNonTranslatable: Set<String>, localizedPath: String) {
        val localizedNames = readStringNames(localizedPath)
        assertEquals(defaultNames, localizedNames)
        defaultsNonTranslatable.forEach { assertTrue(!localizedNames.contains(it)) }
    }

    private fun assertLocalizedStringNamesAllowingFallback(defaultNames: Set<String>, defaultsNonTranslatable: Set<String>, localizedPath: String) {
        val localizedNames = readStringNames(localizedPath)
        assertTrue(defaultNames.containsAll(localizedNames))
        defaultsNonTranslatable.forEach { assertTrue(!localizedNames.contains(it)) }
    }

    private fun readStringNames(relativePath: String, translatable: Boolean? = null): Set<String> {
        val factory = DocumentBuilderFactory.newInstance().apply { isIgnoringComments = true }
        SourceSmokeTestPaths.open(relativePath).use { input: InputStream ->
            val strings = factory.newDocumentBuilder().parse(input).getElementsByTagName("string")
            return LinkedHashSet<String>().apply {
                repeat(strings.length) { index ->
                    val element = strings.item(index) as Element
                    val isTranslatable = element.getAttribute("translatable") != "false"
                    if (translatable == null || translatable == isTranslatable) add(element.getAttribute("name"))
                }
            }
        }
    }

    private fun readStringValue(relativePath: String, name: String): String {
        val factory = DocumentBuilderFactory.newInstance().apply { isIgnoringComments = true }
        SourceSmokeTestPaths.open(relativePath).use { input ->
            val strings = factory.newDocumentBuilder().parse(input).getElementsByTagName("string")
            repeat(strings.length) { index ->
                val element = strings.item(index) as Element
                if (name == element.getAttribute("name")) return element.textContent
            }
        }
        error("Missing string resource: $name")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
    private fun String.assertContainsAll(vararg needles: String) = needles.forEach { assertTrue("Missing $it", contains(it)) }
    private fun String.assertNotContainsAll(vararg needles: String) = needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
}
