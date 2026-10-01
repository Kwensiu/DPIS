package com.dpis.module.viewport

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
    fun propertyNameForPackage(packageName: String) = PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun compatConfigPropertyNameForPackage(packageName: String) =
        COMPAT_CONFIG_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun targetTypePropertyNameForPackage(packageName: String) =
        TARGET_TYPE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun scalePropertyNameForPackage(packageName: String) = SCALE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun compatModePropertyNameForPackage(packageName: String) =
        COMPAT_MODE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun persistentPropertyNameForPackage(packageName: String) =
        PERSIST_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun persistentCompatConfigPropertyNameForPackage(packageName: String) =
        PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun persistentTargetTypePropertyNameForPackage(packageName: String) =
        PERSIST_TARGET_TYPE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun persistentScalePropertyNameForPackage(packageName: String) =
        PERSIST_SCALE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun persistentCompatModePropertyNameForPackage(packageName: String) =
        PERSIST_COMPAT_MODE_PROPERTY_PREFIX + hash(packageName)

    @JvmStatic
    fun readTargetWidthDp(packageName: String?): Int? = packageName?.takeIf { it.isNotEmpty() }
        ?.let {
            readOverrideValue(
                propertyNameForPackage(it),
                persistentPropertyNameForPackage(it)
            )
        }

    @JvmStatic
    fun readCompatConfigWidthDp(packageName: String?): Int? =
        packageName?.takeIf { it.isNotEmpty() }?.let {
            readOverrideValue(
                compatConfigPropertyNameForPackage(it),
                persistentCompatConfigPropertyNameForPackage(it)
            )
        }

    @JvmStatic
    fun readTargetSpec(packageName: String?): ViewportTargetSpec {
        if (packageName.isNullOrEmpty()) return ViewportTargetSpec.off()
        val type = readPropertyWithPersistentFallback(
            targetTypePropertyNameForPackage(packageName),
            persistentTargetTypePropertyNameForPackage(packageName)
        )
        val widthDp = readTargetWidthDp(packageName)
        val compatConfigWidthDp = readCompatConfigWidthDp(packageName)
        val scale = readOverrideValue(
            scalePropertyNameForPackage(packageName),
            persistentScalePropertyNameForPackage(packageName)
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
    fun readCompatMode(packageName: String?): String =
        if (packageName.isNullOrEmpty()) ViewportApplyMode.OFF else ViewportApplyMode.normalize(
            readPropertyWithPersistentFallback(
                compatModePropertyNameForPackage(packageName),
                persistentCompatModePropertyNameForPackage(packageName)
            )
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

    private fun hash(packageName: String) = String.format(Locale.US, "%08x", packageName.hashCode())
}
