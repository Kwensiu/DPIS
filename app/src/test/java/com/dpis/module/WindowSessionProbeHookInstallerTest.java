package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WindowSessionProbeHookInstallerTest {
    @Test
    public void buildsRelayoutAfterLog() {
        String message = WindowSessionProbeHookInstaller.buildLog(
                "relayout", "after", 3, 1080, 2376);

        assertEquals(
                "WindowSession probe(relayout:after): result=3, frame=1080x2376",
                message);
    }

    @Test
    public void buildsRelayoutBeforeLog() {
        String message = WindowSessionProbeHookInstaller.buildLog(
                "relayout", "before", -1, 561, 231);

        assertEquals(
                "WindowSession probe(relayout:before): frame=561x231",
                message);
    }
}
