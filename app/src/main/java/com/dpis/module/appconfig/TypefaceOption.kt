package com.dpis.module.appconfig

/** A selectable typeface row, including the explicit disabled sentinel. */
internal open class TypefaceOptionModel(
    @JvmField val id: String?,
    @JvmField val label: String,
) {
    fun isDisabled(): Boolean = DISABLED_ID == id

    fun matches(selectedTypefaceId: String?): Boolean = if (id == null) {
        selectedTypefaceId.isNullOrBlank()
    } else {
        id == selectedTypefaceId
    }

    companion object {
        const val DISABLED_ID = "__disabled__"
    }
}
