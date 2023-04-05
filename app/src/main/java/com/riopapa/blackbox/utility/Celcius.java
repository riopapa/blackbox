package com.riopapa.blackbox.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class Celcius {
    static int celcius;
    public static void start(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(mBroadcastReceiver, iFilter);
    }

    public static int get() {
        return celcius;
    }

    static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            celcius = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
        }
    };
}