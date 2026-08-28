package com.dpis.module.backup;

import android.content.ContentResolver;
import android.net.Uri;

import com.dpis.module.DpisConfigStore;
import com.dpis.module.templates.QuickTemplateStore;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Coordinates portable backup I/O and cross-store restore semantics. */
public final class ConfigBackupCoordinator {
    public static final int MAX_BACKUP_BYTES = 4 * 1024 * 1024;

    public enum Code { SUCCESS, INVALID_FILE, IO_ERROR, RESTORE_ERROR, ROLLBACK_ERROR }

    public static final class Result {
        public final Code code;
        public final Throwable cause;

        private Result(Code code, Throwable cause) {
            this.code = code;
            this.cause = cause;
        }

        public boolean isSuccess() { return code == Code.SUCCESS; }
        public static Result success() { return new Result(Code.SUCCESS, null); }
        private static Result failure(Code code, Throwable cause) { return new Result(code, cause); }
    }

    private final ContentResolver resolver;
    private final DpisConfigStore configStore;
    private final QuickTemplateStore templateStore;

    public ConfigBackupCoordinator(ContentResolver resolver, DpisConfigStore configStore,
                                   QuickTemplateStore templateStore) {
        this.resolver = resolver;
        this.configStore = configStore;
        this.templateStore = templateStore;
    }

    public Result export(Uri uri) {
        if (uri == null) return Result.failure(Code.IO_ERROR, null);
        Map<String, Object> entries = configStore.snapshotBackup();
        templateStore.copyToBackup(entries);
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) return Result.failure(Code.IO_ERROR, null);
            output.write(ConfigBackupCodec.encode(entries).getBytes(StandardCharsets.UTF_8));
            return Result.success();
        } catch (IOException | JSONException | RuntimeException error) {
            return Result.failure(Code.IO_ERROR, error);
        }
    }

    public Result restore(Uri uri) {
        if (uri == null) return Result.failure(Code.INVALID_FILE, null);
        final String payload;
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) return Result.failure(Code.IO_ERROR, null);
            payload = readLimited(input);
        } catch (IOException error) {
            return Result.failure(Code.IO_ERROR, error);
        }
        final Map<String, Object> incoming;
        try {
            incoming = ConfigBackupCodec.decode(payload);
        } catch (JSONException | IllegalArgumentException error) {
            return Result.failure(Code.INVALID_FILE, error);
        }
        for (String key : incoming.keySet()) {
            if (!BackupKeyPolicy.isImportable(key)) {
                return Result.failure(Code.INVALID_FILE,
                        new IllegalArgumentException("Unknown backup key: " + key));
            }
        }
        normalizeLegacyTargetPackages(incoming);
        Map<String, Object> snapshot = configStore.snapshotBackup();
        templateStore.copyToBackup(snapshot);
        Map<String, Object> configEntries = new LinkedHashMap<>(incoming);
        configEntries.entrySet().removeIf(entry -> entry.getKey().startsWith("template."));
        if (!configStore.replaceBackup(configEntries)) {
            return Result.failure(Code.RESTORE_ERROR, null);
        }
        boolean hasTemplates = QuickTemplateStore.containsTemplateEntries(incoming);
        if (!hasTemplates || templateStore.restoreFromBackup(incoming)) {
            return Result.success();
        }
        Map<String, Object> rollbackConfig = new LinkedHashMap<>(snapshot);
        rollbackConfig.entrySet().removeIf(entry -> entry.getKey().startsWith("template."));
        boolean configRolledBack = configStore.replaceBackup(rollbackConfig);
        boolean templatesRolledBack = templateStore.restoreFromBackup(snapshot);
        return Result.failure(configRolledBack && templatesRolledBack
                ? Code.RESTORE_ERROR : Code.ROLLBACK_ERROR, null);
    }

    private static String readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BACKUP_BYTES) throw new IOException("Backup exceeds size limit");
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static void normalizeLegacyTargetPackages(Map<String, Object> entries) {
        boolean hasAggregatedPackages = entries.keySet().stream()
                .anyMatch(key -> key.startsWith("package_config."));
        if (hasAggregatedPackages) {
            entries.remove("target_packages");
            return;
        }
        Object raw = entries.get("target_packages");
        if (!(raw instanceof java.util.Set<?> values)) return;
        java.util.LinkedHashSet<String> valid = new java.util.LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof String packageName
                    && packageName.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) {
                valid.add(packageName);
            }
        }
        if (valid.isEmpty()) entries.remove("target_packages");
        else entries.put("target_packages", valid);
    }
}
