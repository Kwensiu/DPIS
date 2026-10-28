package com.dpis.module.runtime.lifecycle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.dpis.module.DpisApplication;
import com.dpis.module.config.DpisConfigStore;
import com.dpis.module.diagnostics.DpisLog;
import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.runtime.ConfigStoreFactory;
import com.dpis.module.runtime.delivery.RuntimePropertyRecoveryCoordinator;
import com.dpis.module.runtime.hyperos.HyperOsNativeProxyAssetExporter;

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
        if (!DpisApplication.isOnboardingCompleted()) {
            return;
        }
        final DpisConfigStore recoveryStore = store;
        RootAccessProbe.refreshAsync(result -> {
            if (result != null && result.status == RootAccessProbe.Status.AVAILABLE) {
                // BOOT_COMPLETED and MY_PACKAGE_REPLACED are only best-effort triggers on some ROMs.
                // The coordinator itself is idempotent and can be replayed safely from other entrypoints.
                RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(recoveryStore);
            }
        });
    }

    private static boolean isSupportedAction(String action) {
        return Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action);
    }
}
