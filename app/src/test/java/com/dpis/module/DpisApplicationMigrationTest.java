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

    @Test
    public void seedsRemoteTogglesWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setSystemServerHooksEnabled(false));
        assertTrue(local.setSystemServerSafeModeEnabled(false));
        assertTrue(local.setGlobalLogEnabled(true));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit()
                .putBoolean(DpiConfigStore.KEY_SYSTEM_SERVER_HOOKS_ENABLED, true)
                .putBoolean(DpiConfigStore.KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, true)
                .putBoolean(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED, false)
                .commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertFalse(remoteOnly.isSystemServerHooksEnabled());
        assertFalse(remoteOnly.isSystemServerSafeModeEnabled());
        assertTrue(remoteOnly.isGlobalLogEnabled());
    }

    @Test
    public void seedsRemoteFontScaleWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(remote.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertTrue(remote.getTargetViewportWidthDp("com.max.xiaoheihe") == null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") == 150);
    }

    @Test
    public void doesNotOverwriteRemoteFontScaleWhenAlreadyPresent() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setTargetFontScalePercent("com.max.xiaoheihe", 130));

        invokeMigrate(local, remote);

        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") == 130);
    }

    @Test
    public void seedsRemoteFontModeWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));
        assertTrue(local.setTargetFontApplyMode("com.max.xiaoheihe", FontApplyMode.FIELD_REWRITE));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(FontApplyMode.FIELD_REWRITE.equals(
                remote.getTargetFontApplyMode("com.max.xiaoheihe")));
    }

    @Test
    public void seedsRemoteViewportWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetViewportWidthDp("com.max.xiaoheihe", 360));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit().putInt("viewport.com.max.xiaoheihe.width_dp", 300).commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertTrue(remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe") != null);
        assertTrue(remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe") == 360);
    }

    @Test
    public void seedsRemoteFontScaleWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit().putInt("font.com.max.xiaoheihe.scale_percent", 120).commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertTrue(remoteOnly.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remoteOnly.getTargetFontScalePercent("com.max.xiaoheihe") == 150);
    }

    @Test
    public void doesNotOverwriteRemoteLauncherIconHiddenWhenLocalMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setLauncherIconHidden(true));

        invokeMigrate(local, remote);

        assertTrue(remote.isLauncherIconHidden());
    }

    @Test
    public void seedsRemoteLauncherIconHiddenWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setLauncherIconHidden(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(remote.isLauncherIconHidden());
    }

    private static void invokeMigrate(DpiConfigStore from, DpiConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "migrateConfig", DpiConfigStore.class, DpiConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }
}
