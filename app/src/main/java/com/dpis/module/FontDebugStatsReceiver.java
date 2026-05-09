package com.dpis.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FontDebugStatsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        if (!FontDebugStatsStore.ACTION_STATS_UPDATE.equals(intent.getAction())) {
            return;
        }
        FontDebugStatsUpdateWriter.applyExtras(context, intent.getExtras());
    }
}
