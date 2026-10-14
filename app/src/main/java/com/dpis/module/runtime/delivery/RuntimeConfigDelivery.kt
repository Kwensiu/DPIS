package com.dpis.module.runtime.delivery

object RuntimeConfigDelivery {
    @Volatile
    private var localSnapshotReloader: Runnable = Runnable {}

    @JvmStatic
    fun setLocalSnapshotReloader(reloader: Runnable?) {
        localSnapshotReloader = reloader ?: Runnable {}
    }

    @JvmStatic
    fun publishLocalSnapshotAfterSave() {
        localSnapshotReloader.run()
    }
}
