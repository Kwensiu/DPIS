package com.dpis.module.runtime.font

import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import java.util.Locale

object HyperOsFlutterFontBridge {
    private const val PROPERTY_PREFIX = "debug.dpis.font."
    private const val FORCE_PROPERTY_PREFIX = "debug.dpis.forcefont."
    private const val COMPAT_FONT_PROPERTY_PREFIX = "debug.dpis.compatfont."

    // compatfont.* is non-zero only for system emulation; fontmode.* lets
    // legacy interpret forcefont.* as field_rewrite when needed.
    private const val COMPAT_FONT_MODE_PROPERTY_PREFIX = "debug.dpis.fontmode."
    private const val TYPEFACE_PROPERTY_PREFIX = "debug.dpis.typeface."
    private const val RUST_BINARY_PROPERTY_PREFIX = "debug.dpis.rustbin."
    private const val PERSIST_FORCE_PROPERTY_PREFIX = "persist.debug.dpis.forcefont."
    private const val PERSIST_COMPAT_FONT_PROPERTY_PREFIX = "persist.debug.dpis.compatfont."
    private const val PERSIST_COMPAT_FONT_MODE_PROPERTY_PREFIX = "persist.debug.dpis.fontmode."
    private const val PERSIST_TYPEFACE_PROPERTY_PREFIX = "persist.debug.dpis.typeface."

    @JvmStatic
    fun propertyNameForPackage(packageName: String): String =
        PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun forcePropertyNameForPackage(packageName: String): String =
        FORCE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun compatFontPropertyNameForPackage(packageName: String): String =
        COMPAT_FONT_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun compatFontModePropertyNameForPackage(packageName: String): String =
        COMPAT_FONT_MODE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun typefacePropertyNameForPackage(packageName: String): String =
        TYPEFACE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun persistentForcePropertyNameForPackage(packageName: String): String =
        PERSIST_FORCE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun persistentCompatFontPropertyNameForPackage(packageName: String): String =
        PERSIST_COMPAT_FONT_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun persistentCompatFontModePropertyNameForPackage(packageName: String): String =
        PERSIST_COMPAT_FONT_MODE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun persistentTypefacePropertyNameForPackage(packageName: String): String =
        PERSIST_TYPEFACE_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun readForceFontScalePercent(packageName: String): Int? =
        readPositiveIntProperty(
            forcePropertyNameForPackage(packageName),
            persistentForcePropertyNameForPackage(packageName),
        )

    @JvmStatic
    fun readCompatFontScalePercent(packageName: String?): Int? {
        if (packageName.isNullOrEmpty()) return null
        return readPositiveIntProperty(
            compatFontPropertyNameForPackage(packageName),
            persistentCompatFontPropertyNameForPackage(packageName),
        )
    }

    @JvmStatic
    fun readCompatFontMode(packageName: String?): String {
        if (packageName.isNullOrEmpty()) return FontApplyMode.OFF
        return FontApplyMode.normalize(
            readPropertyWithPersistentFallback(
                compatFontModePropertyNameForPackage(packageName),
                persistentCompatFontModePropertyNameForPackage(packageName),
            ),
        )
    }

    @JvmStatic
    fun readTypefaceId(packageName: String?): String? {
        if (packageName.isNullOrEmpty()) return null
        val value = readPropertyWithPersistentFallback(
            typefacePropertyNameForPackage(packageName),
            persistentTypefacePropertyNameForPackage(packageName),
        )
        if (value.isNullOrBlank() || value.trim() == "0") return null
        return value.trim()
    }

    private fun propertySuffix(packageName: String): String =
        String.format(Locale.US, "%08x", packageName.hashCode())

    private fun readPositiveIntProperty(key: String, persistentKey: String): Int? =
        readPropertyWithPersistentFallback(key, persistentKey)?.trim()?.toIntOrNull()
            ?.takeIf { it > 0 }

    @JvmStatic
    fun rustBinaryPropertyNameForPackage(packageName: String): String =
        RUST_BINARY_PROPERTY_PREFIX + propertySuffix(packageName)

    @JvmStatic
    fun publishTarget(packageName: String?, config: PerAppDisplayConfig?) {
        if (packageName.isNullOrEmpty() || config == null ||
            !config.hyperOsFlutterFontHookEnabled ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0
        ) {
            if (shouldClearOnPublishTargetSkip(packageName, config)) clearTarget(packageName)
            return
        }
        setSystemProperty(
            propertyNameForPackage(packageName),
            config.targetFontScalePercent.toString()
        )
    }

    @JvmStatic
    fun publishRustProxyTarget(packageName: String?, config: PerAppDisplayConfig?) {
        if (packageName.isNullOrEmpty() || config == null ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0
        ) {
            clearTarget(packageName)
            return
        }
        setSystemProperty(
            propertyNameForPackage(packageName),
            config.targetFontScalePercent.toString()
        )
    }

    @JvmStatic
    fun clearTarget(packageName: String?) {
        if (packageName.isNullOrEmpty()) return
        val assignments = clearTargetAssignments(packageName)
        var index = 0
        while (index < assignments.size) {
            setSystemProperty(assignments[index], assignments[index + 1])
            index += 2
        }
    }

    @JvmStatic
    fun clearNativeTarget(packageName: String?) {
        if (packageName.isNullOrEmpty()) return
        setSystemProperty(propertyNameForPackage(packageName), "0")
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName), "0")
    }

    @JvmStatic
    fun shouldClearOnPublishTargetSkipForTest(
        packageName: String?,
        config: PerAppDisplayConfig?,
    ): Boolean = shouldClearOnPublishTargetSkip(packageName, config)

    @JvmStatic
    fun clearTargetAssignmentsForTest(packageName: String): Array<String> =
        clearTargetAssignments(packageName)

    private fun clearTargetAssignments(packageName: String): Array<String> = arrayOf(
        propertyNameForPackage(packageName), "0",
        forcePropertyNameForPackage(packageName), "0",
        compatFontPropertyNameForPackage(packageName), "0",
        compatFontModePropertyNameForPackage(packageName), FontApplyMode.OFF,
        persistentForcePropertyNameForPackage(packageName), "0",
        persistentCompatFontPropertyNameForPackage(packageName), "0",
        persistentCompatFontModePropertyNameForPackage(packageName), FontApplyMode.OFF,
        typefacePropertyNameForPackage(packageName), "0",
        persistentTypefacePropertyNameForPackage(packageName), "0",
        rustBinaryPropertyNameForPackage(packageName), "0",
    )

    private fun shouldClearOnPublishTargetSkip(
        packageName: String?,
        config: PerAppDisplayConfig?,
    ): Boolean = packageName.isNullOrEmpty() || config == null ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0

    @JvmStatic
    fun publishRustBinaryPath(packageName: String?, binaryPath: String?) {
        if (packageName.isNullOrEmpty() || binaryPath.isNullOrEmpty()) return
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName), binaryPath)
    }

    private fun setSystemProperty(key: String, value: String) {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val set =
                systemProperties.getDeclaredMethod("set", String::class.java, String::class.java)
            set.invoke(null, key, value)
        } catch (throwable: Throwable) {
            DpisLog.e("DPIS_FONT HyperOS Flutter property publish failed: key=$key", throwable)
        }
    }

    private fun readSystemProperty(key: String): String = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getDeclaredMethod(
            "get",
            String::class.java,
            String::class.java,
        )
        getMethod.invoke(null, key, "") as? String ?: ""
    } catch (_: Throwable) {
        ""
    }

    private fun readPropertyWithPersistentFallback(key: String, persistentKey: String): String {
        val value = readSystemProperty(key)
        return if (value.isNotBlank()) value else readSystemProperty(persistentKey)
    }
}
