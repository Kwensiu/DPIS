package com.dpis.module;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseNotesMarkdownLite {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((https?://[^)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern VERSION_HEADING_PATTERN = Pattern.compile(
            "^##\\s*\\[.+?\\]\\(https?://[^)]+\\)\\s*\\(\\d{4}-\\d{2}-\\d{2}\\)\\s*$");

    private ReleaseNotesMarkdownLite() {
    }

    static CharSequence format(String markdown, Locale locale) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        String filtered = filterBodyForLocale(markdown, locale);
        if (filtered.trim().isEmpty()) {
            return "";
        }
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = filtered.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.equals("---")) {
                appendLine(out, "────────");
                continue;
            }
            if (line.startsWith("### ")) {
                appendHeading(out, line.substring(4), 1.06f);
                continue;
            }
            if (line.startsWith("## ")) {
                appendHeading(out, line.substring(3), 1.14f);
                continue;
            }
            if (line.startsWith("* ")) {
                appendStyledText(out, "• " + line.substring(2));
                appendNewLineIfNeeded(out, i, lines.length);
                continue;
            }
            appendStyledText(out, rawLine);
            appendNewLineIfNeeded(out, i, lines.length);
        }
        return out;
    }

    static String filterBodyForLocale(String markdown, Locale locale) {
        String normalized = markdown.replace("\r\n", "\n");
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
        String content = body.toString();
        String[] sectionSplit = splitByDividerLine(content);
        if (sectionSplit == null) {
            return content.trim();
        }

        String englishPart = sectionSplit[0].trim();
        String chinesePart = sectionSplit[1].trim();
        boolean isChinese = locale != null && locale.getLanguage().startsWith("zh");
        String preferred = isChinese ? chinesePart : englishPart;
        if (!preferred.isEmpty()) {
            return preferred;
        }
        return (englishPart + "\n" + chinesePart).trim();
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
                if (top.length() > 0) {
                    top.append('\n');
                }
                top.append(raw);
            } else {
                if (bottom.length() > 0) {
                    bottom.append('\n');
                }
                bottom.append(raw);
            }
        }
        if (!foundDivider) {
            return null;
        }
        return new String[] { top.toString(), bottom.toString() };
    }

    private static void appendHeading(SpannableStringBuilder out, String text, float scale) {
        int start = out.length();
        appendStyledText(out, text);
        int end = out.length();
        out.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new RelativeSizeSpan(scale), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append('\n');
    }

    private static void appendStyledText(SpannableStringBuilder out, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int contentStart = out.length();
        CharSequence segment = replaceLinks(text);
        out.append(segment);
        int contentEnd = out.length();
        applyInlinePattern(out, contentStart, contentEnd, BOLD_PATTERN, true);
        applyInlinePattern(out, contentStart, contentEnd, CODE_PATTERN, false);
    }

    private static CharSequence replaceLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int last = 0;
        while (matcher.find()) {
            builder.append(text, last, matcher.start());
            String label = matcher.group(1);
            String url = matcher.group(2);
            int spanStart = builder.length();
            builder.append(label);
            int spanEnd = builder.length();
            builder.setSpan(new URLSpan(url), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            last = matcher.end();
        }
        builder.append(text.substring(last));
        return builder;
    }

    private static void applyInlinePattern(SpannableStringBuilder out,
            int start,
            int end,
            Pattern pattern,
            boolean isBold) {
        String content = out.subSequence(start, end).toString();
        Matcher matcher = pattern.matcher(content);
        int offset = 0;
        while (matcher.find()) {
            int tokenStart = start + matcher.start() - offset;
            int tokenEnd = start + matcher.end() - offset;
            String inner = matcher.group(1);
            out.replace(tokenStart, tokenEnd, inner);
            int innerEnd = tokenStart + inner.length();
            if (isBold) {
                out.setSpan(
                        new StyleSpan(Typeface.BOLD),
                        tokenStart,
                        innerEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                out.setSpan(
                        new TypefaceSpan("monospace"),
                        tokenStart,
                        innerEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            offset += (matcher.group(0).length() - inner.length());
        }
    }

    private static void appendLine(SpannableStringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void appendNewLineIfNeeded(SpannableStringBuilder out, int index, int totalLines) {
        if (index < totalLines - 1) {
            out.append('\n');
        }
    }
}
