package com.urrecliner.blackbox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import static com.urrecliner.blackbox.Vars.DELAY_I_WILL_BACK;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.utils;

class BeBackSoon extends AsyncTask<String, String, String> {

    private String jumpTo, title;
    private int downCount;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... s) {
        jumpTo = s[0];
        title = s[1];
        downCount = Integer.parseInt(s[2]);
        while (downCount > 0) {
            publishProgress("" + downCount);
            SystemClock.sleep(1000);
            downCount--;
        }
        return "Done";
    }
    protected void onProgressUpdate(String... s) {
        utils.displayCount(title+"\n"+downCount,Toast.LENGTH_SHORT, Color.DKGRAY);
    }

    @Override
    protected void onPostExecute(String m) {
        if(jumpTo.equals("x")) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(() -> {
                utils.beepOnce(7,0.4f); // I will be back
                utils.displayCount("I will be back in "+ DELAY_I_WILL_BACK +" secs.",Toast.LENGTH_LONG, Color.BLACK);
            }, 0);
            mHandler.postDelayed(() -> {
                Intent restartIntent = mContext.getPackageManager()     // exit and reload app
                        .getLaunchIntentForPackage(mContext.getPackageName() );
                PendingIntent intent = PendingIntent.getActivity(
                        mContext, 0,
                        restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                manager.set(AlarmManager.RTC, System.currentTimeMillis() + DELAY_I_WILL_BACK *1000, intent);
                mActivity.finish();
                System.exit(2);
            }, 2000);

        }
        else if (jumpTo.equals("v")) {
        }
        else
            Log.e("jumpTo","jumpTo Error : "+jumpTo);
    }
}
