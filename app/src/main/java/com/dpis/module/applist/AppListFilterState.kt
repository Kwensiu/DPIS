package com.dpis.module.applist

/** Immutable app-list filter selection. Empty sets mean that the corresponding group is unrestricted. */
class AppListFilterState(
    selectedAppTypes: Set<AppType>,
    selectedConfigurationFilters: Set<ConfigurationFilter>,
    val sortOrder: SortOrder,
    private val reverse: Boolean,
) {
    enum class AppType { USER, SYSTEM, ALL }

    enum class ConfigurationFilter { ALL, INJECTED, DISABLED, VIEWPORT, FONT, TYPEFACE, HOOK }

    enum class SortOrder { NAME, UPDATED, INSTALLED }

    val selectedAppTypes: Set<AppType> = selectedAppTypes
        .filter { it != AppType.ALL }
        .toSet()
    val selectedConfigurationFilters: Set<ConfigurationFilter> = selectedConfigurationFilters
        .filter { it != ConfigurationFilter.ALL }
        .toSet()

    fun allAppsSelected(): Boolean = selectedAppTypes.isEmpty()

    fun userAppsSelected(): Boolean = AppType.USER in selectedAppTypes

    fun systemAppsSelected(): Boolean = AppType.SYSTEM in selectedAppTypes

    fun showSystemApps(): Boolean = systemAppsSelected()

    fun injectedOnly(): Boolean = ConfigurationFilter.INJECTED in selectedConfigurationFilters

    fun disabledOnly(): Boolean = ConfigurationFilter.DISABLED in selectedConfigurationFilters

    fun widthConfiguredOnly(): Boolean =
        ConfigurationFilter.VIEWPORT in selectedConfigurationFilters

    fun fontConfiguredOnly(): Boolean = ConfigurationFilter.FONT in selectedConfigurationFilters

    fun typefaceConfiguredOnly(): Boolean =
        ConfigurationFilter.TYPEFACE in selectedConfigurationFilters

    fun hookConfiguredOnly(): Boolean = ConfigurationFilter.HOOK in selectedConfigurationFilters

    fun allConfigurationSelected(): Boolean = selectedConfigurationFilters.isEmpty()

    fun isDefaultSelection(): Boolean =
        allAppsSelected() && allConfigurationSelected() && sortOrder == SortOrder.NAME && !reverse

    /** Legacy single-value view used by saved-instance-state compatibility. */
    fun appType(): AppType = when {
        allAppsSelected() -> AppType.ALL
        systemAppsSelected() && !userAppsSelected() -> AppType.SYSTEM
        else -> AppType.USER
    }

    fun sortOrder(): SortOrder = sortOrder

    fun reverseOrder(): Boolean = reverse

    fun toggleAppType(type: AppType): AppListFilterState = when (type) {
        AppType.ALL -> selectAllAppTypes()
        AppType.USER, AppType.SYSTEM -> {
            val next = if (type in selectedAppTypes) {
                selectedAppTypes - type
            } else {
                selectedAppTypes + type
            }
            copy(selectedAppTypes = next)
        }
    }

    fun selectAllAppTypes(): AppListFilterState = copy(selectedAppTypes = emptySet())

    fun selectAppTypes(types: Set<AppType>): AppListFilterState = copy(selectedAppTypes = types)

    fun toggleConfiguration(filter: ConfigurationFilter): AppListFilterState {
        if (filter == ConfigurationFilter.ALL) {
            return clearConfigurationFilters()
        }
        val next = if (filter in selectedConfigurationFilters) {
            selectedConfigurationFilters - filter
        } else {
            selectedConfigurationFilters + filter
        }
        return copy(selectedConfigurationFilters = next)
    }

    fun clearConfigurationFilters(): AppListFilterState =
        copy(selectedConfigurationFilters = emptySet())

    fun withSortOrder(value: SortOrder): AppListFilterState = copy(sortOrder = value)

    fun withReverseOrder(value: Boolean): AppListFilterState = copy(reverse = value)

    private fun copy(
        selectedAppTypes: Set<AppType> = this.selectedAppTypes,
        selectedConfigurationFilters: Set<ConfigurationFilter> = this.selectedConfigurationFilters,
        sortOrder: SortOrder = this.sortOrder,
        reverse: Boolean = this.reverse,
    ): AppListFilterState = AppListFilterState(
        selectedAppTypes,
        selectedConfigurationFilters,
        sortOrder,
        reverse,
    )

    companion object {
        @JvmStatic
        fun defaultState(): AppListFilterState = AppListFilterState(
            emptySet(),
            emptySet(),
            SortOrder.NAME,
            false,
        )

        @JvmStatic
        fun noAdditionalConstraints(): AppListFilterState = defaultState()
    }
}
