package com.dpis.module.runtime;

public final class RuntimeConfigDelivery {
    private static volatile Runnable localSnapshotReloader = () -> {
    };

    private RuntimeConfigDelivery() {
    }

    public static void setLocalSnapshotReloader(Runnable reloader) {
        localSnapshotReloader = reloader != null ? reloader : () -> {
        };
    }

    public static void publishLocalSnapshotAfterSave() {
        localSnapshotReloader.run();
    }
}
