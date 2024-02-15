package com.riopapa.blackbox;

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
import android.util.Log;
import android.view.View;

import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.vImgBattery;
import static com.riopapa.blackbox.Vars.vTextBattery;
import static com.riopapa.blackbox.Vars.vPreviewView;

class DisplayBattery  {

    static private int prevPercent = 0;
    static IntentFilter chgFilter = null;
    static Intent statusReceiver = null;
    static boolean prevCharging = false;
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        Log.w("onRecieve", action);
//        if (action.equals(Intent.ACTION_POWER_CONNECTED) || action.equals(Intent.ACTION_POWER_DISCONNECTED))
//            showBattery(action);
//    }

    void start() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//            ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
//            ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            mContext.registerReceiver(mBroadcastReceiver, ifilter);
        } catch (Exception e) {
            // ignore registering
        }
    }

    static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            show();
        }
    };

    static void show() {

        chgFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        statusReceiver = mContext.registerReceiver(null, chgFilter);
        int status = statusReceiver.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int nowPercent = statusReceiver.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//        int batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
//        int nowPercent = (int) (batteryLevel * 100 / (float) batteryScale);
        if (nowPercent < 50)
            vPreviewView.setVisibility(View.INVISIBLE);
        if (nowPercent != prevPercent || isCharging != prevCharging) {
            mActivity.runOnUiThread(() -> {
                String s = ""+nowPercent;
                vTextBattery.setText(s);
                prevPercent = nowPercent;
                prevCharging = isCharging;
                drawBattery(nowPercent, isCharging);
            });
        }
    }

    static final int CIRCLE_RADIUS = 70, CIRCLE_WIDTH = 4;
    static void drawBattery(int nowPercent, boolean isCharging) {
        Bitmap bitmap = Bitmap.createBitmap(CIRCLE_RADIUS, CIRCLE_RADIUS, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        final RectF rect = new RectF();
        rect.set(CIRCLE_WIDTH, CIRCLE_WIDTH, CIRCLE_RADIUS - CIRCLE_WIDTH, CIRCLE_RADIUS - CIRCLE_WIDTH);
        if (isCharging) {
            if (nowPercent > 30)
                paint.setColor(Color.GREEN);
            else
                paint.setColor(Color.YELLOW);
        } else
            paint.setColor(Color.GRAY);
        paint.setStrokeWidth(CIRCLE_WIDTH);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawArc(rect, 90f- (nowPercent*180f/100f), nowPercent*360f/100f , false, paint);
        vImgBattery.setImageBitmap(bitmap);
    }
}