package com.dpis.module.hyperos

import java.util.Locale
import java.util.StringJoiner

/**
 * Argument layout and rewrite rules for HyperOS `startRustProcess`.
 *
 * Slot indices and environment encoding are the compatibility contract. Native
 * implementation is selected by `DPIS_NATIVE_ROUTE`, not by package ifs here.
 */
object HyperOsRustProcessArgPolicy {
    const val PACKAGE_NAME_INDEX = 1
    const val BINARY_PATH_INDEX = 20
    const val ENVIRONMENTS_INDEX = 21

    class StartArgs(
        @JvmField val packageName: String,
        @JvmField val existingEnvironments: String,
        @JvmField val binaryPath: String,
    )

    @JvmStatic
    fun parse(args: List<*>?): StartArgs? {
        if (args == null || args.size <= ENVIRONMENTS_INDEX) return null
        val packageValue = args[PACKAGE_NAME_INDEX]
        if (packageValue !is String) return null
        val existingValue = args[ENVIRONMENTS_INDEX]
        val existing = if (existingValue is String) existingValue else ""
        val binaryValue = args[BINARY_PATH_INDEX]
        val binaryPath = if (binaryValue is String) binaryValue else ""
        return StartArgs(packageValue, existing, binaryPath)
    }

    @JvmStatic
    fun shouldRewrite(
        fontScalePercent: Int?,
        hyperOsFlutterFontHookEnabled: Boolean,
    ): Boolean = hyperOsFlutterFontHookEnabled &&
        fontScalePercent != null &&
        fontScalePercent > 0

    @JvmStatic
    fun appendEnvironment(
        existing: String?,
        packageName: String,
        targetFontScalePercent: Int,
        binaryPath: String?,
    ): String {
        val builder = StringBuilder()
        if (existing != null && existing.trim().isNotEmpty()) {
            builder.append(existing.trim())
            if (builder[builder.length - 1] != ',') {
                builder.append(',')
            }
        }
        appendPair(builder, "DPIS_PACKAGE", packageName)
        appendPair(
            builder,
            "DPIS_FONT_SCALE_PERCENT",
            String.format(Locale.US, "%d", targetFontScalePercent),
        )
        appendPair(builder, "DPIS_RUST_BINARY", binaryPath ?: "")
        val nativeRoute = HyperOsNativeRoutePolicy.routeForPackage(packageName)
        if (nativeRoute != null) {
            appendPair(builder, HyperOsNativeRoutePolicy.ENV_KEY, nativeRoute)
        }
        builder.append(" --cold-boot-speed")
        return builder.toString()
    }

    @JvmStatic
    fun withProxyAndEnvironment(
        args: List<*>,
        proxyLibraryPath: String,
        environments: String,
    ): Array<Any?> {
        val updated = Array(args.size) { args[it] }
        updated[BINARY_PATH_INDEX] = proxyLibraryPath
        updated[ENVIRONMENTS_INDEX] = environments
        return updated
    }

    @JvmStatic
    fun buildArgumentProbeSummary(args: List<*>?): String? {
        if (args == null || args.isEmpty()) return null
        var hasTargetPackage = false
        val strings = StringJoiner(", ")
        for (index in args.indices) {
            val value = args[index]
            if (value !is String || value.isEmpty()) continue
            if (HyperOsNativeRoutePolicy.isKnownPackageFragment(value)) {
                hasTargetPackage = true
            }
            if (isInterestingArgumentString(value)) {
                strings.add(index.toString() + "=" + shorten(value))
            }
        }
        if (!hasTargetPackage) return null
        return "DPIS_FONT HyperOS Rust process args probe: size=" + args.size +
            ", strings={" + strings + "}"
    }

    private fun isInterestingArgumentString(value: String): Boolean =
        HyperOsNativeRoutePolicy.isKnownPackageFragment(value) ||
            value.contains(".so") ||
            value.contains("DPIS_") ||
            value.contains("--envs=") ||
            value.contains("hyperos")

    private fun shorten(value: String): String {
        val sanitized = sanitize(value)
        if (sanitized.length <= 240) return sanitized
        return sanitized.substring(0, 237) + "..."
    }

    private fun appendPair(builder: StringBuilder, key: String, value: String) {
        if (builder.isNotEmpty()) {
            builder.append(" --envs=")
        }
        builder.append(key).append('=').append(sanitize(value))
    }

    private fun sanitize(value: String?): String {
        if (value == null) return ""
        return value.replace(',', '_')
            .replace('\n', '_')
            .replace('\r', '_')
            .replace(' ', '_')
    }
}
