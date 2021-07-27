package com.test.microphonekill.microphoneguard;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallService extends Service {
    private static final String TAG = "CallService";
    private PhoneStateListener callListener;
    private TelephonyManager telephonyManager;

    public void onCreate() {
        ActivityHelper.callService = this;
        this.callListener = new CallReceiver();
        this.telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        this.telephonyManager.listen(this.callListener, 32);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void stopGuardService() {
        if (ActivityHelper.guardService != null) {
            //ActivityHelper.guardService.stopGuardService();
            ActivityHelper.guardService.muteUnMuteMicrophone(true);
           // Toast.makeText(this, "Microphone Enable", Toast.LENGTH_SHORT).show();
        }
    }

    public void startGuardService() {
        //if (Build.VERSION.SDK_INT >= 26) {
        //     Intent i = new Intent(this, GuardService.class);
        //      i.putExtra("flag", true);
        //      startForegroundService(i);
        //  } else {
        ActivityHelper.guardService.muteUnMuteMicrophone(false);
        // Intent i = new Intent(this, GuardService.class);
        // i.putExtra("flag", true);
        // startService(i);
        //  }
        //Toast.makeText(this, "Microphone Disable", Toast.LENGTH_SHORT).show();
    }

    public void onDestroy() {
        this.telephonyManager.listen(this.callListener, 0);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }


}
