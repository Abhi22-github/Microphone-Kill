package com.test.microphonekill.microphoneguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.test.test.R;

public class NotificationService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Intent intent;
    private PendingIntent pendingIntent;
    private Notification.Builder builder;
    private int REQUEST_CODE;
    private NotificationManager notificationManger;
    private SharedPreferences settings;
    private boolean isProtectionEnabled;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.settings = getApplicationContext().getSharedPreferences(MainActivity.SETTINGS, 0);
        builder.setContentTitle("Microphone is Blocked");
        builder.setNumber(2844);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.mute);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        // === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManger.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        startForeground(12, builder.build());
        //notificationManger.notify(22, builder.build());
    }

    public void reloadConfig() {
        this.isProtectionEnabled = this.settings.getBoolean("protection_enabled", false);
        // this.isUnmutedOnScreenOn = this.settings.getBoolean("unmute_on_screen_on", false);
        if (this.isProtectionEnabled) {
            updateNotification(isProtectionEnabled);
        } else {
            updateNotification(isProtectionEnabled);
        }
    }

    private void updateNotification(boolean isProtectionEnabled) {
    }

    private void generateNotification(String text) {

        builder.setContentTitle("Microphone is Blocked");
        builder.setNumber(2844);
        builder.setContentIntent(pendingIntent);
        builder.setSmallIcon(R.drawable.mute);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        // === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManger.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        startForeground(12, builder.build());
        //notificationManger.notify(22, builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
