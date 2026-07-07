package com.dpis.module.fonts;

import java.io.File;

public final class PublishedFontFileResolver {
    private static final File PUBLIC_FONT_DIRECTORY = new File("/data/local/tmp");

    private PublishedFontFileResolver() {
    }

    public static File resolve(String typefaceId) {
        return resolveInDirectory(PUBLIC_FONT_DIRECTORY, typefaceId);
    }

    public static File resolveInDirectory(File directory, String typefaceId) {
        if (directory == null || typefaceId == null || typefaceId.isBlank()) {
            return null;
        }
        File ttf = new File(directory, "dpis_" + typefaceId + ".ttf");
        if (ttf.isFile()) {
            return ttf;
        }
        File otf = new File(directory, "dpis_" + typefaceId + ".otf");
        if (otf.isFile()) {
            return otf;
        }
        File ttc = new File(directory, "dpis_" + typefaceId + ".ttc");
        if (ttc.isFile()) {
            return ttc;
        }
        String collectionId = stripTtcFaceSuffix(typefaceId);
        if (!collectionId.equals(typefaceId)) {
            File sharedTtc = new File(directory, "dpis_" + collectionId + ".ttc");
            if (sharedTtc.isFile()) {
                return sharedTtc;
            }
        }
        return null;
    }

    private static String stripTtcFaceSuffix(String typefaceId) {
        int marker = typefaceId.lastIndexOf("_ttc_");
        if (marker <= 0 || marker + 5 >= typefaceId.length()) {
            return typefaceId;
        }
        for (int index = marker + 5; index < typefaceId.length(); index++) {
            if (!Character.isDigit(typefaceId.charAt(index))) {
                return typefaceId;
            }
        }
        return typefaceId.substring(0, marker);
    }
}
