package com.dpis.module.runtime.font

import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontPropertyKeys

object HyperOsFlutterFontBridge {
    @JvmStatic
    fun propertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "font")

    @JvmStatic
    fun forcePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "forcefont")

    @JvmStatic
    fun compatFontPropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "compatfont")

    @JvmStatic
    fun compatFontModePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "fontmode")

    @JvmStatic
    fun typefacePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "typeface")

    @JvmStatic
    fun persistentForcePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "forcefont", true)

    @JvmStatic
    fun persistentCompatFontPropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "compatfont", true)

    @JvmStatic
    fun persistentCompatFontModePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "fontmode", true)

    @JvmStatic
    fun persistentTypefacePropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "typeface", true)

    @JvmStatic
    fun readForceFontScalePercent(
        packageName: String,
        userId: Int,
        reader: (String) -> String = ::readSystemProperty
    ): Int? =
        readPositiveIntProperty(
            forcePropertyNameForPackage(packageName, userId),
            persistentForcePropertyNameForPackage(packageName, userId),
            reader,
        )

    @JvmStatic
    fun readCompatFontScalePercent(
        packageName: String?,
        userId: Int,
        reader: (String) -> String = ::readSystemProperty
    ): Int? {
        if (packageName.isNullOrEmpty()) return null
        return readPositiveIntProperty(
            compatFontPropertyNameForPackage(packageName, userId),
            persistentCompatFontPropertyNameForPackage(packageName, userId),
            reader,
        )
    }

    @JvmStatic
    fun readCompatFontMode(
        packageName: String?,
        userId: Int,
        reader: (String) -> String = ::readSystemProperty
    ): String {
        if (packageName.isNullOrEmpty()) return FontApplyMode.OFF
        return FontApplyMode.normalize(
            readPropertyWithPersistentFallback(
                compatFontModePropertyNameForPackage(packageName, userId),
                persistentCompatFontModePropertyNameForPackage(packageName, userId),
                reader,
            ),
        )
    }

    @JvmStatic
    fun readTypefaceId(
        packageName: String?,
        userId: Int,
        reader: (String) -> String = ::readSystemProperty
    ): String? {
        if (packageName.isNullOrEmpty()) return null
        val value = readPropertyWithPersistentFallback(
            typefacePropertyNameForPackage(packageName, userId),
            persistentTypefacePropertyNameForPackage(packageName, userId),
            reader,
        )
        if (value.isNullOrBlank() || value.trim() == "0") return null
        return value.trim()
    }

    private fun readPositiveIntProperty(
        key: String,
        persistentKey: String,
        reader: (String) -> String
    ): Int? =
        readPropertyWithPersistentFallback(key, persistentKey, reader).trim().toIntOrNull()
            ?.takeIf { it > 0 }

    @JvmStatic
    fun rustBinaryPropertyNameForPackage(packageName: String, userId: Int): String =
        FontPropertyKeys.name(packageName, userId, "rustbin")

    @JvmStatic
    fun publishTarget(packageName: String?, config: PerAppDisplayConfig?, userId: Int) {
        if (packageName.isNullOrEmpty() || config == null ||
            !config.hyperOsFlutterFontHookEnabled ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0
        ) {
            if (shouldClearOnPublishTargetSkip(packageName, config)) clearTarget(
                packageName,
                userId
            )
            return
        }
        setSystemProperty(
            propertyNameForPackage(packageName, userId),
            config.targetFontScalePercent.toString()
        )
    }

    @JvmStatic
    fun publishRustProxyTarget(packageName: String?, config: PerAppDisplayConfig?, userId: Int) {
        if (packageName.isNullOrEmpty() || config == null ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0
        ) {
            clearTarget(packageName, userId)
            return
        }
        setSystemProperty(
            propertyNameForPackage(packageName, userId),
            config.targetFontScalePercent.toString()
        )
    }

    @JvmStatic
    fun clearTarget(packageName: String?, userId: Int) {
        if (packageName.isNullOrEmpty()) return
        val assignments = clearTargetAssignments(packageName, userId)
        var index = 0
        while (index < assignments.size) {
            setSystemProperty(assignments[index], assignments[index + 1])
            index += 2
        }
    }

    @JvmStatic
    fun clearNativeTarget(packageName: String?, userId: Int) {
        if (packageName.isNullOrEmpty()) return
        setSystemProperty(propertyNameForPackage(packageName, userId), "0")
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName, userId), "0")
    }

    @JvmStatic
    fun shouldClearOnPublishTargetSkipForTest(
        packageName: String?,
        config: PerAppDisplayConfig?,
    ): Boolean = shouldClearOnPublishTargetSkip(packageName, config)

    @JvmStatic
    fun clearTargetAssignmentsForTest(packageName: String, userId: Int): Array<String> =
        clearTargetAssignments(packageName, userId)

    private fun clearTargetAssignments(packageName: String, userId: Int): Array<String> = arrayOf(
        propertyNameForPackage(packageName, userId), "0",
        forcePropertyNameForPackage(packageName, userId), "0",
        compatFontPropertyNameForPackage(packageName, userId), "0",
        compatFontModePropertyNameForPackage(packageName, userId), FontApplyMode.OFF,
        persistentForcePropertyNameForPackage(packageName, userId), "0",
        persistentCompatFontPropertyNameForPackage(packageName, userId), "0",
        persistentCompatFontModePropertyNameForPackage(packageName, userId), FontApplyMode.OFF,
        typefacePropertyNameForPackage(packageName, userId), "0",
        persistentTypefacePropertyNameForPackage(packageName, userId), "0",
        rustBinaryPropertyNameForPackage(packageName, userId), "0",
    )

    private fun shouldClearOnPublishTargetSkip(
        packageName: String?,
        config: PerAppDisplayConfig?,
    ): Boolean = packageName.isNullOrEmpty() || config == null ||
            config.targetFontScalePercent == null || config.targetFontScalePercent <= 0

    @JvmStatic
    fun publishRustBinaryPath(packageName: String?, binaryPath: String?, userId: Int) {
        if (packageName.isNullOrEmpty() || binaryPath.isNullOrEmpty()) return
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName, userId), binaryPath)
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

    private fun readPropertyWithPersistentFallback(
        key: String,
        persistentKey: String,
        reader: (String) -> String,
    ): String {
        val value = reader(key)
        return if (value.isNotBlank()) value else reader(persistentKey)
    }
}
