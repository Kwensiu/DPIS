package com.dpis.module.fonts

/** Describes whether a catalogued font is readable outside the app sandbox. */
enum class FontPublicationStatus {
    PRIVATE,
    PUBLISHED,
    PUBLISH_FAILED;

    companion object {
        @JvmStatic
        fun fromStoredValue(value: String?): FontPublicationStatus {
            if (value.isNullOrBlank()) return PRIVATE
            return try {
                valueOf(value)
            } catch (_: IllegalArgumentException) {
                PRIVATE
            }
        }
    }
}
