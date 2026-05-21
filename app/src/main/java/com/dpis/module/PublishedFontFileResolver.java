package com.dpis.module;

import java.io.File;

final class PublishedFontFileResolver {
    private static final File PUBLIC_FONT_DIRECTORY = new File("/data/local/tmp");

    private PublishedFontFileResolver() {
    }

    static File resolve(String typefaceId) {
        return resolveInDirectory(PUBLIC_FONT_DIRECTORY, typefaceId);
    }

    static File resolveInDirectory(File directory, String typefaceId) {
        if (directory == null || typefaceId == null || typefaceId.isBlank()) {
            return null;
        }
        File ttf = new File(directory, "dpis_" + typefaceId + ".ttf");
        if (ttf.isFile()) {
            return ttf;
        }
        File otf = new File(directory, "dpis_" + typefaceId + ".otf");
        return otf.isFile() ? otf : null;
    }
}
