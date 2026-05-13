package com.dpis.module;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseNotesMarkdownLite {
    private static final int CODE_BLOCK_BG_COLOR = 0x122D61D8;
    private static final int INLINE_CODE_BG_COLOR = 0x142D61D8;
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((https?://[^)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern VERSION_HEADING_PATTERN = Pattern.compile(
            "^##\\s*\\[.+?\\]\\(https?://[^)]+\\)\\s*\\(\\d{4}-\\d{2}-\\d{2}\\)\\s*$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)([*-])\\s+(.+)$");

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
        boolean enableCodeFence = hasBalancedCodeFences(filtered);
        SpannableStringBuilder out = new SpannableStringBuilder();
        String[] lines = filtered.replace("\r\n", "\n").split("\n", -1);
        boolean inCodeBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i];
            String line = rawLine == null ? "" : rawLine.trim();
            if (enableCodeFence && line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (inCodeBlock) {
                appendCodeBlockLine(out, rawLine == null ? "" : rawLine);
                appendNewLineIfNeeded(out, i, lines.length);
                continue;
            }
            if (line.equals("---")) {
                appendLine(out, "────────");
                continue;
            }
            if (line.startsWith("# ")) {
                appendHeading(out, line.substring(2), 1.2f);
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
            if (line.startsWith("#### ")) {
                appendHeading(out, line.substring(5), 1.02f);
                continue;
            }
            Matcher listMatcher = UNORDERED_LIST_PATTERN.matcher(rawLine == null ? "" : rawLine);
            if (listMatcher.matches()) {
                appendUnorderedListItem(out, listMatcher.group(1), listMatcher.group(3));
                appendNewLineIfNeeded(out, i, lines.length);
                continue;
            }
            appendStyledText(out, rawLine);
            appendNewLineIfNeeded(out, i, lines.length);
        }
        return out;
    }

    private static boolean hasBalancedCodeFences(String content) {
        if (content == null || content.isEmpty()) {
            return true;
        }
        String[] lines = content.replace("\r\n", "\n").split("\n", -1);
        int fenceCount = 0;
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.startsWith("```")) {
                fenceCount++;
            }
        }
        return (fenceCount % 2) == 0;
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

        String chinesePart = sectionSplit[0].trim();
        String englishPart = sectionSplit[1].trim();
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

    private static void appendUnorderedListItem(SpannableStringBuilder out, String indent, String content) {
        int level = estimateListLevel(indent);
        String marker = level == 0 ? "• " : "◦ ";
        int padCount = Math.min(6, Math.max(0, level)) * 2;
        if (padCount > 0) {
            out.append(" ".repeat(padCount));
        }
        appendStyledText(out, marker + content);
    }

    private static void appendCodeBlockLine(SpannableStringBuilder out, String line) {
        int start = out.length();
        out.append("  ").append(line);
        int end = out.length();
        out.setSpan(
                new TypefaceSpan("monospace"),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(
                new BackgroundColorSpan(CODE_BLOCK_BG_COLOR),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static int estimateListLevel(String indent) {
        if (indent == null || indent.isEmpty()) {
            return 0;
        }
        int spaces = 0;
        for (int i = 0; i < indent.length(); i++) {
            if (indent.charAt(i) == ' ') {
                spaces++;
            } else if (indent.charAt(i) == '\t') {
                spaces += 2;
            }
        }
        return spaces / 2;
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
                out.setSpan(
                        new BackgroundColorSpan(INLINE_CODE_BG_COLOR),
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
