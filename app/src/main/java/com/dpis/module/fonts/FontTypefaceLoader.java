package com.dpis.module.fonts;

import android.graphics.Typeface;

import java.io.File;
import java.util.Locale;

public final class FontTypefaceLoader {
    private FontTypefaceLoader() {
    }

    public static Typeface load(File file, int ttcIndex) {
        if (file == null || !file.canRead()) {
            return null;
        }
        try {
            if (isTtc(file) || ttcIndex > 0) {
                return new Typeface.Builder(file)
                        .setTtcIndex(Math.max(0, ttcIndex))
                        .build();
            }
            return Typeface.createFromFile(file);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTtc(File file) {
        String name = file.getName();
        return name != null && name.toLowerCase(Locale.US).endsWith(".ttc");
    }
}
