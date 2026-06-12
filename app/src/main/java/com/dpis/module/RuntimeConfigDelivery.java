package com.dpis.module;

final class RuntimeConfigDelivery {
    private RuntimeConfigDelivery() {
    }

    static void publishLocalSnapshotAfterSave() {
        DpisApplication.reloadConfigStore();
    }
}
