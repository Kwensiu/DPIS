package com.dpis.module.backup;

public final class BackupReplaceResult {
    public final boolean success;
    public final BackupReplaceStage failedStage;

    private BackupReplaceResult(boolean success, BackupReplaceStage failedStage) {
        this.success = success;
        this.failedStage = failedStage;
    }

    public boolean isSuccess() { return success; }
    public static BackupReplaceResult success() { return new BackupReplaceResult(true, null); }
    public static BackupReplaceResult failed(BackupReplaceStage stage) {
        return new BackupReplaceResult(false, stage);
    }
}
