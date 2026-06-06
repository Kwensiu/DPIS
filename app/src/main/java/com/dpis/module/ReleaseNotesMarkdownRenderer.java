package com.dpis.module;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;

import java.util.Locale;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.core.MarkwonTheme;

final class ReleaseNotesMarkdownRenderer {
    interface Renderer {
        CharSequence render(Context context, String markdown);
    }

    private static Renderer renderer = ReleaseNotesMarkdownRenderer::renderWithMarkwon;

    private ReleaseNotesMarkdownRenderer() {
    }

    static CharSequence render(Context context, String markdown, Locale locale) {
        String filtered = ReleaseNotesMarkdownLite.filterBodyForLocale(safe(markdown), locale);
        if (filtered.trim().isEmpty()) {
            return "";
        }
        try {
            return renderer.render(context, filtered);
        } catch (Throwable ignored) {
            // Release notes come from GitHub text controlled outside the app. Keep the UI
            // alive even if a Markdown extension or malformed input trips the renderer.
            return fallbackPlainText(filtered);
        }
    }

    static void setRendererForTesting(Renderer testRenderer) {
        renderer = testRenderer != null
                ? testRenderer
                : ReleaseNotesMarkdownRenderer::renderWithMarkwon;
    }

    private static CharSequence renderWithMarkwon(Context context, String markdown) {
        if (context == null) {
            return fallbackPlainText(markdown);
        }
        return Markwon.builder(context)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(MarkwonTheme.Builder builder) {
                        int bulletWidth = Math.max(3, Math.round(context.getResources()
                                .getDisplayMetrics().density * 3f));
                        builder.bulletWidth(bulletWidth);
                    }
                })
                .build()
                .toMarkdown(markdown);
    }

    private static CharSequence fallbackPlainText(String markdown) {
        StringBuilder plain = new StringBuilder();
        String[] lines = safe(markdown).replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = stripMarkdownLine(lines[i]);
            plain.append(toPlainLinkLabels(line));
            if (i < lines.length - 1) {
                plain.append('\n');
            }
        }
        return applyPlainLinkSpans(plain.toString(), markdown);
    }

    private static String stripMarkdownLine(String line) {
        if (line == null) {
            return "";
        }
        String stripped = line.trim();
        if (stripped.startsWith("```")) {
            return "";
        }
        stripped = stripped.replaceFirst("^#{1,6}\\s+", "");
        stripped = stripped.replaceFirst("^[-*+]\\s+", "• ");
        stripped = stripped.replace("**", "");
        stripped = stripped.replace("__", "");
        stripped = stripped.replace("`", "");
        return stripped;
    }

    private static String toPlainLinkLabels(String line) {
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            int labelStart = line.indexOf('[', index);
            if (labelStart < 0) {
                out.append(line, index, line.length());
                return out.toString();
            }
            int labelEnd = line.indexOf("](", labelStart);
            int urlEnd = labelEnd >= 0 ? line.indexOf(')', labelEnd + 2) : -1;
            if (labelEnd < 0 || urlEnd < 0) {
                out.append(line, index, line.length());
                return out.toString();
            }
            String url = line.substring(labelEnd + 2, urlEnd);
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                out.append(line, index, urlEnd + 1);
                index = urlEnd + 1;
                continue;
            }
            out.append(line, index, labelStart);
            out.append(line, labelStart + 1, labelEnd);
            index = urlEnd + 1;
        }
        return out.toString();
    }

    private static CharSequence applyPlainLinkSpans(String plainText, String markdown) {
        try {
            SpannableStringBuilder out = new SpannableStringBuilder(plainText);
            String markdownText = safe(markdown);
            int searchStart = 0;
            int plainSearchStart = 0;
            while (searchStart < markdownText.length()) {
                int labelStart = markdownText.indexOf('[', searchStart);
                if (labelStart < 0) {
                    break;
                }
                int labelEnd = markdownText.indexOf("](", labelStart);
                int urlEnd = labelEnd >= 0 ? markdownText.indexOf(')', labelEnd + 2) : -1;
                if (labelEnd < 0 || urlEnd < 0) {
                    break;
                }
                String url = markdownText.substring(labelEnd + 2, urlEnd);
                String label = markdownText.substring(labelStart + 1, labelEnd);
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    int spanStart = plainText.indexOf(label, plainSearchStart);
                    if (spanStart >= 0) {
                        int spanEnd = spanStart + label.length();
                        out.setSpan(new URLSpan(url), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        plainSearchStart = spanEnd;
                    }
                }
                searchStart = urlEnd + 1;
            }
            return out;
        } catch (RuntimeException ignored) {
            return plainText;
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
