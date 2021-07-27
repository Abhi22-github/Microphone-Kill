package com.test.microphonekill.microphoneguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class GuardServiceRestartBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(GuardServiceRestartBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
        if (Build.VERSION.SDK_INT > 26) {
            context.startForegroundService(new Intent(context, GuardService.class));
        } else {
            context.startService(new Intent(context, GuardService.class));
        }
    }
}
