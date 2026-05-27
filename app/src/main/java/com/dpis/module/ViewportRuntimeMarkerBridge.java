package com.dpis.module;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class ViewportRuntimeMarkerBridge {
    // Android system property values are capped at 91 bytes including the
    // terminating NUL in native storage, so keep encoded marker payloads within
    // this conservative Java string length.
    static final int MAX_SYSTEM_PROPERTY_VALUE_LENGTH = 91;

    private static final String PROPERTY_PREFIX = "debug.dpis.vprtm.";
    private static final String VALUE_VERSION = "v2";
    private static final String LEGACY_VALUE_VERSION = "v1";
    private static final int HASH_HEX_LENGTH = 8;
    private static final long MAX_AGE_MILLIS = 30_000L;
    private static final String PROVENANCE_APP_PROCESS = "a";
    private static final String PROVENANCE_SYSTEM_SERVER = "s";

    private ViewportRuntimeMarkerBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", safeString(packageName).hashCode());
    }

    static MarkerRecord createRecord(String packageName,
                                     int targetSmallestWidthDp,
                                     int sourceWidthDp,
                                     int sourceHeightDp,
                                     int sourceSmallestWidthDp,
                                     int sourceDensityDpi,
                                     int resultWidthDp,
                                     int resultHeightDp,
                                     int resultSmallestWidthDp,
                                     int resultDensityDpi,
                                     String provenance,
                                     long elapsedRealtimeMillis) {
        String targetFingerprint = "a" + toBase36(targetSmallestWidthDp);
        String sourceSignature = signature(
                sourceWidthDp,
                sourceHeightDp,
                sourceSmallestWidthDp,
                sourceDensityDpi);
        String resultSignature = signature(
                resultWidthDp,
                resultHeightDp,
                resultSmallestWidthDp,
                resultDensityDpi);
        return new MarkerRecord(
                packageCheck(packageName),
                targetFingerprint,
                sourceSignature,
                Math.max(1, resultSmallestWidthDp),
                resultSignature,
                resultWidthDp,
                resultHeightDp,
                resultSmallestWidthDp,
                resultDensityDpi,
                normalizeProvenance(provenance),
                Math.max(0L, elapsedRealtimeMillis));
    }

    static MarkerRecord createRecord(String packageName,
                                     ViewportTargetSpec targetSpec,
                                     int effectiveSmallestWidthDp,
                                     ViewportSourceSnapshot source,
                                     ViewportOverride.Result result,
                                     String provenance,
                                     long elapsedRealtimeMillis) {
        if (source == null || result == null) {
            return null;
        }
        return new MarkerRecord(
                packageCheck(packageName),
                targetFingerprintForSpec(targetSpec),
                source.sourceSignature(),
                Math.max(1, effectiveSmallestWidthDp),
                configurationSignature(
                        result.widthDp,
                        result.heightDp,
                        result.smallestWidthDp,
                        result.densityDpi,
                        source.scope),
                result.widthDp,
                result.heightDp,
                result.smallestWidthDp,
                result.densityDpi,
                normalizeProvenance(provenance),
                Math.max(0L, elapsedRealtimeMillis));
    }

    static String encode(MarkerRecord record) {
        if (record == null) {
            return "";
        }
        return VALUE_VERSION
                + "|" + record.packageHash
                + "|" + record.targetFingerprint
                + "|" + record.sourceSignature
                + "|" + toBase36(record.effectiveSmallestWidthDp)
                + "|" + record.resultSignature
                + "|" + encodeResult(record)
                + "|" + record.provenance
                + "|" + toBase36(record.elapsedRealtimeMillis);
    }

    static ParseResult parse(String packageName,
                             String expectedTargetFingerprint,
                             String raw,
                             long nowElapsedRealtimeMillis) {
        return parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis, false);
    }

    static ParseResult parseAllowingStale(String packageName,
                                          String expectedTargetFingerprint,
                                          String raw,
                                          long nowElapsedRealtimeMillis) {
        return parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis, true);
    }

    private static ParseResult parse(String packageName,
                                     String expectedTargetFingerprint,
                                     String raw,
                                     long nowElapsedRealtimeMillis,
                                     boolean allowStale) {
        if (raw == null || raw.trim().isEmpty()) {
            return ParseResult.miss("empty");
        }
        String normalized = raw.trim();
        if (normalized.length() > MAX_SYSTEM_PROPERTY_VALUE_LENGTH) {
            return ParseResult.miss("too-long");
        }
        String[] parts = normalized.split("\\|", -1);
        boolean legacy = LEGACY_VALUE_VERSION.equals(parts[0]);
        boolean current = VALUE_VERSION.equals(parts[0]);
        if ((!legacy && !current)
                || (legacy && parts.length != 8)
                || (current && parts.length != 9)) {
            return ParseResult.miss("malformed");
        }
        String expectedPackageHash = packageCheck(packageName);
        if (!expectedPackageHash.equals(parts[1])) {
            return ParseResult.miss("package-mismatch");
        }
        if (expectedTargetFingerprint != null
                && !expectedTargetFingerprint.isBlank()
                && !expectedTargetFingerprint.equals(parts[2])) {
            return ParseResult.miss("target-mismatch");
        }
        Integer effectiveSmallestWidthDp = parseBase36Int(parts[4]);
        if (effectiveSmallestWidthDp == null || effectiveSmallestWidthDp <= 0) {
            return ParseResult.miss("malformed");
        }
        int provenanceIndex = legacy ? 6 : 7;
        int elapsedIndex = legacy ? 7 : 8;
        String provenance = parseProvenance(parts[provenanceIndex]);
        if (provenance == null) {
            return ParseResult.miss("malformed");
        }
        int resultWidthDp = 0;
        int resultHeightDp = 0;
        int resultSmallestWidthDp = 0;
        int resultDensityDpi = 0;
        if (current) {
            int[] result = parseResult(parts[6]);
            if (result == null) {
                return ParseResult.miss("malformed");
            }
            resultWidthDp = result[0];
            resultHeightDp = result[1];
            resultSmallestWidthDp = result[2];
            resultDensityDpi = result[3];
        }
        Long elapsedRealtimeMillis = parseBase36Long(parts[elapsedIndex]);
        if (elapsedRealtimeMillis == null || elapsedRealtimeMillis < 0) {
            return ParseResult.miss("malformed");
        }
        long ageMillis = nowElapsedRealtimeMillis - elapsedRealtimeMillis;
        if (ageMillis < 0 || (!allowStale && ageMillis > MAX_AGE_MILLIS)) {
            return ParseResult.miss("stale");
        }
        MarkerRecord record = new MarkerRecord(
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
                elapsedRealtimeMillis);
        return ParseResult.hit(record, ageMillis);
    }

    static String targetFingerprintForAbsoluteDp(int targetSmallestWidthDp) {
        return "a" + toBase36(targetSmallestWidthDp);
    }

    static String targetFingerprintForSpec(ViewportTargetSpec spec) {
        return spec != null ? spec.fingerprint() : "off";
    }

    static String configurationSignature(int widthDp,
                                         int heightDp,
                                         int smallestWidthDp,
                                         int densityDpi,
                                         String scope) {
        return signature(widthDp, heightDp, smallestWidthDp, densityDpi, scope);
    }

    static boolean publish(String packageName, MarkerRecord record) {
        if (packageName == null || packageName.isBlank() || record == null) {
            return false;
        }
        String value = encode(record);
        if (value.length() > MAX_SYSTEM_PROPERTY_VALUE_LENGTH) {
            DpisLog.i("DPIS_VIEWPORT_MARKER publish skip: reason=too-long"
                    + ", package=" + packageName
                    + ", length=" + value.length());
            return false;
        }
        return setSystemProperty(propertyNameForPackage(packageName), value);
    }

    static boolean publishSystemServerRecord(String packageName,
                                             ViewportTargetSpec targetSpec,
                                             ConfigurationLike source,
                                             ConfigurationLike result,
                                             String scope,
                                             long elapsedRealtimeMillis) {
        if (packageName == null || packageName.isBlank()
                || targetSpec == null || !targetSpec.isEnabled()
                || source == null || result == null) {
            return false;
        }
        MarkerRecord record = new MarkerRecord(
                packageCheck(packageName),
                targetFingerprintForSpec(targetSpec),
                configurationSignature(
                        source.widthDp(),
                        source.heightDp(),
                        source.smallestWidthDp(),
                        source.densityDpi(),
                        scope),
                Math.max(1, result.smallestWidthDp()),
                configurationSignature(
                        result.widthDp(),
                        result.heightDp(),
                        result.smallestWidthDp(),
                        result.densityDpi(),
                        scope),
                result.widthDp(),
                result.heightDp(),
                result.smallestWidthDp(),
                result.densityDpi(),
                PROVENANCE_SYSTEM_SERVER,
                Math.max(0L, elapsedRealtimeMillis));
        return publish(packageName, record);
    }

    static boolean isCurrentMarker(String packageName, MarkerRecord record) {
        if (packageName == null || packageName.isBlank() || record == null) {
            return false;
        }
        String current = readSystemProperty(propertyNameForPackage(packageName), "");
        return encode(record).equals(current);
    }

    static ParseResult read(String packageName,
                            String expectedTargetFingerprint,
                            long nowElapsedRealtimeMillis) {
        if (packageName == null || packageName.isBlank()) {
            return ParseResult.miss("empty-package");
        }
        String raw = readSystemProperty(propertyNameForPackage(packageName), "");
        return parse(packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis);
    }

    static ParseResult readAllowingStale(String packageName,
                                         String expectedTargetFingerprint,
                                         long nowElapsedRealtimeMillis) {
        if (packageName == null || packageName.isBlank()) {
            return ParseResult.miss("empty-package");
        }
        String raw = readSystemProperty(propertyNameForPackage(packageName), "");
        return parseAllowingStale(
                packageName, expectedTargetFingerprint, raw, nowElapsedRealtimeMillis);
    }

    private static boolean setSystemProperty(String key, String value) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method set = systemProperties.getDeclaredMethod("set", String.class, String.class);
            set.invoke(null, key, value);
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_VIEWPORT_MARKER publish failed: key=" + key, throwable);
            return false;
        }
    }

    private static String readSystemProperty(String key, String fallback) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, fallback);
            return value instanceof String ? (String) value : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String signature(int widthDp, int heightDp, int smallestWidthDp, int densityDpi) {
        return signature(widthDp, heightDp, smallestWidthDp, densityDpi, "");
    }

    private static String encodeResult(MarkerRecord record) {
        return toBase36(record.resultWidthDp)
                + "." + toBase36(record.resultHeightDp)
                + "." + toBase36(record.resultSmallestWidthDp)
                + "." + toBase36(record.resultDensityDpi);
    }

    private static int[] parseResult(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        int[] result = new int[4];
        for (int i = 0; i < parts.length; i++) {
            Integer parsed = parseBase36Int(parts[i]);
            if (parsed == null || parsed < 0) {
                return null;
            }
            result[i] = parsed;
        }
        return result;
    }

    private static String signature(int widthDp,
                                    int heightDp,
                                    int smallestWidthDp,
                                    int densityDpi,
                                    String scope) {
        return shortHash(widthDp + "x" + heightDp + "s" + smallestWidthDp
                + "d" + densityDpi + "@" + safeString(scope));
    }

    private static String packageCheck(String packageName) {
        return shortHash(safeString(packageName));
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safeString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(HASH_HEX_LENGTH);
            for (byte b : bytes) {
                if (builder.length() >= HASH_HEX_LENGTH) {
                    break;
                }
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.substring(0, HASH_HEX_LENGTH);
        } catch (NoSuchAlgorithmException ignored) {
            return String.format(Locale.US, "%08x", safeString(value).hashCode());
        }
    }

    private static String normalizeProvenance(String provenance) {
        return PROVENANCE_APP_PROCESS.equals(provenance)
                ? PROVENANCE_APP_PROCESS
                : PROVENANCE_SYSTEM_SERVER;
    }

    private static String parseProvenance(String provenance) {
        if (PROVENANCE_APP_PROCESS.equals(provenance)) {
            return PROVENANCE_APP_PROCESS;
        }
        if (PROVENANCE_SYSTEM_SERVER.equals(provenance)) {
            return PROVENANCE_SYSTEM_SERVER;
        }
        return null;
    }

    private static String toBase36(int value) {
        return Integer.toString(Math.max(0, value), 36);
    }

    private static String toBase36(long value) {
        return Long.toString(Math.max(0L, value), 36);
    }

    private static Integer parseBase36Int(String value) {
        try {
            return Integer.parseInt(value, 36);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Long parseBase36Long(String value) {
        try {
            return Long.parseLong(value, 36);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    static final class MarkerRecord {
        final String packageHash;
        final String targetFingerprint;
        final String sourceSignature;
        final int effectiveSmallestWidthDp;
        final String resultSignature;
        final int resultWidthDp;
        final int resultHeightDp;
        final int resultSmallestWidthDp;
        final int resultDensityDpi;
        final String provenance;
        final long elapsedRealtimeMillis;

        MarkerRecord(String packageHash,
                     String targetFingerprint,
                     String sourceSignature,
                     int effectiveSmallestWidthDp,
                     String resultSignature,
                     String provenance,
                     long elapsedRealtimeMillis) {
            this(packageHash,
                    targetFingerprint,
                    sourceSignature,
                    effectiveSmallestWidthDp,
                    resultSignature,
                    0,
                    0,
                    0,
                    0,
                    provenance,
                    elapsedRealtimeMillis);
        }

        MarkerRecord(String packageHash,
                     String targetFingerprint,
                     String sourceSignature,
                     int effectiveSmallestWidthDp,
                     String resultSignature,
                     int resultWidthDp,
                     int resultHeightDp,
                     int resultSmallestWidthDp,
                     int resultDensityDpi,
                     String provenance,
                     long elapsedRealtimeMillis) {
            this.packageHash = packageHash;
            this.targetFingerprint = targetFingerprint;
            this.sourceSignature = sourceSignature;
            this.effectiveSmallestWidthDp = effectiveSmallestWidthDp;
            this.resultSignature = resultSignature;
            this.resultWidthDp = resultWidthDp;
            this.resultHeightDp = resultHeightDp;
            this.resultSmallestWidthDp = resultSmallestWidthDp;
            this.resultDensityDpi = resultDensityDpi;
            this.provenance = provenance;
            this.elapsedRealtimeMillis = elapsedRealtimeMillis;
        }
    }

    interface ConfigurationLike {
        int widthDp();

        int heightDp();

        int smallestWidthDp();

        int densityDpi();
    }

    static final class ParseResult {
        final boolean hit;
        final MarkerRecord record;
        final String reason;
        final long ageMillis;

        private ParseResult(boolean hit, MarkerRecord record, String reason, long ageMillis) {
            this.hit = hit;
            this.record = record;
            this.reason = reason;
            this.ageMillis = ageMillis;
        }

        static ParseResult hit(MarkerRecord record, long ageMillis) {
            return new ParseResult(true, record, "hit", ageMillis);
        }

        static ParseResult miss(String reason) {
            return new ParseResult(false, null, reason, -1L);
        }
    }
}
