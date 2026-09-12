package com.dpis.module.settings;

import java.util.LinkedHashSet;
import java.util.Set;

/** JVM-testable state publisher over Java-owned Settings workflow execution. */
public final class SettingsPresentationController {
    public interface Port {
        SettingsUiState snapshot();
        void setSafeModeEnabled(boolean enabled);
        void setGlobalLogEnabled(boolean enabled);
        void setLauncherIconHidden(boolean hidden);
        void refresh();
    }
    public interface Listener { void onStateChanged(SettingsUiState state); }

    private final Port port;
    private final Set<Listener> listeners = new LinkedHashSet<>();

    public SettingsPresentationController(Port port) { this.port = port; }

    public void addListener(Listener listener) {
        if (listener == null) return;
        listeners.add(listener);
        listener.onStateChanged(port.snapshot());
    }

    public void removeListener(Listener listener) { listeners.remove(listener); }

    /**
     * Publishes only after the Java workflow reaches a stable observable state.
     * Confirmation dialogs and background work complete asynchronously, so the port
     * deliberately owns the timing rather than guessing from an action invocation.
     */
    public void publishState() { publish(port.snapshot()); }

    public void setSafeModeEnabled(boolean enabled) { port.setSafeModeEnabled(enabled); }
    public void setGlobalLogEnabled(boolean enabled) { port.setGlobalLogEnabled(enabled); }
    public void setLauncherIconHidden(boolean hidden) { port.setLauncherIconHidden(hidden); }
    public void refresh() { port.refresh(); }

    private void publish(SettingsUiState state) {
        for (Listener listener : new LinkedHashSet<>(listeners)) listener.onStateChanged(state);
    }
}
