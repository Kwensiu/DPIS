package com.dpis.module.updates;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ReleaseNotesMarkdownLite {
    private static final Pattern VERSION_HEADING_PATTERN = Pattern.compile(
            "^##\\s*+\\[[^]]++]\\(https?://[^)]++\\)\\s*+\\(\\d{4}-\\d{2}-\\d{2}\\)\\s*$");
    private static final Pattern GENERATED_SECTION_PATTERN = Pattern.compile(
            "^##\\s+(?:new contributors|contributors)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FULL_CHANGELOG_PATTERN = Pattern.compile(
            "^(?:#+\\s*)?\\*?\\*?full changelog\\*?\\*?:?.*$",
            Pattern.CASE_INSENSITIVE);

    private ReleaseNotesMarkdownLite() {
    }

    public static String filterBodyForLocale(String markdown, Locale locale) {
        String normalized = markdown != null ? markdown.replace("\r\n", "\n") : "";
        String[] lines = normalized.split("\n", -1);
        int start = 0;
        while (start < lines.length && lines[start].trim().isEmpty()) {
            start++;
        }
        if (start < lines.length && VERSION_HEADING_PATTERN.matcher(lines[start].trim()).matches()) {
            start++;
            while (start < lines.length && lines[start].trim().isEmpty()) {
                start++;
            }
        }

        StringBuilder body = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            body.append(lines[i]);
            if (i < lines.length - 1) {
                body.append('\n');
            }
        }
        String content = removeGeneratedReleaseMetadata(body.toString()).trim();
        String[] sectionSplit = splitByDividerLine(content);
        if (sectionSplit == null) {
            return content.trim();
        }

        String chinesePart = sectionSplit[0].trim();
        String englishPart = sectionSplit[1].trim();
        boolean isChinese = locale != null && locale.getLanguage().startsWith("zh");
        String preferred = isChinese ? chinesePart : englishPart;
        if (!preferred.isEmpty()) {
            return preferred;
        }
        return (englishPart + "\n" + chinesePart).trim();
    }

    /** Removes GitHub-generated publication metadata while retaining release-note prose. */
    private static String removeGeneratedReleaseMetadata(String content) {
        String[] lines = content.split("\\n", -1);
        StringBuilder filtered = new StringBuilder();
        boolean skippingGeneratedSection = false;
        for (String raw : lines) {
            String line = raw != null ? raw : "";
            String trimmed = line.trim();
            if (GENERATED_SECTION_PATTERN.matcher(trimmed).matches()) {
                skippingGeneratedSection = true;
                continue;
            }
            if (skippingGeneratedSection && trimmed.startsWith("#")) {
                skippingGeneratedSection = false;
            }
            if (skippingGeneratedSection || FULL_CHANGELOG_PATTERN.matcher(trimmed).matches()) {
                continue;
            }
            appendLine(filtered, line);
        }
        return filtered.toString();
    }

    private static String[] splitByDividerLine(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder top = new StringBuilder();
        StringBuilder bottom = new StringBuilder();
        boolean foundDivider = false;
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (!foundDivider && line.matches("-{3,}")) {
                foundDivider = true;
                continue;
            }
            if (!foundDivider) {
                appendLine(top, raw);
            } else {
                appendLine(bottom, raw);
            }
        }
        if (!foundDivider) {
            return null;
        }
        return new String[] { top.toString(), bottom.toString() };
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
