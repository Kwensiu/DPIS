package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WechatTargetFieldPropertySyncerTest {
    @Test
    public void publishCommandWritesVolatileAndPersistentTarget() {
        assertEquals("setprop 'debug.dpis.wechat.targetfield.c5fe9776' '800'; "
                        + "setprop 'persist.debug.dpis.wechat.targetfield.c5fe9776' '800'",
                WechatTargetFieldPropertySyncer.buildTargetCommandForTest(
                        "com.tencent.mm", 800));
    }

    @Test
    public void publishCommandClearsVolatileAndPersistentTargetWhenValueMissing() {
        assertEquals("setprop 'debug.dpis.wechat.targetfield.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.targetfield.c5fe9776' '0'",
                WechatTargetFieldPropertySyncer.buildTargetCommandForTest(
                        "com.tencent.mm", null));
    }

    @Test
    public void publishCommandClearsVolatileAndPersistentTargetWhenValueInvalid() {
        assertEquals("setprop 'debug.dpis.wechat.targetfield.c5fe9776' '0'; "
                        + "setprop 'persist.debug.dpis.wechat.targetfield.c5fe9776' '0'",
                WechatTargetFieldPropertySyncer.buildTargetCommandForTest(
                        "com.tencent.mm", 199));
    }
}
