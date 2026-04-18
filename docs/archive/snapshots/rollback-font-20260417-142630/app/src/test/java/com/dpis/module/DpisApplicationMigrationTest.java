package com.dpis.module;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DpisApplicationMigrationTest {
    @Test
    public void doesNotOverwriteRemoteTogglesWhenLocalMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setSystemServerHooksEnabled(false));
        assertTrue(remote.setSystemServerSafeModeEnabled(false));
        assertTrue(remote.setGlobalLogEnabled(true));

        invokeMigrate(local, remote);

        assertFalse(remote.isSystemServerHooksEnabled());
        assertFalse(remote.isSystemServerSafeModeEnabled());
        assertTrue(remote.isGlobalLogEnabled());
    }

    @Test
    public void seedsRemoteTogglesWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setSystemServerHooksEnabled(false));
        assertTrue(local.setSystemServerSafeModeEnabled(false));
        assertTrue(local.setGlobalLogEnabled(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertFalse(remote.isSystemServerHooksEnabled());
        assertFalse(remote.isSystemServerSafeModeEnabled());
        assertTrue(remote.isGlobalLogEnabled());
    }

    private static void invokeMigrate(DpiConfigStore from, DpiConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "migrateConfig", DpiConfigStore.class, DpiConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }
}

