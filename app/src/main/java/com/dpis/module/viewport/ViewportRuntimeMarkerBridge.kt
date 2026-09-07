package com.dpis.module.viewport

import com.dpis.module.DpisLog
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object ViewportRuntimeMarkerBridge {
    // Android system property values are capped at 91 bytes including the
    // terminating NUL in native storage, so keep encoded marker payloads within
    // this conservative Java string length.
    const val MAX_SYSTEM_PROPERTY_VALUE_LENGTH: Int = 91

    private const val PROPERTY_PREFIX = "debug.dpis.vprtm."
    private const val VALUE_VERSION = "v2"
    private const val LEGACY_VALUE_VERSION = "v1"
    private const val HASH_HEX_LENGTH = 8
    private const val MAX_AGE_MILLIS = 30000L
    private const val PROVENANCE_APP_PROCESS = "a"
    private const val PROVENANCE_SYSTEM_SERVER = "s"
    private val PROCESS_LOCAL_MARKERS: MutableMap<String?, String?> =
        ConcurrentHashMap<String?, String?>()

    @JvmStatic
    fun propertyNameForPackage(packageName: String?): String {
        return PROPERTY_PREFIX + String.format(
            Locale.US,
            "%08x",
            safeString(packageName).hashCode()
        )
    }

    @JvmStatic
    fun createRecord(
        packageName: String?,
        targetSmallestWidthDp: Int,
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceSmallestWidthDp: Int,
        sourceDensityDpi: Int,
        resultWidthDp: Int,
        resultHeightDp: Int,
        resultSmallestWidthDp: Int,
        resultDensityDpi: Int,
        provenance: String?,
        elapsedRealtimeMillis: Long
    ): MarkerRecord {
        val targetFingerprint = "a" + toBase36(targetSmallestWidthDp)
        val sourceSignature: String = signature(
            sourceWidthDp,
            sourceHeightDp,
            sourceSmallestWidthDp,
            sourceDensityDpi
        )
        val resultSignature: String = signature(
            resultWidthDp,
            resultHeightDp,
            resultSmallestWidthDp,
            resultDensityDpi
        )
        return MarkerRecord(
            packageCheck(packageName),
            targetFingerprint,
            sourceSignature,
            max(1, resultSmallestWidthDp),
            resultSignature,
            resultWidthDp,
            resultHeightDp,
            resultSmallestWidthDp,
            resultDensityDpi,
            normalizeProvenance(provenance),
            max(0L, elapsedRealtimeMillis)
        )
    }

    @JvmStatic
    fun createRecord(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        effectiveSmallestWidthDp: Int,
        source: ViewportSourceSnapshot?,
        result: ViewportOverride.Result?,
        provenance: String?,
        elapsedRealtimeMillis: Long
    ): MarkerRecord? {
        if (source == null || result == null) {
            return null
        }
        return MarkerRecord(
            packageCheck(packageName),
            targetFingerprintForSpec(targetSpec),
            source.sourceSignature(),
            max(1, effectiveSmallestWidthDp),
            configurationSignature(
                result.widthDp,
                result.heightDp,
                result.smallestWidthDp,
                result.densityDpi,
                source.scope
            ),
            result.widthDp,
            result.heightDp,
            result.smallestWidthDp,
            result.densityDpi,
            normalizeProvenance(provenance),
            max(0L, elapsedRealtimeMillis)
        )
    }

    @JvmStatic
    fun encode(record: MarkerRecord?): String {
        if (record == null) {
            return ""
        }
        return (VALUE_VERSION
                + "|" + record.packageHash
                + "|" + record.targetFingerprint
                + "|" + record.sourceSignature
                + "|" + toBase36(record.effectiveSmallestWidthDp)
                + "|" + record.resultSignature
                + "|" + encodeResult(record)
                + "|" + record.provenance
                + "|" + toBase36(record.elapsedRealtimeMillis))
    }

    @JvmStatic
    fun parse(
        packageName: String?,
        expectedTargetFingerprint: String?,
        raw: String?,
        nowElapsedRealtimeMillis: Long
    ): ParseResult {
        return parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis, false)
    }

    @JvmStatic
    fun parseAllowingStale(
        packageName: String?,
        expectedTargetFingerprint: String?,
        raw: String?,
        nowElapsedRealtimeMillis: Long
    ): ParseResult {
        return parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis, true)
    }

    private fun parse(
        packageName: String?,
        expectedTargetFingerprint: String?,
        raw: String?,
        nowElapsedRealtimeMillis: Long,
        allowStale: Boolean
    ): ParseResult {
        if (raw == null || raw.trim { it <= ' ' }.isEmpty()) {
            return ParseResult.miss("empty")
        }
        val normalized = raw.trim { it <= ' ' }
        if (normalized.length > MAX_SYSTEM_PROPERTY_VALUE_LENGTH) {
            return ParseResult.miss("too-long")
        }
        val parts: Array<String?> = normalized.split("\\|".toRegex()).toTypedArray()
        val legacy = LEGACY_VALUE_VERSION == parts[0]
        val current = VALUE_VERSION == parts[0]
        if ((!legacy && !current)
            || (legacy && parts.size != 8)
            || (current && parts.size != 9)
        ) {
            return ParseResult.miss("malformed")
        }
        val expectedPackageHash = packageCheck(packageName)
        if (expectedPackageHash != parts[1]) {
            return ParseResult.miss("package-mismatch")
        }
        if (expectedTargetFingerprint != null && !expectedTargetFingerprint.isBlank() && (expectedTargetFingerprint != parts[2])) {
            return ParseResult.miss("target-mismatch")
        }
        val effectiveSmallestWidthDp = parseBase36Int(parts[4]!!)
        if (effectiveSmallestWidthDp == null || effectiveSmallestWidthDp <= 0) {
            return ParseResult.miss("malformed")
        }
        val provenanceIndex = if (legacy) 6 else 7
        val elapsedIndex = if (legacy) 7 else 8
        val provenance = parseProvenance(parts[provenanceIndex])
        if (provenance == null) {
            return ParseResult.miss("malformed")
        }
        var resultWidthDp = 0
        var resultHeightDp = 0
        var resultSmallestWidthDp = 0
        var resultDensityDpi = 0
        if (current) {
            val result = parseResult(parts[6])
            if (result == null) {
                return ParseResult.miss("malformed")
            }
            resultWidthDp = result[0]
            resultHeightDp = result[1]
            resultSmallestWidthDp = result[2]
            resultDensityDpi = result[3]
        }
        val elapsedRealtimeMillis =
            parseBase36Long(parts[elapsedIndex]!!)
        if (elapsedRealtimeMillis == null || elapsedRealtimeMillis < 0) {
            return ParseResult.miss("malformed")
        }
        val ageMillis = nowElapsedRealtimeMillis - elapsedRealtimeMillis
        if (ageMillis < 0 || (!allowStale && ageMillis > MAX_AGE_MILLIS)) {
            return ParseResult.miss("stale")
        }
        val record = MarkerRecord(
            parts[1],
            parts[2],
            parts[3],
            effectiveSmallestWidthDp,
            parts[5],
            resultWidthDp,
            resultHeightDp,
            resultSmallestWidthDp,
            resultDensityDpi,
            provenance,
            elapsedRealtimeMillis
        )
        return ParseResult.matched(record, ageMillis)
    }

    @JvmStatic
    fun targetFingerprintForAbsoluteDp(targetSmallestWidthDp: Int): String {
        return "a" + toBase36(targetSmallestWidthDp)
    }

    @JvmStatic
    fun targetFingerprintForSpec(spec: ViewportTargetSpec?): String {
        return if (spec != null) spec.fingerprint() else "off"
    }

    @JvmStatic
    fun configurationSignature(
        widthDp: Int,
        heightDp: Int,
        smallestWidthDp: Int,
        densityDpi: Int,
        scope: String?
    ): String {
        return signature(widthDp, heightDp, smallestWidthDp, densityDpi, scope)
    }

    @JvmStatic
    fun publish(packageName: String?, record: MarkerRecord?): Boolean {
        if (packageName == null || packageName.isBlank() || record == null) {
            return false
        }
        val value = encode(record)
        if (value.length > MAX_SYSTEM_PROPERTY_VALUE_LENGTH) {
            DpisLog.i(
                ("DPIS_VIEWPORT_MARKER publish skip: reason=too-long"
                        + ", package=" + packageName
                        + ", length=" + value.length)
            )
            return false
        }
        val propertyName = propertyNameForPackage(packageName)
        PROCESS_LOCAL_MARKERS.put(propertyName, value)
        val systemPropertyWritten = setSystemProperty(propertyName, value)
        if (!systemPropertyWritten) {
            DpisLog.i(
                ("DPIS_VIEWPORT_MARKER publish using process-local fallback"
                        + ", package=" + packageName
                        + ", property=" + propertyName)
            )
        }
        return true
    }

    @JvmStatic
    fun publishSystemServerRecord(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        source: ConfigurationLike?,
        result: ConfigurationLike?,
        scope: String?,
        elapsedRealtimeMillis: Long
    ): Boolean {
        if (packageName == null || packageName.isBlank()
            || targetSpec == null || !targetSpec.isEnabled || source == null || result == null
        ) {
            return false
        }
        val record = MarkerRecord(
            packageCheck(packageName),
            targetFingerprintForSpec(targetSpec),
            configurationSignature(
                source.widthDp(),
                source.heightDp(),
                source.smallestWidthDp(),
                source.densityDpi(),
                scope
            ),
            max(1, result.smallestWidthDp()),
            configurationSignature(
                result.widthDp(),
                result.heightDp(),
                result.smallestWidthDp(),
                result.densityDpi(),
                scope
            ),
            result.widthDp(),
            result.heightDp(),
            result.smallestWidthDp(),
            result.densityDpi(),
            PROVENANCE_SYSTEM_SERVER,
            max(0L, elapsedRealtimeMillis)
        )
        return publish(packageName, record)
    }

    @JvmStatic
    fun isCurrentMarker(packageName: String?, record: MarkerRecord?): Boolean {
        if (packageName == null || packageName.isBlank() || record == null) {
            return false
        }
        val current = readSystemProperty(propertyNameForPackage(packageName), "")
        return encode(record) == current
    }

    @JvmStatic
    fun read(
        packageName: String?,
        expectedTargetFingerprint: String?,
        nowElapsedRealtimeMillis: Long
    ): ParseResult {
        if (packageName == null || packageName.isBlank()) {
            return ParseResult.miss("empty-package")
        }
        val raw = readSystemProperty(propertyNameForPackage(packageName), "")
        val result = parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis)
        if (result.hit) {
            return result
        }
        val localResult = readProcessLocal(
            packageName, expectedTargetFingerprint, nowElapsedRealtimeMillis, false
        )
        return if (localResult.hit) localResult else result
    }

    @JvmStatic
    fun readAllowingStale(
        packageName: String?,
        expectedTargetFingerprint: String?,
        nowElapsedRealtimeMillis: Long
    ): ParseResult {
        if (packageName == null || packageName.isBlank()) {
            return ParseResult.miss("empty-package")
        }
        val raw = readSystemProperty(propertyNameForPackage(packageName), "")
        val result = parseAllowingStale(
            packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis
        )
        if (result.hit) {
            return result
        }
        val localResult = readProcessLocal(
            packageName, expectedTargetFingerprint, nowElapsedRealtimeMillis, true
        )
        return if (localResult.hit) localResult else result
    }

    private fun readProcessLocal(
        packageName: String?,
        expectedTargetFingerprint: String?,
        nowElapsedRealtimeMillis: Long,
        allowStale: Boolean
    ): ParseResult {
        val raw = PROCESS_LOCAL_MARKERS.get(propertyNameForPackage(packageName))
        return if (allowStale)
            parseAllowingStale(
                packageName,
                expectedTargetFingerprint,
                raw,
                nowElapsedRealtimeMillis
            )
        else
            parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis)
    }

    private fun setSystemProperty(key: String?, value: String?): Boolean {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val set =
                systemProperties.getDeclaredMethod("set", String::class.java, String::class.java)
            set.invoke(null, key, value)
            return true
        } catch (throwable: Throwable) {
            // Marker publication has a process-local fallback. When property
            // writes fail in ordinary app processes, the follow-up fallback log
            // is enough evidence without escalating to an error-level stack.
            return false
        }
    }

    private fun readSystemProperty(key: String?, fallback: String?): String? {
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get =
                systemProperties.getDeclaredMethod("get", String::class.java, String::class.java)
            val value = get.invoke(null, key, fallback)
            return if (value is String) value else fallback
        } catch (ignored: Throwable) {
            return fallback
        }
    }

    private fun encodeResult(record: MarkerRecord): String {
        return (toBase36(record.resultWidthDp)
                + "." + toBase36(record.resultHeightDp)
                + "." + toBase36(record.resultSmallestWidthDp)
                + "." + toBase36(record.resultDensityDpi))
    }

    private fun parseResult(value: String?): IntArray? {
        if (value == null) {
            return null
        }
        val parts: Array<String?> = value.split("\\.".toRegex()).toTypedArray()
        if (parts.size != 4) {
            return null
        }
        val result = IntArray(4)
        for (i in parts.indices) {
            val parsed = parseBase36Int(parts[i]!!)
            if (parsed == null || parsed < 0) {
                return null
            }
            result[i] = parsed
        }
        return result
    }

    private fun signature(
        widthDp: Int,
        heightDp: Int,
        smallestWidthDp: Int,
        densityDpi: Int,
        scope: String? = ""
    ): String {
        return shortHash(
            (widthDp.toString() + "x" + heightDp + "s" + smallestWidthDp
                    + "d" + densityDpi + "@" + safeString(scope))
        )
    }

    private fun packageCheck(packageName: String?): String {
        return shortHash(safeString(packageName))
    }

    private fun shortHash(value: String?): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(safeString(value).toByteArray(StandardCharsets.UTF_8))
            val builder = StringBuilder(HASH_HEX_LENGTH)
            for (b in bytes) {
                if (builder.length >= HASH_HEX_LENGTH) {
                    break
                }
                builder.append(String.format(Locale.US, "%02x", b))
            }
            return builder.substring(0, HASH_HEX_LENGTH)
        } catch (ignored: NoSuchAlgorithmException) {
            return String.format(Locale.US, "%08x", safeString(value).hashCode())
        }
    }

    private fun normalizeProvenance(provenance: String?): String {
        return if (PROVENANCE_APP_PROCESS == provenance)
            PROVENANCE_APP_PROCESS
        else
            PROVENANCE_SYSTEM_SERVER
    }

    private fun parseProvenance(provenance: String?): String? {
        if (PROVENANCE_APP_PROCESS == provenance) {
            return PROVENANCE_APP_PROCESS
        }
        if (PROVENANCE_SYSTEM_SERVER == provenance) {
            return PROVENANCE_SYSTEM_SERVER
        }
        return null
    }

    private fun toBase36(value: Int): String {
        return max(0, value).toString(36)
    }

    private fun toBase36(value: Long): String {
        return max(0L, value).toString(36)
    }

    private fun parseBase36Int(value: String): Int? {
        try {
            return value.toInt(36)
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun parseBase36Long(value: String): Long? {
        try {
            return value.toLong(36)
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun safeString(value: String?): String {
        return if (value == null) "" else value
    }

    class MarkerRecord(
        @JvmField val packageHash: String?,
        @JvmField val targetFingerprint: String?,
        @JvmField val sourceSignature: String?,
        @JvmField val effectiveSmallestWidthDp: Int,
        @JvmField val resultSignature: String?,
        @JvmField val resultWidthDp: Int,
        @JvmField val resultHeightDp: Int,
        @JvmField val resultSmallestWidthDp: Int,
        @JvmField val resultDensityDpi: Int,
        @JvmField val provenance: String?,
        @JvmField val elapsedRealtimeMillis: Long
    ) {
        constructor(
            packageHash: String?,
            targetFingerprint: String?,
            sourceSignature: String?,
            effectiveSmallestWidthDp: Int,
            resultSignature: String?,
            provenance: String?,
            elapsedRealtimeMillis: Long
        ) : this(
            packageHash,
            targetFingerprint,
            sourceSignature,
            effectiveSmallestWidthDp,
            resultSignature,
            0,
            0,
            0,
            0,
            provenance,
            elapsedRealtimeMillis
        )
    }

    interface ConfigurationLike {
        fun widthDp(): Int

        fun heightDp(): Int

        fun smallestWidthDp(): Int

        fun densityDpi(): Int
    }

    class ParseResult private constructor(
        @JvmField val hit: Boolean,
        @JvmField val record: MarkerRecord?,
        @JvmField val reason: String?,
        @JvmField val ageMillis: Long
    ) {
        companion object {
            @JvmStatic
            fun matched(record: MarkerRecord?, ageMillis: Long): ParseResult {
                return ParseResult(true, record, "hit", ageMillis)
            }

            @JvmStatic
            fun hit(record: MarkerRecord?, ageMillis: Long): ParseResult {
                return matched(record, ageMillis)
            }

            @JvmStatic
            fun miss(reason: String?): ParseResult {
                return ParseResult(false, null, reason, -1L)
            }
        }
    }
}
