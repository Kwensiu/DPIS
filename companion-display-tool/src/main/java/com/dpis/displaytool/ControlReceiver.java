package com.dpis.displaytool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activityIntent.putExtra(CompanionContract.EXTRA_FROM_CONTROL_RECEIVER, true);
        activityIntent.putExtra(
                CompanionContract.EXTRA_ACTION,
                intent.getStringExtra(CompanionContract.EXTRA_ACTION)
        );
        activityIntent.putExtra(
                CompanionContract.EXTRA_SCENE,
                intent.getStringExtra(CompanionContract.EXTRA_SCENE)
        );
        activityIntent.putExtra(
                CompanionContract.EXTRA_VARIANT,
                intent.getStringExtra(CompanionContract.EXTRA_VARIANT)
        );
        activityIntent.putExtra(
                CompanionContract.EXTRA_TRIGGER,
                intent.getStringExtra(CompanionContract.EXTRA_TRIGGER)
        );
        context.startActivity(activityIntent);
    }
}
