package com.test.microphonekill.microphoneguard;

import android.telephony.PhoneStateListener;

public abstract class PhoneCallReceiver extends PhoneStateListener {
    private static final String TAG = "PhoneCallReceiver";
    private static boolean isIncoming;

    public void onCallStateChanged(int state, String incomingNumber) {
        if (ActivityHelper.LastCallState != state) {
            switch (state) {
                case 0:
                    if (ActivityHelper.LastCallState != 1) {
                        if (!isIncoming) {
                            onOutgoingCallEnded();
                            break;
                        } else {
                            onIncomingCallEnded();
                            break;
                        }
                    }
                    onMissedCall();
                    break;
                case 1:
                    isIncoming = true;
                    onIncomingCallStarted();
                    break;
                case 2:
                    if (ActivityHelper.LastCallState != 1) {
                        isIncoming = false;
                        onOutgoingCallStarted();
                        break;
                    }
                    break;
            }
            ActivityHelper.LastCallState = state;
        }
    }


    protected void onIncomingCallStarted() {
    }


    protected void onOutgoingCallStarted() {
    }


    protected void onIncomingCallEnded() {
    }


    protected void onOutgoingCallEnded() {
    }


    protected void onMissedCall() {
    }
}
