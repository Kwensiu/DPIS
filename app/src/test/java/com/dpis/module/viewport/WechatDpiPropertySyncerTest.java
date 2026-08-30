package com.dpis.module;

import com.dpis.module.quirks.WechatDpiPropertySyncer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WechatDpiPropertySyncerTest {
    @Test
    public void wechatDpiCommandWritesVolatileAndPersistentTarget() {
        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '360'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '360'",
                WechatDpiPropertySyncer.buildDpiCommandForTest(
                        "com.tencent.mm", 360));
    }

    @Test
    public void wechatDpiCommandClearsVolatileAndPersistentTargetWhenValueMissing() {
        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '0'",
                WechatDpiPropertySyncer.buildDpiCommandForTest(
                        "com.tencent.mm", null));
    }

    @Test
    public void wechatDpiCommandClearsVolatileAndPersistentTargetWhenValueInvalid() {
        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '0'",
                WechatDpiPropertySyncer.buildDpiCommandForTest(
                        "com.tencent.mm", 0));
    }

    @Test
    public void syncCommandPublishesConfiguredWechatDpiWhenTargetEnabled() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setWechatDpi("com.tencent.mm", 600);

        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '600'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '600'",
                WechatDpiPropertySyncer.buildSyncCommandForTest(store));
    }

    @Test
    public void syncCommandClearsWechatDpiWhenTargetDisabled() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setWechatDpi("com.tencent.mm", 600);
        store.setTargetDpisEnabled("com.tencent.mm", false);

        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '0'",
                WechatDpiPropertySyncer.buildSyncCommandForTest(store));
    }

    @Test
    public void syncCommandClearsWechatDpiEvenWhenPackageNoLongerConfigured() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertEquals("setprop 'debug.dpis.wechat.dpi.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.dpi.c5fe9776' '0'",
                WechatDpiPropertySyncer.buildSyncCommandForTest(store));
    }
}
