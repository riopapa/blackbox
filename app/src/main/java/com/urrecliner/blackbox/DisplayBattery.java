package com.urrecliner.blackbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;

import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vImgBattery;
import static com.urrecliner.blackbox.Vars.vTextBattery;
import static com.urrecliner.blackbox.Vars.vTextureView;

class DisplayBattery extends BroadcastReceiver {

    private int [] batteryMipmap = { R.mipmap.battery_none, R.mipmap.battery_level90, R.mipmap.battery_level70, R.mipmap.battery_level50, R.mipmap.battery_level30};
    private int batteryPrev = 0;
    private BroadcastReceiver mPowerOn = null, mPowerOff = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final int level = intent.getIntExtra( BatteryManager.EXTRA_LEVEL, 0 );
        final int scale = intent.getIntExtra( BatteryManager.EXTRA_SCALE, 0 );

        utils.logBoth("received","battery: "+action+" level= "+level+" scale= "+scale);
        showBattery();
    }

    void init() {
//        try {
//            mContext.unregisterReceiver(mPowerOn);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            mContext.unregisterReceiver(mPowerOff);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mPowerOn = new BatteryBroadcastReceiver();
//        mContext.registerReceiver(mPowerOn, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
//        mPowerOff = new BatteryBroadcastReceiver();
//        mContext.registerReceiver(mPowerOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    void showBattery() {
//        utils.log("show"," detected");
        boolean isCharging;
        IntentFilter intFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, intFilter);
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int) (level * 100 / (float) scale);
        int batteryNow;
        if (isCharging) {
            if (batteryPct > 85)
                batteryNow = batteryMipmap[1];
            else if (batteryPct > 70)
                batteryNow = batteryMipmap[2];
            else if (batteryPct > 50)
                batteryNow = batteryMipmap[3];
            else
                batteryNow = batteryMipmap[4];
            if (batteryPct < 50)
                vTextureView.setVisibility(View.INVISIBLE);
        } else
            batteryNow = batteryMipmap[0];
        if (batteryNow != batteryPrev) {
            mActivity.runOnUiThread(() -> {
                vImgBattery.setImageResource(batteryNow);
            });
            batteryPrev = batteryNow;
        }
        String s = ""+batteryPct;
        vTextBattery.setText(s);
    }
}
