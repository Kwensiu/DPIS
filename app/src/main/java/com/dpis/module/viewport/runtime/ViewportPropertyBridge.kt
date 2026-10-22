package com.dpis.module.viewport

import android.os.Process
import java.lang.reflect.Method
import java.util.Locale

object ViewportPropertyBridge {
    private const val PROPERTY_PREFIX = "debug.dpis.vp."
    private const val TARGET_TYPE_PROPERTY_PREFIX = "debug.dpis.vptype."
    private const val SCALE_PROPERTY_PREFIX = "debug.dpis.vpscale."

    // legacy needs the requested value even for field_rewrite, while vp.* must
    // stay 0 unless system emulation is active.
    private const val COMPAT_CONFIG_PROPERTY_PREFIX = "debug.dpis.vpcfg."
    private const val COMPAT_MODE_PROPERTY_PREFIX = "debug.dpis.vpmode."
    private const val PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.vp."
    private const val PERSIST_TARGET_TYPE_PROPERTY_PREFIX = "persist.debug.dpis.vptype."
    private const val PERSIST_SCALE_PROPERTY_PREFIX = "persist.debug.dpis.vpscale."
    private const val PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX = "persist.debug.dpis.vpcfg."
    private const val PERSIST_COMPAT_MODE_PROPERTY_PREFIX = "persist.debug.dpis.vpmode."

    @JvmStatic
    fun propertyNameForPackage(packageName: String, userId: Int = currentUserId()) =
        PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun compatConfigPropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = COMPAT_CONFIG_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun targetTypePropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = TARGET_TYPE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun scalePropertyNameForPackage(packageName: String, userId: Int = currentUserId()) =
        SCALE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun compatModePropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = COMPAT_MODE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun persistentPropertyNameForPackage(packageName: String, userId: Int = currentUserId()) =
        PERSIST_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun persistentCompatConfigPropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun persistentTargetTypePropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = PERSIST_TARGET_TYPE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun persistentScalePropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = PERSIST_SCALE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun persistentCompatModePropertyNameForPackage(
        packageName: String,
        userId: Int = currentUserId(),
    ) = PERSIST_COMPAT_MODE_PROPERTY_PREFIX + hash(packageName, userId)

    @JvmStatic
    fun readTargetWidthDp(packageName: String?, userId: Int = currentUserId()): Int? =
        packageName?.takeIf { it.isNotEmpty() }?.let {
            readOverrideValue(
                propertyNameForPackage(it, userId),
                persistentPropertyNameForPackage(it, userId),
            )
        }

    @JvmStatic
    fun readCompatConfigWidthDp(packageName: String?, userId: Int = currentUserId()): Int? =
        packageName?.takeIf { it.isNotEmpty() }?.let {
            readOverrideValue(
                compatConfigPropertyNameForPackage(it, userId),
                persistentCompatConfigPropertyNameForPackage(it, userId),
            )
        }

    @JvmStatic
    fun readTargetSpec(packageName: String?, userId: Int = currentUserId()): ViewportTargetSpec {
        if (packageName.isNullOrEmpty()) return ViewportTargetSpec.off()
        val type = readPropertyWithPersistentFallback(
            targetTypePropertyNameForPackage(packageName, userId),
            persistentTargetTypePropertyNameForPackage(packageName, userId),
        )
        val widthDp = readTargetWidthDp(packageName, userId)
        val compatConfigWidthDp = readCompatConfigWidthDp(packageName, userId)
        val scale = readOverrideValue(
            scalePropertyNameForPackage(packageName, userId),
            persistentScalePropertyNameForPackage(packageName, userId),
        )
        return ViewportPropertyProjection.decode(
            widthDp,
            type,
            scale,
            compatConfigWidthDp,
            null
        ).targetSpec
    }

    @JvmStatic
    fun readCompatMode(packageName: String?, userId: Int = currentUserId()): String =
        if (packageName.isNullOrEmpty()) ViewportApplyMode.OFF else ViewportApplyMode.normalize(
            readPropertyWithPersistentFallback(
                compatModePropertyNameForPackage(packageName, userId),
                persistentCompatModePropertyNameForPackage(packageName, userId),
            ),
        )

    @JvmStatic
    fun parseOverrideValueForTest(value: String?) = parseOverrideValue(value)

    private fun parseOverrideValue(value: String?): Int? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()?.takeIf { it >= 0 }

    private fun readOverrideValue(propertyName: String, persistentPropertyName: String) =
        parseOverrideValue(readPropertyWithPersistentFallback(propertyName, persistentPropertyName))

    private fun readPropertyWithPersistentFallback(
        propertyName: String,
        persistentPropertyName: String
    ) = readSystemProperty(propertyName).takeIf { it.trim().isNotEmpty() } ?: readSystemProperty(
        persistentPropertyName
    )

    private fun readSystemProperty(key: String): String = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod: Method =
            systemProperties.getDeclaredMethod("get", String::class.java, String::class.java)
        val value = getMethod.invoke(null, key, "")
        return value as? String ?: ""
    } catch (_: Throwable) {
        ""
    }

    @JvmStatic
    fun readTargetSpecForTest(
        packageName: String,
        userId: Int,
        reader: (String) -> String?,
    ): ViewportTargetSpec {
        val names = propertyNamesForPackage(packageName, userId)
        return ViewportPropertyProjection.decode(
            parseOverrideValue(reader(names.targetWidth)),
            reader(names.targetType),
            parseOverrideValue(reader(names.scale)),
            parseOverrideValue(reader(names.compatConfigWidth)),
            reader(names.compatMode),
        ).targetSpec
    }

    private data class PropertyNames(
        val targetWidth: String,
        val targetType: String,
        val scale: String,
        val compatConfigWidth: String,
        val compatMode: String,
    )

    private fun propertyNamesForPackage(packageName: String, userId: Int) = PropertyNames(
        propertyNameForPackage(packageName, userId),
        targetTypePropertyNameForPackage(packageName, userId),
        scalePropertyNameForPackage(packageName, userId),
        compatConfigPropertyNameForPackage(packageName, userId),
        compatModePropertyNameForPackage(packageName, userId),
    )

    private fun currentUserId(): Int = runCatching {
        Process.myUid() / PER_USER_RANGE
    }.getOrDefault(0)

    private fun hash(packageName: String, userId: Int) =
        "$userId." + String.format(Locale.US, "%08x", packageName.hashCode())

    private const val PER_USER_RANGE = 100000
}
