package com.test.microphonekill.microphoneguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GuardService guardService = new GuardService();
        guardService.microphoneCheck();
    }
}
