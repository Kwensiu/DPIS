package com.dpis.module.templates

import java.util.Collections

/**
 * Coordinates applying a saved quick-template value to selected packages.
 * 
 * 
 * The coordinator is intentionally generic so template apply semantics can
 * live in the template package without knowing root-owned package config,
 * runtime publishing, or app-list models.
 */
class QuickTemplateApplyCoordinator<T>(
    private val configWriter: ConfigWriter<T?>?,
    private val runtimePublisher: RuntimePublisher<T?>?
) {
    interface Template<T> {
        fun selectedPackages(): MutableSet<String?>?

        fun configValue(): T?
    }

    interface ConfigWriter<T> {
        fun hasRealPackageConfig(packageName: String?): Boolean

        fun writePackageTemplateConfigValue(packageName: String?, value: T?): Boolean
    }

    interface RuntimePublisher<T> {
        fun publish(packageName: String?, value: T?)
    }

    fun interface TargetPackageFilter {
        fun isAllowed(packageName: String?): Boolean
    }

    @JvmOverloads
    fun plan(template: Template<T?>?, targetPackageFilter: TargetPackageFilter? = null): Plan {
        var targets: LinkedHashSet<String?> = sanitizePackages(
            if (template != null) template.selectedPackages() else null
        )
        targets = filterPackages(targets, targetPackageFilter)
        var overwriteCount = 0
        if (configWriter != null) {
            for (packageName in targets) {
                if (configWriter.hasRealPackageConfig(packageName)) {
                    overwriteCount++
                }
            }
        }
        return Plan(targets.size, overwriteCount)
    }

    @JvmOverloads
    fun apply(template: Template<T?>?, targetPackageFilter: TargetPackageFilter? = null): Result {
        if (configWriter == null || template == null) {
            return Result.Companion.failure(mutableListOf<String?>(), mutableListOf<String?>())
        }
        var targets: LinkedHashSet<String?> = sanitizePackages(template.selectedPackages())
        targets = filterPackages(targets, targetPackageFilter)
        if (targets.isEmpty()) {
            return Result.Companion.noSelection()
        }
        val successfulPackages = ArrayList<String?>()
        val failedPackages = ArrayList<String?>()
        val configValue = template.configValue()
        for (packageName in targets) {
            val saved = configWriter.writePackageTemplateConfigValue(packageName, configValue)
            if (saved) {
                successfulPackages.add(packageName)
                if (runtimePublisher != null) {
                    runtimePublisher.publish(packageName, configValue)
                }
            } else {
                failedPackages.add(packageName)
            }
        }
        return Result(false, successfulPackages, failedPackages)
    }

    class Plan(@JvmField val targetCount: Int, @JvmField val overwriteCount: Int)

    class Result(
        @JvmField val emptySelection: Boolean,
        successfulPackages: MutableList<String?>?,
        failedPackages: MutableList<String?>?
    ) {
        @JvmField
        val successfulPackages: MutableList<String?>
        @JvmField
        val failedPackages: MutableList<String?>

        init {
            this.successfulPackages = Collections.unmodifiableList<String?>(
                ArrayList<String?>(
                    if (successfulPackages != null) successfulPackages else mutableListOf<String?>()
                )
            )
            this.failedPackages = Collections.unmodifiableList<String?>(
                ArrayList<String?>(
                    if (failedPackages != null) failedPackages else mutableListOf<String?>()
                )
            )
        }

        fun successCount(): Int {
            return successfulPackages.size
        }

        fun failureCount(): Int {
            return failedPackages.size
        }

        companion object {
            fun noSelection(): Result {
                return Result(true, mutableListOf<String?>(), mutableListOf<String?>())
            }

            fun failure(
                successfulPackages: MutableList<String?>?,
                failedPackages: MutableList<String?>?
            ): Result {
                return Result(false, successfulPackages, failedPackages)
            }
        }
    }

    companion object {
        private fun sanitizePackages(packageNames: MutableSet<String?>?): LinkedHashSet<String?> {
            val sanitized = LinkedHashSet<String?>()
            if (packageNames == null) {
                return sanitized
            }
            for (packageName in packageNames) {
                if (packageName == null) {
                    continue
                }
                val trimmed = packageName.trim { it <= ' ' }
                if (!trimmed.isEmpty()) {
                    sanitized.add(trimmed)
                }
            }
            return sanitized
        }

        private fun filterPackages(
            packageNames: LinkedHashSet<String?>?,
            targetPackageFilter: TargetPackageFilter?
        ): LinkedHashSet<String?> {
            if (targetPackageFilter == null || packageNames == null || packageNames.isEmpty()) {
                return if (packageNames != null) packageNames else LinkedHashSet<String?>()
            }
            val filtered = LinkedHashSet<String?>()
            for (packageName in packageNames) {
                if (targetPackageFilter.isAllowed(packageName)) {
                    filtered.add(packageName)
                }
            }
            return filtered
        }
    }
}
