package com.dpis.module;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SettingsPresentationControllerTest {
    @Test public void listenerGetsInitialAndPublishedSnapshotsAndCanBeRemoved() {
        FakePort port = new FakePort();
        SettingsPresentationController controller = new SettingsPresentationController(port);
        int[] calls = {0};
        SettingsPresentationController.Listener listener = state -> calls[0]++;
        controller.addListener(listener);
        controller.setGlobalLogEnabled(true);
        controller.publishState();
        controller.removeListener(listener);
        controller.refresh();
        assertEquals(2, calls[0]);
        assertEquals(1, port.globalLogWrites);
        assertEquals(1, port.refreshes);
    }
    private static final class FakePort implements SettingsPresentationController.Port {
        int globalLogWrites; int refreshes;
        @Override public SettingsUiState snapshot() { return new SettingsUiState(true,false,false,false,false,100,false,"0 B", "Follow system"); }
        @Override public void setSafeModeEnabled(boolean enabled) { }
        @Override public void setGlobalLogEnabled(boolean enabled) { globalLogWrites++; }
        @Override public void setLauncherIconHidden(boolean hidden) { }
        @Override public void refresh() { refreshes++; }
    }
}
