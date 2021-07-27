package com.test.microphonekill.microphoneguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PowerReceiver extends BroadcastReceiver {
    public boolean isPowerConnected(Context context) {
        int plugged = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("plugged", -1);
        if (plugged == 1 || plugged == 2) {
            return true;
        }
        return false;
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
            if (ActivityHelper.guardService != null) {
                ActivityHelper.guardService.setPowerSavingActive(true);
            }
        } else if (intent.getAction().equals("android.intent.action.SCREEN_ON") && ActivityHelper.guardService != null) {
            ActivityHelper.guardService.setPowerSavingActive(false);
        }
    }
}
