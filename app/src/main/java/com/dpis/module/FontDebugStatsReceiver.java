package com.dpis.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class FontDebugStatsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        if (!FontDebugStatsStore.ACTION_STATS_UPDATE.equals(intent.getAction())) {
            return;
        }
        SharedPreferences preferences = FontDebugStatsStore.getPreferences(context);
        preferences.edit()
                .putString(FontDebugStatsStore.KEY_CHAIN_5S,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_5S))
                .putString(FontDebugStatsStore.KEY_CHAIN_30S,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_30S))
                .putString(FontDebugStatsStore.KEY_CHAIN_ALL,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_ALL))
                .putString(FontDebugStatsStore.KEY_CHAIN_VIEW_5S,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S))
                .putString(FontDebugStatsStore.KEY_CHAIN_VIEW_30S,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S))
                .putString(FontDebugStatsStore.KEY_CHAIN_VIEW_ALL,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL))
                .putInt(FontDebugStatsStore.KEY_EVENT_TOTAL,
                        intent.getIntExtra(FontDebugStatsStore.EXTRA_EVENT_TOTAL, 0))
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT,
                        intent.getLongExtra(FontDebugStatsStore.EXTRA_UPDATED_AT, 0L))
                .putString(FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S,
                        intent.getStringExtra(FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S))
                .apply();
    }
}
