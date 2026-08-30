package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConfigEditorDestinationTest {
    @Test
    public void hookChainTabsRemainInsideTheSameEditorDestinationFamily() {
        assertFalse(ConfigEditorDestination.MAIN.isHookChain());
        assertTrue(ConfigEditorDestination.HOOK_CHAIN_INTERFACE.isHookChain());
        assertTrue(ConfigEditorDestination.HOOK_CHAIN_FONT.isHookChain());
        assertFalse(ConfigEditorDestination.TYPEFACE.isHookChain());
        assertFalse(ConfigEditorDestination.MAIN.isChildPage());
        assertTrue(ConfigEditorDestination.HOOK_CHAIN_INTERFACE.isChildPage());
        assertTrue(ConfigEditorDestination.TYPEFACE.isChildPage());
        assertEquals(0, ConfigEditorDestination.HOOK_CHAIN_INTERFACE.hookChainTabIndex());
        assertEquals(1, ConfigEditorDestination.HOOK_CHAIN_FONT.hookChainTabIndex());
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_INTERFACE,
                ConfigEditorDestination.forHookChainTab(0));
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT,
                ConfigEditorDestination.forHookChainTab(1));
    }

    @Test
    public void persistedDestinationFallsBackToMainWhenMissingOrUnknown() {
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT,
                ConfigEditorDestination.fromName("HOOK_CHAIN_FONT"));
        assertEquals(ConfigEditorDestination.MAIN,
                ConfigEditorDestination.fromName(null));
        assertEquals(ConfigEditorDestination.MAIN,
                ConfigEditorDestination.fromName("REMOVED_DESTINATION"));
    }

    @Test
    public void bothHookChainTabsReturnToMainEditorContent() {
        assertEquals(ConfigEditorDestination.MAIN,
                ConfigEditorDestination.HOOK_CHAIN_INTERFACE.backDestination());
        assertEquals(ConfigEditorDestination.MAIN,
                ConfigEditorDestination.HOOK_CHAIN_FONT.backDestination());
    }
}
