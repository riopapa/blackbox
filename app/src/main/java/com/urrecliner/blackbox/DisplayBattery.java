package com.urrecliner.blackbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.view.View;

import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vImgBattery;
import static com.urrecliner.blackbox.Vars.vTextBattery;
import static com.urrecliner.blackbox.Vars.vPreviewView;

class DisplayBattery extends BroadcastReceiver {

    private int batteryPrev = 0;

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
        boolean isCharging;
        IntentFilter intFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, intFilter);
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = (int) (batteryLevel * 100 / (float) batteryScale);
        if (batteryPct < 50)
            vPreviewView.setVisibility(View.INVISIBLE);
        if (batteryPct != batteryPrev) {
            mActivity.runOnUiThread(() -> {
                String s = ""+batteryPct;
                vTextBattery.setText(s);
                batteryPrev = batteryPct;
                drawBattery(batteryPct, isCharging);
            });
        }
    }

    final int CIRCLE_RADIUS = 200, CIRCLE_WIDTH = 16;
    void drawBattery(int batteryPCT, boolean isCharging) {
        Bitmap bitmap = Bitmap.createBitmap(CIRCLE_RADIUS, CIRCLE_RADIUS, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        final RectF rect = new RectF();
        rect.set(CIRCLE_WIDTH, CIRCLE_WIDTH, CIRCLE_RADIUS - CIRCLE_WIDTH, CIRCLE_RADIUS - CIRCLE_WIDTH);
        paint.setColor((isCharging) ? Color.GREEN : Color.GRAY);
        paint.setStrokeWidth(CIRCLE_WIDTH);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rect, 90-(180*batteryPCT/100), batteryPCT*360/100 , false, paint);
        vImgBattery.setImageBitmap(bitmap);
    }

}
