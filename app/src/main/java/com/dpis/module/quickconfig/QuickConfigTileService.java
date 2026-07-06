package com.dpis.module.quickconfig;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import com.dpis.module.QuickConfigActivity;
import com.dpis.module.applist.ForegroundPackageResolver;

public class QuickConfigTileService extends TileService {
    @Override
    public void onClick() {
        super.onClick();
        Intent intent = QuickConfigActivity.createIntent(
                this,
                ForegroundPackageResolver.resolve(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pendingIntent);
        } else {
            startActivityAndCollapseCompat(intent);
        }
    }

    @SuppressWarnings("deprecation")
    private void startActivityAndCollapseCompat(Intent intent) {
        startActivityAndCollapse(intent);
    }
}
