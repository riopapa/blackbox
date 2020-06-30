package com.urrecliner.blackbox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import static com.urrecliner.blackbox.Vars.DELAY_I_WILL_BACK;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.willBack;

class BeBackSoon extends AsyncTask<String, String, String> {

    private String jumpTo, title;
    private int downCount;
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//    }

    @Override
    protected String doInBackground(String... s) {
        jumpTo = s[0];
        title = s[1];
        utils.beepOnce(7,0.7f); // I will be back
        downCount = Integer.parseInt(s[2]);
        while (downCount > 0) {
            publishProgress("" + downCount);
            SystemClock.sleep(1000);
            downCount--;
        }
//        Handler mHandler = new Handler(Looper.getMainLooper());
//        mHandler.postDelayed(() -> {
//            utils.displayCount("I will be back in "+ DELAY_I_WILL_BACK +" secs.",Toast.LENGTH_LONG, Color.BLACK);
//        }, 100);
        return "Done";
    }
    protected void onProgressUpdate(String... s) {
        utils.displayCount(title+"\n"+downCount,Toast.LENGTH_SHORT, Color.DKGRAY);
    }

    @Override
    protected void onPostExecute(String m) {
        if(jumpTo.equals("x")) {
            if (mExitApplication) {
                Toast.makeText(mContext,"Terminate BlackBox Now",Toast.LENGTH_LONG).show();
            } else {
                Handler mHandler = new Handler(Looper.getMainLooper());
                mHandler.postDelayed(() -> {
//                reStartApp();
                    AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent alarmIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + DELAY_I_WILL_BACK * 1000, alarmIntent);
                    Runtime.getRuntime().exit(0);
                }, 2000);
            }
        }
        else if (jumpTo.equals("v")) {
            Log.w("v","v");
        }
        else
            Log.e("jumpTo","jumpTo Error : "+jumpTo);
    }
}
