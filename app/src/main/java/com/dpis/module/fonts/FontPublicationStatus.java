package com.dpis.module.fonts;

/**
 * Describes whether a catalogued font can be read outside the DPIS app sandbox.
 * The private catalog remains valid when root publication is unavailable.
 */
public enum FontPublicationStatus {
    PRIVATE,
    PUBLISHED,
    PUBLISH_FAILED;

    static FontPublicationStatus fromStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return PRIVATE;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return PRIVATE;
        }
    }
}
