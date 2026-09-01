package com.dpis.module.home

/**
 * User-owned presentation choices for the phone and tablet Home workspace.
 *
 * Runtime configuration and backup data deliberately do not carry this state. Future bottom
 * navigation ordering, visibility, and default-home choices can extend this model without
 * changing item IDs.
 */
class HomeWorkspaceLayout(hiddenItems: Set<Item> = emptySet()) {
    enum class Item {
        CONFIGURED_APPS,
        IMPORTED_FONTS,
        TEMPLATES,
        BASIC_INFO,
        MODE_HELP,
        FEEDBACK,
        DONATE,
    }

    @JvmField
    val hiddenItems = hiddenItems.toSet()

    fun isVisible(item: Item): Boolean = item !in hiddenItems

    fun withVisibility(item: Item, visible: Boolean): HomeWorkspaceLayout =
        HomeWorkspaceLayout(
            if (visible) hiddenItems - item else hiddenItems + item,
        )

    companion object {
        @JvmStatic
        fun defaults(): HomeWorkspaceLayout = HomeWorkspaceLayout()
    }
}
