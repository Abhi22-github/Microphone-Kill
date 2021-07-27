package com.test.microphonekill.microphoneguard;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.messages.Strategy;
import com.test.microphonekill.lib.whitelist.WhitelistItemAdapter;
import com.test.test.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;

public class GuardService extends Service {
    private static final String TAG = "GuardService";

    public static final int AID_APP = 10000;
    public static final int AID_USER = 100000;
    private static final int BUFFER_LENGTH = 10;
    private static final int WATCHDOG_TIMING = 15000;
    public static boolean isRunning;
    private static final int RECORDER_AUDIO_ENCODING = 2;
    private static final int RECORDER_CHANNELS = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int stealthTries = 750;
    private Runnable runnable;
    private AudioManager newAudioManager;
    private ScheduledExecutorService service;
    private ActivityManager activityManager;
    private boolean test;
    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private int cycleCount;
    private Handler handler;
    private boolean isPowerSavingActive;
    private boolean isProtectionEnabled;
    private boolean isUnmutedOnScreenOn;
    private boolean microphoneBlocked;
    private int minBufferSize;
    private int minSampleRate;
    private boolean mutedToast;
    private PowerManager powerManager;
    private BroadcastReceiver powerReceiver;
    private SharedPreferences settings;
    private Timer watchdogTimer;
    private SharedPreferences whitelist;
    private Intent intent;
    private PendingIntent pendingIntent;
    private Notification.Builder builder;
    private int REQUEST_CODE;
    private NotificationManager notificationManger;
    int channelConfig;
    private int NOTIFICATION_ID = 23;
    String channelId = "id";
    private AlarmManager alarmManager;
    //private SharedPreferences whitelist;

    public void onCreate() {
        ////CrashReporter.logException(new RuntimeException("onCreate : started"));
        ActivityHelper.guardService = this;
        this.audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        this.powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        this.activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        this.settings = getApplicationContext().getSharedPreferences(MainActivity.SETTINGS, 0);
        this.whitelist = getApplicationContext().getSharedPreferences(WhitelistItemAdapter.WHITELIST, 0);
        IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        try {
            this.powerReceiver = new PowerReceiver();
            registerReceiver(this.powerReceiver, filter);
        } catch (Exception e) {
            ////CrashReporter.logException(new RuntimeException("onCreate : " + e.getMessage()));
        }
        REQUEST_CODE = 2;
        this.handler = new Handler();
        intent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent, 0);
        builder = new Notification.Builder(this);
        notificationManger =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        PackageManager pm = this.getPackageManager();
        int hasPerm = pm.checkPermission(
                Manifest.permission.RECORD_AUDIO,
                this.getPackageName());
        if (hasPerm == PackageManager.PERMISSION_GRANTED) {

        }

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        ////CrashReporter.logException(new RuntimeException("onStartCommand :"));
        reloadConfig();
        Log.d(TAG, "onStartCommand: ");
        return START_STICKY;
    }

    public void reloadConfig() {
        this.minSampleRate = 11025;
        while (this.minBufferSize <= 0 && this.minSampleRate < 44100) {
            this.minBufferSize = AudioRecord.getMinBufferSize(this.minSampleRate, 16, 2);
            if (this.minBufferSize <= 0) {
                this.minSampleRate *= 2;
            }
        }
        this.minBufferSize = (this.minSampleRate * 2) * 10;
        this.isProtectionEnabled = this.settings.getBoolean("protection_enabled", false);
        this.isUnmutedOnScreenOn = this.settings.getBoolean("unmute_on_screen_on", false);
        if (this.isProtectionEnabled) {
            ////CrashReporter.logException(new RuntimeException("reloadConfig : starting watchdog"));
            startWatchdogTask();
        } else {
            ////CrashReporter.logException(new RuntimeException("reloadConfig :stopping guard service"));
            stopGuardService();
        }
    }

    public void stopGuardService() {
        stopWatchdogTask();
        try {
            unregisterReceiver(this.powerReceiver);
        } catch (Exception e) {
            ////CrashReporter.logException(new RuntimeException("stopGuardService :error while stopping guard service"));
        }
        ActivityHelper.guardService = null;
        ////CrashReporter.logException(new RuntimeException("stopGuardService : stopping self"));
        stopSelf();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        ////CrashReporter.logException(new RuntimeException("onUnbind :"));
        return super.onUnbind(intent);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        ////CrashReporter.logException(new RuntimeException("unbindService :"));
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
        // Intent broadcastIntent = new Intent(this, GuardServiceRestartBroadcastReceiver.class);
        // sendBroadcast(broadcastIntent);
        ////CrashReporter.logException(new RuntimeException("onDestroy : service destroy"));
        super.onDestroy();
    }

    public void startWatchdogTask() {
        if (this.watchdogTimer != null) {
            this.watchdogTimer.cancel();
        }
        if (this.isProtectionEnabled) {
            Log.d("MicrophoneGuard", "Watchdog enabled.");
            ////CrashReporter.logException(new RuntimeException("startWatchdogTask : Watchdog enabled"));
            TimerTask watchdogTimerTask = new TimerTask() {
                public void run() {
                    ////CrashReporter.logException(new RuntimeException("startWatchdogTask : timer is running"));
                    GuardService.this.microphoneCheck();

                }
            };
            this.watchdogTimer = new Timer();
            this.watchdogTimer.schedule(watchdogTimerTask, 0, 15000);
/*
            long ct = System.currentTimeMillis(); //get current time
            AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(getApplicationContext(), MyAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, i, 0);

            mgr.set(AlarmManager.RTC_WAKEUP, ct + 60000, pi); //60 seconds is 60000 mill*/
        }
    }

    public void stopWatchdogTask() {
        if (this.watchdogTimer != null) {
            this.watchdogTimer.cancel();
        }
        unblockMicrophone();
        showStatus(true);

        Log.d("MicrophoneGuard", "Watchdog disabled.");
    }

    private void showStatus(final boolean activated) {
        this.handler.post(new Runnable() {
            public void run() {
                GuardService.this.mutedToast = GuardService.this.settings.getBoolean("muted_toast", false);
                if (activated) {
                    if (GuardService.this.mutedToast) {
                        Toast.makeText(GuardService.this.getApplicationContext(), "Microphone is ON", Toast.LENGTH_SHORT).show();

                        GuardService.this.mutedToast = false;
                    }
                } else if (!GuardService.this.mutedToast) {
                    Toast.makeText(GuardService.this.getApplicationContext(), "Microphone is OFF", Toast.LENGTH_SHORT).show();
                    GuardService.this.mutedToast = true;
                    if (Build.VERSION.SDK_INT > 26) {
                        startNotificationAboveO("Microphone is Blocked", "Tap to Unblock", pendingIntent);
                    } else {
                        startNotificationBelowO("Microphone is Blocked", "Tap to Unblock", pendingIntent);
                    }
                }
                Editor editor = GuardService.this.settings.edit();
                editor.putBoolean("muted_toast", GuardService.this.mutedToast);
                editor.commit();
            }
        });
    }


    private void showWarning(Exception e) {
        this.handler.post(new Runnable() {
            public void run() {
                ////CrashReporter.logException(new RuntimeException("showWarning : protection value changes to false"));
                Editor editor = GuardService.this.settings.edit();
                editor.putBoolean("protection_enabled", false);
                editor.apply();
                Log.d(TAG, "run: failed " + e.getMessage());
                Toast.makeText(GuardService.this.getApplicationContext(), "Microphone blocking failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void microphoneCheck() {
        ////CrashReporter.logException(new RuntimeException("microphoneCheck : 0 :timer : unblockMicrophone"));
        if (this.isPowerSavingActive) {
            if (this.cycleCount % 10 == 0) {
                ////CrashReporter.logException(new RuntimeException("microphoneCheck : 1 :timer : unblockMicrophone"));
                unblockMicrophone();

            } else {
                ////CrashReporter.logException(new RuntimeException("microphoneCheck : 1 :timer : blockMicrophone"));
                blockMicrophone();

            }
            this.cycleCount++;
        } else if (this.isUnmutedOnScreenOn && this.powerManager.isScreenOn()) {
            ////CrashReporter.logException(new RuntimeException("microphoneCheck : 2 :timer : unblockMicrophone"));
            unblockMicrophone();

            showStatus(true);

        } else if (isActiveAppWhitelisted()) {
            ////CrashReporter.logException(new RuntimeException("microphoneCheck : 3 :timer : unblockMicrophone"));
            unblockMicrophone();

            showStatus(true);

        } else if (audioManager.isMicrophoneMute()) {
            ////CrashReporter.logException(new RuntimeException("microphoneCheck : 4 :timer : blockMicrophone"));
        } else if (!audioManager.isMicrophoneMute()) {
            blockMicrophone();
        } else {
            ////CrashReporter.logException(new RuntimeException("microphoneCheck : 5 :timer : blockMicrophone"));
            blockMicrophone();
            showStatus(false);
        }
        ////CrashReporter.logException(new RuntimeException("microphoneCheck : 6 :timer : blockMicrophone"));
        Log.d("MicrophoneGuard", "Microphone muted: " + this.microphoneBlocked);
    }

    private void blockMicrophone() {
        PackageManager pm = this.getPackageManager();
        int hasPermAudio = pm.checkPermission(
                Manifest.permission.RECORD_AUDIO,
                this.getPackageName());
        int hasPermPhone = pm.checkPermission(Manifest.permission.READ_PHONE_STATE, this.getPackageName());
        if (hasPermAudio == PackageManager.PERMISSION_GRANTED && hasPermPhone == PackageManager.PERMISSION_GRANTED) {
            if (!this.microphoneBlocked) {
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    this.audioManager.setMicrophoneMute(true);
                    try {
                        audioRecord.release();
                    } catch (Exception e) {
                        ////CrashReporter.logException(new RuntimeException("blockMicrophone : " + e.getMessage()));
                    }
                    // int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
                    this.audioRecord = new AudioRecord(1, this.minSampleRate, 16, RECORDER_AUDIO_ENCODING, this.minBufferSize);
                    this.audioRecord.startRecording();
                    this.audioManager.setMicrophoneMute(true);
                    this.microphoneBlocked = true;

                    if (Build.VERSION.SDK_INT > 26) {
                        startNotificationAboveO("Microphone is Blocked", "Tap to Unblock", pendingIntent);
                        Log.d(TAG, "blockMicrophone: showing notification");
                    } else {
                        startNotificationBelowO("Microphone is Blocked", "Tap to Unblock", pendingIntent);
                    }
                    Editor editor = GuardService.this.settings.edit();
                    editor.putBoolean("protection_enabled", true);
                    editor.apply();

                } catch (Exception e) {
                    Log.d("MicrophoneGuard", "Microphone blocking failed!" + e.getMessage().toString());
                    if (Build.VERSION.SDK_INT > 26) {
                        startNotificationAboveO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                        Log.d(TAG, "blockMicrophone: showing notification");
                    } else {
                        startNotificationBelowO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                    }
                    ////CrashReporter.logException(e);
                    ////CrashReporter.logException(new RuntimeException("blockMicrophone : " + e.getMessage()));
                    showWarning(e);
                    unblockMicrophone();
                    if (Build.VERSION.SDK_INT > 26) {
                        startForegroundService(new Intent(this, GuardService.class));

                    } else {
                        startService(new Intent(this, GuardService.class));

                    }
                }
            } else {
                ////CrashReporter.logException(new RuntimeException("blockMicrophone : blocked"));
                Editor editor = GuardService.this.settings.edit();
                editor.putBoolean("protection_enabled", true);
                editor.apply();
            }
        }

    }


    private void unblockMicrophone() {
        try {
            if (this.microphoneBlocked) {
                this.microphoneBlocked = false;
                this.audioRecord.stop();
                this.audioRecord.release();
                this.audioRecord = null;
                if (Build.VERSION.SDK_INT > 26) {
                    startNotificationAboveO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                    Log.d(TAG, "blockMicrophone: showing notification");
                } else {
                    startNotificationBelowO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                }
            }
            if (this.isPowerSavingActive) {
                this.audioManager.setMicrophoneMute(true);
                if (Build.VERSION.SDK_INT > 26) {
                    startNotificationAboveO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                    Log.d(TAG, "blockMicrophone: showing notification");
                } else {
                    startNotificationBelowO("Microphone is UnBlock", "Tap to Block", pendingIntent);
                }
                return;
            }
            this.audioManager.setMicrophoneMute(false);


        } catch (Exception e) {
            Log.d("MicrophoneGuard", "Unblock microphone failed!");
            if (this.isPowerSavingActive) {
                this.audioManager.setMicrophoneMute(true);
            }
            this.audioManager.setMicrophoneMute(false);
            ////CrashReporter.logException(new RuntimeException("unblockMicrophone : " + e.getMessage()));

        } catch (Throwable th) {
            if (this.isPowerSavingActive) {
                this.audioManager.setMicrophoneMute(true);
            } else {
                this.audioManager.setMicrophoneMute(false);
            }
            ////CrashReporter.logException(new RuntimeException("unblockMicrophone : " + th.getMessage()));
        }
        if (Build.VERSION.SDK_INT > 26) {
            startNotificationAboveO("Microphone is UnBlock", "Tap to Block", pendingIntent);
            Log.d(TAG, "blockMicrophone: showing unblock notification");
        } else {
            startNotificationBelowO("Microphone is UnBlock", "Tap to Block", pendingIntent);
            Log.d(TAG, "blockMicrophone: showing unblock notification");
        }
    }

    public void setPowerSavingActive(boolean isActive) {
        this.isPowerSavingActive = isActive;
        if (!this.isPowerSavingActive) {
            blockMicrophone();
        }
        this.cycleCount = 1;
    }

    private boolean isActiveAppWhitelisted() {
        if (this.whitelist.getAll().size() > 0) {
            String activePackage;
            if (Build.VERSION.SDK_INT >= 20) {
                activePackage = getForegroundApp();
            } else {
                activePackage = getActivePackagesCompat();
            }
            if (this.whitelist.contains(activePackage)) {
                return true;
            }
        }
        return false;
    }

    private String getActivePackagesCompat() {
        return ((RunningTaskInfo) this.activityManager.getRunningTasks(1).get(0)).topActivity.getPackageName();
    }

    public String getForegroundApp() {
        File[] files = new File("/proc").listFiles();
        int lowestOomScore = Strategy.TTL_SECONDS_INFINITE;
        String foregroundProcess = "";
        for (File file : files) {
            if (file.isDirectory()) {
                int pid;
                try {
                    try {
                        String[] lines = read(String.format("/proc/%d/cgroup", new Object[]{Integer.valueOf(Integer.parseInt(file.getName()))})).split("\n");
                        if (lines.length == 2) {
                            String cpuSubsystem = lines[0];
                            String cpuaccctSubsystem = lines[1];
                            pid = Integer.parseInt(file.getName());
                            if (cpuaccctSubsystem.endsWith(Integer.toString(pid)) && !cpuSubsystem.endsWith("bg_non_interactive")) {
                                String cmdline = read(String.format("/proc/%d/cmdline", new Object[]{Integer.valueOf(pid)}));
                                if (!cmdline.contains("com.android.systemui")) {
                                    int uid = Integer.parseInt(cpuaccctSubsystem.split(":")[2].split("/")[1].replace("uid_", ""));
                                    if (uid < 1000 || uid > 1038) {
                                        int appId = uid - 10000;
                                        int userId = 0;
                                        while (appId > 100000) {
                                            appId -= AID_USER;
                                            userId++;
                                        }
                                        if (appId >= 0) {
                                            File oomScoreAdj = new File(String.format("/proc/%d/oom_score_adj", new Object[]{Integer.valueOf(pid)}));
                                            if (!oomScoreAdj.canRead() || Integer.parseInt(read(oomScoreAdj.getAbsolutePath())) == 0) {
                                                int oomscore = Integer.parseInt(read(String.format("/proc/%d/oom_score", new Object[]{Integer.valueOf(pid)})));
                                                if (oomscore < lowestOomScore) {
                                                    lowestOomScore = oomscore;
                                                    foregroundProcess = cmdline;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (NumberFormatException e2) {
                }
            }
        }
        return foregroundProcess.trim();
    }

    private static String read(String path) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        output.append(reader.readLine());
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            output.append(10).append(line);
        }
        reader.close();
        return output.toString();
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return null;
    }


    public void muteUnMuteMicrophone(boolean i) {
        String str = "Mic is Blocked";
        String str1 = "Tap to unblock";
        String str2 = "Mic is UnBlocked";
        String str3 = null;
        String str4 = null;
        this.isProtectionEnabled = this.settings.getBoolean("protection_enabled", false);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (i) {
            Log.d(TAG, "muteUnMuteMicrophone: mic is enable");
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setMicrophoneMute(false);
            str3 = str2;
            str4 = str1;
        } else {
            Log.d(TAG, "muteUnMuteMicrophone: mic is enable");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.setMicrophoneMute(true);
                }
            }, 3000);
            if (audioManager.isMicrophoneMute()) {

            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        audioManager.setMode(AudioManager.MODE_NORMAL);
                        audioManager.setMicrophoneMute(true);
                    }
                }, 3000);

            }
            str3 = str;
            str4 = str1;
        }


        if (Build.VERSION.SDK_INT > 26) {
            startNotificationAboveO(str3, str4, pendingIntent);
        } else {
            startNotificationBelowO(str3, str4, pendingIntent);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startNotificationAboveO(String str3, String str4, PendingIntent pendingIntent) {
        Context applicationContext = getApplicationContext();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(channelId, "Block_Michrophone_Channel", NotificationManager.IMPORTANCE_HIGH));
        startForeground(NOTIFICATION_ID, new Notification.Builder(applicationContext, channelId).setContentTitle(str3).
                setContentText(str4).
                setSmallIcon(R.drawable.mute).
                setContentIntent(pendingIntent).
                setOngoing(true).
                setColor(ContextCompat.getColor(this, R.color.green)).
                setPriority(Notification.PRIORITY_DEFAULT).build());

    }

    private void startNotificationBelowO(String str3, String str4, PendingIntent pendingIntent) {
        startForeground(NOTIFICATION_ID, new Notification.Builder(getApplicationContext()).
                setContentTitle(str3).
                setContentText(str4).
                setOngoing(true).
                setSmallIcon(R.drawable.mute).
                setColor(ContextCompat.getColor(this, R.color.green)).
                setContentIntent(pendingIntent).
                setPriority(Notification.PRIORITY_DEFAULT).build());
    }


}
