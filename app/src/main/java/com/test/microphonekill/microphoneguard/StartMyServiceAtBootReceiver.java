package com.test.microphonekill.microphoneguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class StartMyServiceAtBootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(new Intent(context, CallService.class));
                context.startForegroundService(new Intent(context, GuardService.class));
            } else {
                context.startService(new Intent(context, CallService.class));
                context.startService(new Intent(context, GuardService.class));
            }

        }
    }
}