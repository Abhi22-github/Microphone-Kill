package com.test.microphonekill.microphoneguard;

import android.util.Log;

public class CallReceiver extends PhoneCallReceiver {
    private static final String TAG = "CallReceiver";

    protected void onIncomingCallStarted() {
        if (ActivityHelper.callService != null) {
            ActivityHelper.callService.stopGuardService();
            Log.d(TAG, "onIncomingCallStarted: ");
        }
    }


    protected void onOutgoingCallStarted() {
        if (ActivityHelper.callService != null) {
            ActivityHelper.callService.stopGuardService();
            Log.d(TAG, "onOutgoingCallStarted: ");
        }
    }


    protected void onIncomingCallEnded() {
        if (ActivityHelper.callService != null) {
            ActivityHelper.callService.startGuardService();
            Log.d(TAG, "onIncomingCallEnded: ");
        }
    }


    protected void onOutgoingCallEnded() {
        if (ActivityHelper.callService != null) {
            ActivityHelper.callService.startGuardService();
            Log.d(TAG, "onOutgoingCallEnded: ");
        }
    }


    protected void onMissedCall() {
        if (ActivityHelper.callService != null) {
            ActivityHelper.callService.startGuardService();
            Log.d(TAG, "onMissedCall: ");
        }
    }

}
