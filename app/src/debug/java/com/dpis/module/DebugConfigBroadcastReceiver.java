package com.dpis.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class DebugConfigBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION_SET_PACKAGE_CONFIG =
            DebugConfigApplier.ACTION_SET_PACKAGE_CONFIG;

    @Override
    public void onReceive(Context context, Intent intent) {
        DebugConfigApplier.apply(context, intent, true);
    }
}
