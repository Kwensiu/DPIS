package com.dpis.module;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unused")
public final class LegacyModuleMainHook implements IXposedHookLoadPackage {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!LegacySystemServerGate.shouldInstall(lpparam.packageName, lpparam.processName)) {
            return;
        }
        if (!INSTALLED.compareAndSet(false, true)) {
            DpisLog.i("legacy smoke skipped: already-installed, marker="
                    + LegacySmokeProbe.marker()
                    + ", process=" + lpparam.processName);
            return;
        }
        String message = LegacySmokeProbe.loadMessage(lpparam.packageName, lpparam.processName);
        DpisLog.i(message);
    }
}
