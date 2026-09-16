package com.dpis.module.tools

/** View-free state/action boundary for the system font scale tool. */
class SystemFontScaleToolPresenter(
    private val gateway: Gateway,
    private val listener: Listener?,
) {
    interface Listener {
        fun onStateChanged(state: SystemFontScaleToolState)
        fun onWriteFailed()
    }

    interface Gateway {
        fun readPercent(): Int?
        fun canWrite(): Boolean
        fun writePercent(percent: Int): Boolean
    }

    var state: SystemFontScaleToolState? = null
        private set

    fun refresh() {
        val canWrite = gateway.canWrite()
        val current = gateway.readPercent()
        val currentState = state
        val pending = if (currentState != null && currentState.userSelectedPending) {
            currentState.pendingPercent
        } else {
            SystemFontScaleToolState.initialPendingPercent(current)
        }
        state = SystemFontScaleToolState(
            canWrite,
            current,
            pending,
            currentState != null && currentState.userSelectedPending,
            current == null,
        )
        publish()
    }

    fun selectPendingPercent(percent: Int) {
        if (state == null) refresh()
        val currentState = state ?: return
        state = SystemFontScaleToolState(
            currentState.canWrite,
            currentState.currentPercent,
            SystemFontScaleToolState.clampPercent(percent),
            true,
            currentState.unavailable,
        )
        publish()
    }

    fun apply() {
        val currentState = state
        if (currentState == null || !currentState.canApply()) return
        if (!gateway.writePercent(currentState.pendingPercent)) {
            listener?.onWriteFailed()
            return
        }
        refresh()
    }

    fun restoreDefault() {
        val currentState = state
        if (currentState == null || !currentState.canRestore()) return
        selectPendingPercent(SystemFontScaleToolState.DEFAULT_PERCENT)
        apply()
    }

    private fun publish() {
        val currentState = state ?: return
        listener?.onStateChanged(currentState)
    }
}
