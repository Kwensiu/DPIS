package com.dpis.module;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import io.github.libxposed.api.XposedInterface;

final class FontDebugStatsTransport {
    private static final String MODULE_CLASS_PACKAGE = "com.dpis.module";
    private static volatile SharedPreferences remotePreferences;

    private FontDebugStatsTransport() {
    }

    static void initialize(XposedInterface xposed) {
        if (xposed == null) {
            return;
        }
        try {
            remotePreferences = xposed.getRemotePreferences(DpiConfigStore.GROUP);
        } catch (Throwable ignored) {
            remotePreferences = null;
        }
    }

    static void sendUpdate(Context context, Bundle extras) {
        if (extras == null || extras.isEmpty()) {
            return;
        }
        SharedPreferences preferences = remotePreferences;
        if (preferences != null) {
            try {
                FontDebugStatsUpdateWriter.applyExtras(preferences, extras);
            } catch (Throwable throwable) {
                DpisLog.e("font debug remote preferences update failed", throwable);
            }
        }
        if (context == null) {
            return;
        }
        try {
            context.getContentResolver().call(buildUri(),
                    FontDebugStatsProvider.METHOD_APPLY_UPDATE, null, extras);
        } catch (Throwable throwable) {
            DpisLog.e("font debug provider update failed", throwable);
        }
        try {
            Intent intent = new Intent(FontDebugStatsStore.ACTION_STATS_UPDATE);
            intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID,
                    MODULE_CLASS_PACKAGE + ".FontDebugStatsReceiver"));
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtras(extras);
            context.sendBroadcast(intent);
        } catch (Throwable throwable) {
            DpisLog.e("font debug explicit broadcast update failed", throwable);
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID,
                    MODULE_CLASS_PACKAGE + ".FontDebugStatsIngestService"));
            intent.putExtras(extras);
            context.startService(intent);
        } catch (Throwable throwable) {
            DpisLog.e("font debug ingest service update failed", throwable);
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID,
                    MODULE_CLASS_PACKAGE + ".FontDebugStatsIngestActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtras(extras);
            context.startActivity(intent);
        } catch (Throwable throwable) {
            DpisLog.e("font debug ingest activity update failed", throwable);
        }
        FontDebugStatsFileBridge.write(extras);
    }

    private static Uri buildUri() {
        return new Uri.Builder()
                .scheme("content")
                .authority(BuildConfig.APPLICATION_ID + FontDebugStatsProvider.AUTHORITY_SUFFIX)
                .build();
    }
}
