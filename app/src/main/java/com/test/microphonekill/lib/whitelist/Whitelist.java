package com.test.microphonekill.lib.whitelist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.test.test.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Whitelist extends Activity {
    private PackageManager packageManager;
    private ListView whitelist;
    private ArrayAdapter<PackageInfo> whitelistItemAdapter;

    @SuppressLint("ResourceType")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.whitelist);
        this.packageManager = getPackageManager();
        List<PackageInfo> allApps = this.packageManager.getInstalledPackages(0);
        Collections.sort(allApps, new Comparator<PackageInfo>() {
            public int compare(PackageInfo o1, PackageInfo o2) {
                return o1.applicationInfo.loadLabel(Whitelist.this.getPackageManager()).toString().compareToIgnoreCase(o2.applicationInfo.loadLabel(Whitelist.this.getPackageManager()).toString());
            }
        });
        this.whitelistItemAdapter = new WhitelistItemAdapter(getApplicationContext(), allApps);
        this.whitelist = (ListView) findViewById(R.layout.whitelist);
        this.whitelist.setClickable(true);
        this.whitelist.setAdapter(this.whitelistItemAdapter);
    }
}
