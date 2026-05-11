package com.dpis.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class DpisPackageLifecycleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !isSupportedAction(intent.getAction())) {
            return;
        }
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context);
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            store = ConfigStoreFactory.createForModuleApp(context);
        }
        ViewportPropertySyncer.syncConfiguredTargetsAsync(store);
        CompatFontPropertySyncer.syncConfiguredTargetsAsync(store);
        HyperOsNativeFontPropertySyncer.syncConfiguredFontTargetsAsync(store);
    }

    private static boolean isSupportedAction(String action) {
        return Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action);
    }
}
