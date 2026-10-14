package com.dpis.module.runtime.probe

object RuntimeClock {
    @JvmStatic
    fun elapsedRealtimeMillis(): Long = try {
        android.os.SystemClock.elapsedRealtime()
    } catch (_: RuntimeException) {
        System.currentTimeMillis()
    }

    @JvmStatic
    fun crossProcessMarkerMillis(): Long = System.currentTimeMillis()
}
