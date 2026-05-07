package com.dpis.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class DpisPackageLifecycleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null
                || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(context);
        DpiConfigStore store = DpisApplication.getConfigStore();
        if (store == null) {
            store = ConfigStoreFactory.createForModuleApp(context);
        }
        HyperOsNativeFontPropertySyncer.syncConfiguredFontTargetsAsync(store);
    }
}
