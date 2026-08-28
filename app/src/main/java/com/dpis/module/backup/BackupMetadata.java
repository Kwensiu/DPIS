package com.dpis.module.backup;

/** Immutable metadata attached to a portable backup document. */
public final class BackupMetadata {
    public final int schemaVersion;
    public final long createdAtEpochMs;
    public final String packageName;
    public final long appVersionCode;
    public final String appVersionName;

    public BackupMetadata(int schemaVersion, long createdAtEpochMs, String packageName,
                          long appVersionCode, String appVersionName) {
        this.schemaVersion = schemaVersion;
        this.createdAtEpochMs = createdAtEpochMs;
        this.packageName = packageName;
        this.appVersionCode = appVersionCode;
        this.appVersionName = appVersionName;
    }
}
