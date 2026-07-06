package com.dpis.module.runtime;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisApplication;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.DpisLog;
import com.dpis.module.fonts.HyperOsNativeProxyAssetExporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class DpisPackageLifecycleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !isSupportedAction(intent.getAction())) {
            return;
        }
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context, DpisLog::e);
        DpisConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            store = ConfigStoreFactory.createPackageLifecycleConfigStore(context);
        }
        // BOOT_COMPLETED and MY_PACKAGE_REPLACED are only best-effort triggers on some ROMs.
        // The coordinator itself is idempotent and can be replayed safely from other entrypoints.
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store);
    }

    private static boolean isSupportedAction(String action) {
        return Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action);
    }
}
