package com.dpis.module;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontStyle;
import android.graphics.fonts.SystemFonts;
import android.os.Build;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class SystemFontRegistry {
    private static final String FILE_ID_PREFIX = "system-font:";
    private static final String FAMILY_ID_PREFIX = "system-family:";
    private static final String ID_SEPARATOR = ":";
    private static final File[] FONT_CONFIG_FILES = {
            new File("/system/etc/fonts.xml"),
            new File("/system_ext/etc/hyper_fonts.xml"),
            new File("/system_ext/etc/miui_fonts.xml"),
            new File("/product/etc/mi_fonts_customization.xml")
    };
    private static final RecommendedFamily[] RECOMMENDED_FAMILIES = {
            new RecommendedFamily("sans-serif", "Sans Serif"),
            new RecommendedFamily("sans-serif-condensed", "Sans Serif Condensed"),
            new RecommendedFamily("serif", "Serif"),
            new RecommendedFamily("monospace", "Monospace"),
            new RecommendedFamily("source-sans-pro", "Source Sans Pro"),
            new RecommendedFamily("roboto-flex", "Roboto Flex"),
            new RecommendedFamily("cursive", "Cursive")
    };

    private SystemFontRegistry() {
    }

    static boolean isSystemFontId(String typefaceId) {
        return typefaceId != null
                && (typefaceId.startsWith(FILE_ID_PREFIX) || typefaceId.startsWith(FAMILY_ID_PREFIX));
    }

    static List<SystemFontEntry> listRecommendedFonts() {
        return listRecommendedFonts(readDeclaredFamilyNames());
    }

    @SuppressLint("NewApi")
    static List<SystemFontEntry> listAvailableFonts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return List.of();
        }
        Map<String, SystemFontEntry> entries = new LinkedHashMap<>();
        try {
            for (Font font : SystemFonts.getAvailableFonts()) {
                File file = font.getFile();
                if (file == null || !file.canRead()) {
                    continue;
                }
                String id = buildFontId(file.getAbsolutePath(), font.getTtcIndex());
                entries.putIfAbsent(id, new SystemFontEntry(id, formatDisplayName(font)));
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        List<SystemFontEntry> result = new ArrayList<>(entries.values());
        result.sort((first, second) -> first.displayName.compareToIgnoreCase(second.displayName));
        return result;
    }

    static Typeface loadTypeface(String typefaceId) {
        String familyName = decodeFamilyName(typefaceId);
        if (familyName != null) {
            try {
                return Typeface.create(familyName, Typeface.NORMAL);
            } catch (Throwable ignored) {
                return null;
            }
        }
        Font font = findFontById(typefaceId);
        if (font == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        File file = font.getFile();
        if (!file.canRead()) {
            return null;
        }
        try {
            return new Typeface.Builder(file)
                    .setTtcIndex(font.getTtcIndex())
                    .build();
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String buildFontIdForTest(String path, int ttcIndex) {
        return buildFontId(path, ttcIndex);
    }

    static String buildFamilyIdForTest(String familyName) {
        return buildFamilyId(familyName);
    }

    static List<SystemFontEntry> listRecommendedFontsForTest(Set<String> declaredFamilyNames) {
        return listRecommendedFonts(declaredFamilyNames);
    }

    private static List<SystemFontEntry> listRecommendedFonts(Set<String> declaredFamilyNames) {
        if (declaredFamilyNames == null || declaredFamilyNames.isEmpty()) {
            return List.of();
        }
        List<SystemFontEntry> result = new ArrayList<>();
        for (RecommendedFamily family : RECOMMENDED_FAMILIES) {
            if (declaredFamilyNames.contains(family.name)) {
                result.add(new SystemFontEntry(buildFamilyId(family.name), family.displayName));
            }
        }
        return result;
    }

    private static String buildFamilyId(String familyName) {
        if (familyName == null || familyName.isBlank()) {
            return null;
        }
        return FAMILY_ID_PREFIX + familyName.trim();
    }

    private static String decodeFamilyName(String typefaceId) {
        if (typefaceId == null || !typefaceId.startsWith(FAMILY_ID_PREFIX)) {
            return null;
        }
        String familyName = typefaceId.substring(FAMILY_ID_PREFIX.length()).trim();
        return familyName.isEmpty() ? null : familyName;
    }

    private static String buildFontId(String path, int ttcIndex) {
        if (path == null || path.isBlank() || ttcIndex < 0) {
            return null;
        }
        return FILE_ID_PREFIX + hashPathAndIndex(path, ttcIndex);
    }

    @SuppressLint("NewApi")
    private static Font findFontById(String typefaceId) {
        if (typefaceId == null || !typefaceId.startsWith(FILE_ID_PREFIX)) {
            return null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        try {
            for (Font font : SystemFonts.getAvailableFonts()) {
                File file = font.getFile();
                if (file == null || !file.canRead()) {
                    continue;
                }
                String candidateId = buildFontId(file.getAbsolutePath(), font.getTtcIndex());
                if (typefaceId.equals(candidateId)) {
                    return font;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    @SuppressLint("NewApi")
    private static String formatDisplayName(Font font) {
        File file = font.getFile();
        String name = file != null ? file.getName() : "System font";
        FontStyle style = font.getStyle();
        String slant = style.getSlant() == FontStyle.FONT_SLANT_ITALIC ? "italic" : "normal";
        String suffix = "w" + style.getWeight() + ", " + slant;
        if (font.getTtcIndex() > 0) {
            suffix += ", ttc " + font.getTtcIndex();
        }
        return String.format(Locale.US, "%s (%s)", name, suffix);
    }

    private static Set<String> readDeclaredFamilyNames() {
        Set<String> result = new LinkedHashSet<>();
        for (File file : FONT_CONFIG_FILES) {
            result.addAll(readDeclaredFamilyNames(file));
        }
        return result;
    }

    private static Set<String> readDeclaredFamilyNames(File file) {
        if (file == null || !file.canRead()) {
            return Set.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            NodeList families = factory.newDocumentBuilder()
                    .parse(file)
                    .getElementsByTagName("family");
            Set<String> result = new LinkedHashSet<>();
            for (int i = 0; i < families.getLength(); i++) {
                if (!(families.item(i) instanceof Element element)) {
                    continue;
                }
                String name = element.getAttribute("name");
                if (name != null && !name.isBlank()) {
                    result.add(name.trim());
                }
            }
            return result;
        } catch (Throwable ignored) {
            return Set.of();
        }
    }

    private static String hashPathAndIndex(String path, int ttcIndex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((path + ID_SEPARATOR + ttcIndex).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                builder.append(String.format(Locale.US, "%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString((path + ID_SEPARATOR + ttcIndex).hashCode());
        }
    }

    private static final class RecommendedFamily {
        final String name;
        final String displayName;

        RecommendedFamily(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }
}
