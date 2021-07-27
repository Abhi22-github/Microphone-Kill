package com.test.microphonekill.lib.whitelist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.test.test.R;

import java.util.List;

public class WhitelistItemAdapter extends ArrayAdapter<PackageInfo> {
    public static final String WHITELIST = "cz.auradesign.lib.whitelist.settings";
    private LayoutInflater inflater;
    private PackageManager packageManager;
    private SharedPreferences whitelistPreferences;

    @SuppressLint("WrongConstant")
    public WhitelistItemAdapter(Context context, List<PackageInfo> apps) {
        super(context, 0, apps);
        this.packageManager = context.getPackageManager();
        this.inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.whitelistPreferences = context.getSharedPreferences(WHITELIST, 0);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (getCount() > 0 && position < getCount()) {
            PackageInfo packageInfo = (PackageInfo) getItem(position);
            if (convertView == null) {
                convertView = this.inflater.inflate(R.layout.list_item, null);
            }
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.checkWhitelist);
            ((TextView) convertView.findViewById(R.id.appName)).setText(this.packageManager.getApplicationLabel(packageInfo.applicationInfo));
            ((TextView) convertView.findViewById(R.id.appPackage)).setText(packageInfo.packageName);
            checkBox.setTag(packageInfo.packageName);
            if (this.whitelistPreferences.contains(packageInfo.packageName)) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
            ((ImageView) convertView.findViewById(R.id.appIcon)).setVisibility(0);
            ((ImageView) convertView.findViewById(R.id.appIcon)).setImageDrawable(this.packageManager.getApplicationIcon(packageInfo.applicationInfo));
            checkBox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkWhitelist);
                    boolean checked = checkBox.isChecked();
                    String packageName = checkBox.getTag().toString();
                    Editor editor = WhitelistItemAdapter.this.whitelistPreferences.edit();
                    if (checked) {
                        editor.putString(packageName, "");
                    } else {
                        editor.remove(packageName);
                    }
                    editor.commit();
                }
            });
            convertView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkWhitelist);
                    boolean checked = checkBox.isChecked();
                    String packageName = checkBox.getTag().toString();
                    Editor editor = WhitelistItemAdapter.this.whitelistPreferences.edit();
                    if (checked) {
                        editor.remove(packageName);
                        checkBox.setChecked(false);
                    } else {
                        editor.putString(packageName, "");
                        checkBox.setChecked(true);
                    }
                    editor.commit();
                }
            });
        }
        return convertView;
    }
}
