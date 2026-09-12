package com.dpis.module.settings

/** JVM-testable state publisher over Java-owned Settings workflow execution. */
class SettingsPresentationController(private val port: Port) {
    interface Port {
        fun snapshot(): SettingsUiState
        fun setSafeModeEnabled(enabled: Boolean)
        fun setGlobalLogEnabled(enabled: Boolean)
        fun setLauncherIconHidden(hidden: Boolean)
        fun refresh()
    }

    fun interface Listener {
        fun onStateChanged(state: SettingsUiState)
    }

    private val listeners = LinkedHashSet<Listener>()

    fun addListener(listener: Listener?) {
        if (listener == null) return
        listeners.add(listener)
        listener.onStateChanged(port.snapshot())
    }

    fun removeListener(listener: Listener?) {
        listeners.remove(listener)
    }

    /**
     * Publishes only after the Java workflow reaches a stable observable state.
     * Confirmation dialogs and background work complete asynchronously, so the port
     * deliberately owns the timing rather than guessing from an action invocation.
     */
    fun publishState() {
        publish(port.snapshot())
    }

    fun setSafeModeEnabled(enabled: Boolean) {
        port.setSafeModeEnabled(enabled)
    }

    fun setGlobalLogEnabled(enabled: Boolean) {
        port.setGlobalLogEnabled(enabled)
    }

    fun setLauncherIconHidden(hidden: Boolean) {
        port.setLauncherIconHidden(hidden)
    }

    fun refresh() {
        port.refresh()
    }

    private fun publish(state: SettingsUiState) {
        for (listener in LinkedHashSet(listeners)) listener.onStateChanged(state)
    }
}
