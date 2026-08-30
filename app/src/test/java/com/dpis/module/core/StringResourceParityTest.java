package com.dpis.module;

import com.dpis.module.settings.AppLocaleManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class StringResourceParityTest {
    @Test
    public void localizedStringsExposeOnlyTranslatableNames()
            throws IOException, ParserConfigurationException, SAXException {
        Set<String> defaultNames = readStringNames(
                "src/main/res/values/strings.xml",
                true);
        Set<String> defaultNonTranslatableNames = readStringNames(
                "src/main/res/values/strings.xml",
                false);

        assertLocalizedStringNames(defaultNames, defaultNonTranslatableNames,
                "src/main/res/values-zh-rCN/strings.xml");
        assertLocalizedStringNamesAllowingFallback(defaultNames, defaultNonTranslatableNames,
                "src/main/res/values-ja-rJP/strings.xml");
        assertLocalizedStringNamesAllowingFallback(defaultNames, defaultNonTranslatableNames,
                "src/main/res/values-ru-rRU/strings.xml");
    }

    @Test
    public void defaultStringsAreEnglishFallback()
            throws IOException, ParserConfigurationException, SAXException {
        String defaultStrings = read("src/main/res/values/strings.xml");

        assertTrue(defaultStrings.contains("<string name=\"settings_language_label\">Language</string>"));
        assertTrue(defaultStrings.contains("<string name=\"module_description\">Per-app DPI &amp; text size</string>"));
        assertEquals("\u7B80\u4F53\u4E2D\u6587",
                readStringValue("src/main/res/values/strings.xml", "settings_language_simplified_chinese"));
        assertEquals("\u65E5\u672C\u8A9E\uFF08\u672A\u6821\u6B63\uFF09",
                readStringValue("src/main/res/values/strings.xml", "settings_language_japanese"));
        assertEquals("\u0420\u0443\u0441\u0441\u043A\u0438\u0439",
                readStringValue("src/main/res/values/strings.xml", "settings_language_russian"));
        assertEquals("\u8BED\u8A00",
                readStringValue("src/main/res/values-zh-rCN/strings.xml", "settings_language_label"));
    }

    @Test
    public void settingsScreenWiresLanguageSelector() throws IOException {
        String layout = read("src/main/res/layout/view_system_server_settings_content.xml");
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String dialogs = read("src/main/java/com/dpis/module/settings/presentation/SettingsComposeDialogs.kt");
        String localeManager = read("src/main/java/com/dpis/module/settings/AppLocaleManager.java");

        assertTrue(layout.contains("android:id=\"@+id/row_language\""));
        assertTrue(dialogs.contains("LanguageDialogContent("));
        int dismissIndex = dialogs.indexOf("dismiss()", dialogs.indexOf("onSelected = {"));
        int callbackIndex = dialogs.indexOf("onSelected.accept(selectedTag)", dismissIndex);
        assertTrue(dismissIndex >= 0);
        assertTrue(callbackIndex > dismissIndex);
        assertTrue(dialogs.contains("R.dimen.dialog_surface_padding_horizontal"));
        assertTrue(dialogs.contains("R.dimen.dialog_action_spacing_top"));
        assertTrue(source.contains("R.id.row_language"));
        assertTrue(source.contains("bindLanguageRow()"));
        assertTrue(source.contains("showLanguageDialog"));
        assertTrue(source.contains("AppLocaleManager.supportedLanguages()"));
        assertTrue(source.contains("new LanguageDialogOption("));
        assertTrue(source.contains("AppLocaleManager.setLanguageTag"));
        assertTrue(source.contains("SettingsComposeDialogs.showLanguage"));
        assertTrue(source.contains("updateLanguageEntrySubtitle()"));
        assertTrue(source.contains("AppLocaleManager.selectedLabelResId(activity)"));
        assertTrue(!source.contains("settings_language_hint"));
        assertTrue(localeManager.contains("SUPPORTED_LANGUAGES = List.of("));
        assertTrue(localeManager.contains("TAG_JAPANESE"));
        assertTrue(localeManager.contains("R.string.settings_language_japanese"));
        assertTrue(localeManager.contains("TAG_RUSSIAN"));
        assertTrue(localeManager.contains("R.string.settings_language_russian"));
        assertTrue(localeManager.contains("static List<LanguageOption> supportedLanguages()"));
    }

    @Test
    public void languageSwitchDoesNotUseSavedInstanceStateForPersistedSwitches() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String localeManager = read("src/main/java/com/dpis/module/settings/AppLocaleManager.java");

        assertTrue(!source.contains("STATE_HOOKS_SWITCH_CHECKED"));
        assertTrue(!source.contains("protected void onSaveInstanceState(Bundle outState)"));
        assertTrue(!source.contains("restoreSwitchStates(savedInstanceState)"));
        assertTrue(localeManager.contains("boolean setLanguageTag"));
        assertTrue(localeManager.contains(".commit()"));
    }

    @Test
    public void localeSwitchUsesWrappedBaseContextAndExplicitRecreate() throws IOException {
        String settingsSource = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String localizedSource = read("src/main/java/com/dpis/module/LocalizedActivity.kt");
        String localeManager = read("src/main/java/com/dpis/module/settings/AppLocaleManager.java");
        String mainSource = read("src/main/java/com/dpis/module/MainActivity.java");
        String aboutSource = read("src/main/java/com/dpis/module/about/AboutActivity.java");
        String licenseSource = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(localizedSource.contains(": ComponentActivity()"));
        assertTrue(localeManager.contains("Context wrap(Context context)"));
        assertTrue(localeManager.contains("createConfigurationContext(configuration)"));
        assertTrue(localeManager.contains("Context.MODE_PRIVATE"));
        assertTrue(localizedSource.contains("attachBaseContext("));
        assertTrue(localizedSource.contains("override fun onResume()"));
        assertTrue(localizedSource.contains("AppLocaleManager.getLanguageTag(this)"));
        assertTrue(localizedSource.contains("recreate()"));
        assertTrue(localizedSource.contains("ThemeModeStore.getAppearance(this)"));
        assertTrue(mainSource.contains("extends LocalizedActivity"));
        assertTrue(aboutSource.contains("extends LocalizedActivity"));
        assertTrue(licenseSource.contains("extends LocalizedActivity"));
        assertTrue(mainSource.contains("new SystemServerSettingsPageController("));
        assertTrue(settingsSource.contains("SystemServerSettingsPageController(LocalizedActivity activity"));
        assertTrue(manifest.contains("android:supportsRtl=\"false\""));
        assertTrue(!manifest.contains("AppLocalesMetadataHolderService"));
    }

    private static Set<String> readStringNames(String relativePath)
            throws IOException, ParserConfigurationException, SAXException {
        return readStringNames(relativePath, null);
    }

    private static void assertLocalizedStringNames(
            Set<String> defaultNames,
            Set<String> defaultNonTranslatableNames,
            String localizedPath)
            throws IOException, ParserConfigurationException, SAXException {
        Set<String> localizedNames = readStringNames(localizedPath);

        assertEquals(defaultNames, localizedNames);
        for (String name : defaultNonTranslatableNames) {
            assertTrue(!localizedNames.contains(name));
        }
    }

    private static void assertLocalizedStringNamesAllowingFallback(
            Set<String> defaultNames,
            Set<String> defaultNonTranslatableNames,
            String localizedPath)
            throws IOException, ParserConfigurationException, SAXException {
        Set<String> localizedNames = readStringNames(localizedPath);

        assertTrue(defaultNames.containsAll(localizedNames));
        for (String name : defaultNonTranslatableNames) {
            assertTrue(!localizedNames.contains(name));
        }
    }

    private static Set<String> readStringNames(String relativePath, Boolean translatable)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try (InputStream input = SourceSmokeTestPaths.open(relativePath)) {
            Document document = factory.newDocumentBuilder().parse(input);
            NodeList strings = document.getElementsByTagName("string");
            Set<String> names = new LinkedHashSet<>();
            for (int i = 0; i < strings.getLength(); i++) {
                Element string = (Element) strings.item(i);
                boolean isTranslatable = !"false".equals(string.getAttribute("translatable"));
                if (translatable == null || translatable == isTranslatable) {
                    names.add(string.getAttribute("name"));
                }
            }
            return names;
        }
    }

    private static String readStringValue(String relativePath, String name)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try (InputStream input = SourceSmokeTestPaths.open(relativePath)) {
            Document document = factory.newDocumentBuilder().parse(input);
            NodeList strings = document.getElementsByTagName("string");
            for (int i = 0; i < strings.getLength(); i++) {
                Element string = (Element) strings.item(i);
                if (name.equals(string.getAttribute("name"))) {
                    return string.getTextContent();
                }
            }
        }
        throw new IllegalArgumentException("Missing string resource: " + name);
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
