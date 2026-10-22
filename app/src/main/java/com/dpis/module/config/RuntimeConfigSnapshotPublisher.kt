package com.dpis.module.config

object RuntimeConfigSnapshotPublisher {
    @JvmStatic
    fun publish(localStore: DpisConfigStore?, runtimeStore: DpisConfigStore?): Boolean {
        if (localStore == null || runtimeStore == null) return false
        if (localStore === runtimeStore) return true
        return try {
            val remoteSnapshot = runtimeStore.snapshotRuntimeDelivery()
            if (!localStore.mergeRuntimeDeliverySnapshotOnce(remoteSnapshot)) return false
            runtimeStore.replaceAll(localStore.snapshotRuntimeDelivery())
        } catch (_: Exception) {
            false
        }
    }
}
