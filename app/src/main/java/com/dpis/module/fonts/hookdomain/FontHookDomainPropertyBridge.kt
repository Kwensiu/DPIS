package com.dpis.module.fonts.hookdomain

import com.dpis.module.hooks.HookDomainOverride
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

object FontHookDomainPropertyBridge {
    private const val PROPERTY_PREFIX = "debug.dpis.hookdomains."
    private const val PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.hookdomains."
    private const val VALUE_VERSION_PREFIX = "v2:"
    private const val PACKAGE_CHECK_HEX_LENGTH = 12

    // Property names carry a stable package hash so global system properties do not expose names.
    @JvmStatic
    fun propertyNameForPackage(packageName: String): String =
        PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode())

    @JvmStatic
    fun persistentPropertyNameForPackage(packageName: String): String =
        PERSIST_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode())

    @JvmStatic
    fun readOverride(packageName: String?): HookDomainOverride {
        if (packageName.isNullOrBlank()) {
            return HookDomainOverride.automatic()
        }
        val raw = readPropertyWithPersistentFallback(
            propertyNameForPackage(packageName),
            persistentPropertyNameForPackage(packageName),
        )
        return parseOverrideValue(packageName, raw)
    }

    @JvmStatic
    fun parseOverrideValueForTest(raw: String?): HookDomainOverride = parseOverrideValue("", raw)

    @JvmStatic
    fun parseOverrideValueForTest(packageName: String?, raw: String?): HookDomainOverride =
        parseOverrideValue(packageName, raw)

    @JvmStatic
    fun encodeOverrideValue(packageName: String?, domains: Set<String>): String {
        val encoded = encodeMask(domains) + 1
        return VALUE_VERSION_PREFIX + packageCheck(packageName) + ":" + encoded
    }

    @JvmStatic
    fun encodeMask(domains: Set<String>): Int {
        val ids = FontHookDomainRegistry.orderedCustomizableIdsList()
        val normalized = FontHookDomainRegistry.orderedCustomizableSubset(domains)
        var mask = 0
        ids.forEachIndexed { index, id ->
            if (normalized.contains(id)) {
                mask = mask or (1 shl index)
            }
        }
        return mask
    }

    @JvmStatic
    fun decodeMask(mask: Int): Set<String> {
        val domains = LinkedHashSet<String>()
        val ids = FontHookDomainRegistry.orderedCustomizableIdsList()
        ids.forEachIndexed { index, id ->
            if (mask and (1 shl index) != 0) {
                domains.add(id)
            }
        }
        return domains
    }

    private fun parseOverrideValue(packageName: String?, raw: String?): HookDomainOverride {
        if (raw.isNullOrBlank() || raw.trim() == "0") {
            return HookDomainOverride.automatic()
        }
        var normalized = raw.trim()
        if (normalized.startsWith(VALUE_VERSION_PREFIX)) {
            normalized = parseVerifiedV2Value(packageName, normalized)
                ?: return HookDomainOverride.automatic()
        }
        val encoded = normalized.toIntOrNull()
            ?: return HookDomainOverride.automatic()
        if (encoded <= 0) {
            return HookDomainOverride.automatic()
        }
        return HookDomainOverride(true, decodeMask(encoded - 1), emptySet())
    }

    private fun parseVerifiedV2Value(packageName: String?, raw: String): String? {
        val parts = raw.split(":", limit = 3)
        if (parts.size != 3 || parts[0] != "v2" || packageCheck(packageName) != parts[1]) {
            return null
        }
        return parts[2]
    }

    private fun packageCheck(packageName: String?): String {
        val value = packageName.orEmpty()
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(value.toByteArray(StandardCharsets.UTF_8))
            val builder = StringBuilder(PACKAGE_CHECK_HEX_LENGTH)
            for (byte in bytes) {
                if (builder.length >= PACKAGE_CHECK_HEX_LENGTH) {
                    break
                }
                builder.append(String.format(Locale.US, "%02x", byte))
            }
            builder.substring(0, PACKAGE_CHECK_HEX_LENGTH)
        } catch (_: NoSuchAlgorithmException) {
            String.format(Locale.US, "%08x", value.hashCode())
        }
    }

    private fun readPropertyWithPersistentFallback(key: String, persistentKey: String): String {
        val value = readSystemProperty(key)
        return if (!value.isNullOrBlank()) value else readSystemProperty(persistentKey)
    }

    private fun readSystemProperty(key: String): String {
        // SystemProperties is hidden from regular app classpaths, so keep this boundary reflective.
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemProperties.getDeclaredMethod(
                "get",
                String::class.java,
                String::class.java,
            )
            getMethod.invoke(null, key, "") as? String ?: ""
        } catch (_: Throwable) {
            ""
        }
    }
}
