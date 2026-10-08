package com.dpis.module.fonts

class SystemFontEntry(
    private val id: String,
    private val displayName: String
) {
    fun id(): String = id

    fun displayName(): String = displayName

    override fun equals(other: Any?): Boolean =
        this === other || (other is SystemFontEntry && id == other.id && displayName == other.displayName)

    override fun hashCode(): Int = 31 * id.hashCode() + displayName.hashCode()
}
