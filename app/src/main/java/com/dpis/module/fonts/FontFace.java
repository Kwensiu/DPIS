package com.dpis.module.fonts;

import java.util.Objects;

/**
 * A selectable face within one physical font collection. Old persisted IDs remain supported.
 */
public final class FontFace {
    public final String collectionId;
    public final int ttcIndex;
    public final boolean collectionFace;

    public FontFace(String collectionId, int ttcIndex) {
        this(collectionId, ttcIndex, false);
    }

    private FontFace(String collectionId, int ttcIndex, boolean collectionFace) {
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId");
        this.ttcIndex = Math.max(0, ttcIndex);
        this.collectionFace = collectionFace;
    }

    public static FontFace fromLegacyId(String typefaceId) {
        if (typefaceId == null || typefaceId.isBlank()) {
            return null;
        }
        int marker = typefaceId.lastIndexOf("_ttc_");
        if (marker <= 0 || marker + 5 >= typefaceId.length()) {
            return new FontFace(typefaceId, 0);
        }
        try {
            int index = Integer.parseInt(typefaceId.substring(marker + 5));
            return index >= 0
                    ? new FontFace(typefaceId.substring(0, marker), index, true)
                    : new FontFace(typefaceId, 0);
        } catch (NumberFormatException ignored) {
            return new FontFace(typefaceId, 0);
        }
    }

    public String toLegacyId() {
        return collectionFace ? collectionId + "_ttc_" + ttcIndex : collectionId;
    }
}
