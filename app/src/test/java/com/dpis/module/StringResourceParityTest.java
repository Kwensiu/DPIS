package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class StringResourceParityTest {
    @Test
    public void defaultAndSimplifiedChineseStringsExposeSameNames()
            throws IOException, ParserConfigurationException, SAXException {
        Set<String> defaultNames = readStringNames("src/main/res/values/strings.xml");
        Set<String> chineseNames = readStringNames("src/main/res/values-zh-rCN/strings.xml");

        assertEquals(defaultNames, chineseNames);
    }

    @Test
    public void defaultStringsAreEnglishFallback()
            throws IOException, ParserConfigurationException, SAXException {
        String defaultStrings = read("src/main/res/values/strings.xml");

        assertTrue(defaultStrings.contains("<string name=\"settings_language_label\">Language</string>"));
        assertTrue(defaultStrings.contains("<string name=\"module_description\">Per-app DPI &amp; text size</string>"));
        assertEquals("\u7B80\u4F53\u4E2D\u6587",
                readStringValue("src/main/res/values/strings.xml", "settings_language_simplified_chinese"));
        assertEquals("\u8BED\u8A00",
                readStringValue("src/main/res/values-zh-rCN/strings.xml", "settings_language_label"));
    }

    @Test
    public void settingsScreenWiresLanguageSelector() throws IOException {
        String layout = read("src/main/res/layout/activity_system_server_settings.xml");
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String dialogLayout = read("src/main/res/layout/dialog_language_selection.xml");
        String localeManager = read("src/main/java/com/dpis/module/AppLocaleManager.java");

        assertTrue(layout.contains("android:id=\"@+id/row_language\""));
        assertTrue(dialogLayout.contains("android:id=\"@+id/language_options_container\""));
        assertTrue(source.contains("R.id.row_language"));
        assertTrue(source.contains("showLanguageDialog"));
        assertTrue(source.contains("AppLocaleManager.supportedLanguages()"));
        assertTrue(source.contains("createLanguageOptionButton("));
        assertTrue(source.contains("AppLocaleManager.setLanguageTag"));
        assertTrue(source.contains("dialog_language_selection"));
        assertTrue(source.contains("updateLanguageEntrySubtitle()"));
        assertTrue(source.contains("AppLocaleManager.selectedLabelResId(this)"));
        assertTrue(localeManager.contains("SUPPORTED_LANGUAGES = List.of("));
        assertTrue(localeManager.contains("static List<LanguageOption> supportedLanguages()"));
    }

    @Test
    public void languageSwitchDoesNotUseSavedInstanceStateForPersistedSwitches() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String localeManager = read("src/main/java/com/dpis/module/AppLocaleManager.java");

        assertTrue(!source.contains("STATE_HOOKS_SWITCH_CHECKED"));
        assertTrue(!source.contains("protected void onSaveInstanceState(Bundle outState)"));
        assertTrue(!source.contains("restoreSwitchStates(savedInstanceState)"));
        assertTrue(localeManager.contains("boolean setLanguageTag"));
        assertTrue(localeManager.contains(".commit()"));
    }

    @Test
    public void localeSwitchUsesWrappedBaseContextAndExplicitRecreate() throws IOException {
        String settingsSource = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String localizedSource = read("src/main/java/com/dpis/module/LocalizedActivity.java");
        String localeManager = read("src/main/java/com/dpis/module/AppLocaleManager.java");
        String mainSource = read("src/main/java/com/dpis/module/MainActivity.java");
        String aboutSource = read("src/main/java/com/dpis/module/AboutActivity.java");
        String licenseSource = read("src/main/java/com/dpis/module/OpenSourceLicenseActivity.java");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(localizedSource.contains("extends Activity"));
        assertTrue(localeManager.contains("Context wrap(Context context)"));
        assertTrue(localeManager.contains("createConfigurationContext(configuration)"));
        assertTrue(localeManager.contains("Context.MODE_PRIVATE"));
        assertTrue(localizedSource.contains("attachBaseContext("));
        assertTrue(localizedSource.contains("protected void onResume()"));
        assertTrue(localizedSource.contains("AppLocaleManager.getLanguageTag(this)"));
        assertTrue(localizedSource.contains("recreate();"));
        assertTrue(mainSource.contains("extends LocalizedActivity"));
        assertTrue(aboutSource.contains("extends LocalizedActivity"));
        assertTrue(licenseSource.contains("extends LocalizedActivity"));
        assertTrue(settingsSource.contains("extends LocalizedActivity"));
        assertTrue(settingsSource.contains("recreate();"));
        assertTrue(!manifest.contains("AppLocalesMetadataHolderService"));
    }

    private static Set<String> readStringNames(String relativePath)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try (InputStream input = Files.newInputStream(Path.of(relativePath))) {
            Document document = factory.newDocumentBuilder().parse(input);
            NodeList strings = document.getElementsByTagName("string");
            Set<String> names = new LinkedHashSet<>();
            for (int i = 0; i < strings.getLength(); i++) {
                names.add(strings.item(i).getAttributes().getNamedItem("name").getTextContent());
            }
            return names;
        }
    }

    private static String readStringValue(String relativePath, String name)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try (InputStream input = Files.newInputStream(Path.of(relativePath))) {
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
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
