/**
 * Developer: Abhishek Satpute
 * Created: 18 Mar 2021
 * Last Modified: 18 Mar 2021
 */

package com.test.microphonekill.microphoneguard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.test.test.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    //widgets
    private TextView microphoneStatus;
    private CheckBox checkBox;
    private CircleImageView darkModeToggle;

    //vars
    public static final String SETTINGS = "microphoneguard.settings";
    private boolean isMicBlocked;
    private boolean isUnmutedOnScreenOn;
    private boolean PERMISSION_GRANTED;
    private SharedPreferences settings;
    private static final int REQUEST_PERMISSION = 410;
    public static final int ALL_PERMISSION_REQUEST_CODE = 22;
    private SharedPreferences sharedPreferences;
    private Editor editor;
    private Boolean darkModeStatus;
    private Boolean darkModeTempStatus = false;


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     /*   sharedPreferences = getSharedPreferences("DarkModeStatus", MODE_PRIVATE);
        darkModeStatus = sharedPreferences.getBoolean("status", false);
        if (darkModeStatus) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }*/


        ActivityHelper.mainActivity = this;
        this.settings = getApplicationContext().getSharedPreferences(SETTINGS, 0);
        reloadConfig();
        setContentView(R.layout.activity_main);
//        editor = sharedPreferences.edit();
        initFields();
        checkPermissionsExplicitly();
        if (PERMISSION_GRANTED) {
            setStatus(isMicBlocked);
            restartGuardService();
        } else {
            Toast.makeText(this, "Please grant all permissions", Toast.LENGTH_SHORT).show();
        }


        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                checkPermissionsExplicitly();
                if (PERMISSION_GRANTED) {
                    enableProtection();
                }

            }
        });


        darkModeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    Log.d(TAG, "onClick:tru ");
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    Log.d(TAG, "onClick: fal");
                }
            }
        });

       /* darkModeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                //darkModeTempStatus = sharedPreferences.getBoolean("status", false);
                if (darkModeTempStatus) {
                    /*bring it in day mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    darkModeToggle.setImageResource(R.drawable.ic_day);
                    editor.putBoolean("status",false);
                    editor.apply();
                } else {

                   /* AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    darkModeToggle.setImageResource(R.drawable.ic_night);
                    editor.putBoolean("status",true);
                    editor.apply();
                }
            }
        });
                */
    }

    private void checkPermissionsExplicitly() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                PERMISSION_GRANTED = true;
            }

        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE
            }, REQUEST_PERMISSION);
            PERMISSION_GRANTED = false;
        }

    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            PERMISSION_GRANTED = true;
                            return;
                        }
                    }
                } else {
                    Toast.makeText(this, "Permissions Denied ", Toast.LENGTH_SHORT).show();
                    PERMISSION_GRANTED = false;
                }
                return;
            }
        }
    }


    private void reloadConfig() {
        this.isMicBlocked = this.settings.getBoolean("protection_enabled", false);
        this.isUnmutedOnScreenOn = this.settings.getBoolean("unmute_on_screen_on", false);
    }


    private void initFields() {
        microphoneStatus = findViewById(R.id.status);
        checkBox = findViewById(R.id.checkbox);
        checkBox.setChecked(isMicBlocked);
        PERMISSION_GRANTED = false;
        darkModeToggle = findViewById(R.id.dark_mode_toggle);
    }

    public void enableProtection() {

        boolean enabled = checkBox.isChecked();
        Editor editor = this.settings.edit();
        editor.putBoolean("protection_enabled", enabled);
        editor.commit();
        setStatus(enabled);
        restartGuardService();
    }


    /**
     * for future if needed
     */
    /*
    public void unmuteOnScreenOn(View view) {
        boolean enabled = screenSwitch.isChecked();
        Editor editor = this.settings.edit();
        editor.putBoolean("unmute_on_screen_on", enabled);
        editor.commit();
        restartGuardService();
    }*/

    /*
    public void showWhitelist(View view) {
        startActivity(new Intent(this, Whitelist.class));
    }*/
    private void restartGuardService() {
        reloadConfig();
        if (ActivityHelper.guardService != null) {
            ActivityHelper.guardService.reloadConfig();
        } else if (this.isMicBlocked) {
            // if (Build.VERSION.SDK_INT >= 26) {
            //     startForegroundService(new Intent(this, CallService.class));
            //     startForegroundService(new Intent(this, GuardService.class));
            // } else {
            startService(new Intent(this, CallService.class));
            startService(new Intent(this, GuardService.class));
            //  }
        }
    }

    public void setStatus(boolean isMicBlocked) {
        if (isMicBlocked) {
            microphoneStatus.setText("Microphone is OFF");
            microphoneStatus.setTextColor(getResources().getColor(R.color.red));
            return;
        }
        microphoneStatus.setText("Microphone is ON");
        microphoneStatus.setTextColor(getResources().getColor(R.color.green));
    }


}
