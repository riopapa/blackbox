package com.urrecliner.blackbox;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import static com.urrecliner.blackbox.Vars.DELAY_WAIT_EXIT;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.utils;

class BeBackSoon extends AsyncTask<String, String, String> {

    private String xcode, title;
    private int downCount;
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//    }

    @Override
    protected String doInBackground(String... s) {
        xcode = s[0];   // currently 'x' only
        title = mContext.getString(R.string.i_will_back);
        downCount = DELAY_WAIT_EXIT;
        while (downCount > 0) {
            if (!mExitApplication) {
                publishProgress("" + downCount);
                SystemClock.sleep(1000);
            }
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
        switch (xcode) {
            case "x":
                if (!mExitApplication) {
//                    utils.beepOnce(7,0.7f); // I will be back
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(() -> {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {    // s8 is 28 android.P
                            Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.urrecliner.blackboxwait");
                            assert sendIntent != null;
//                            sendIntent.putExtra("delay", DELAY_I_WILL_BACK);
                            mActivity.startActivity(sendIntent);
                            System.exit(0);
                            android.os.Process.killProcess(android.os.Process.myPid());
//                        } else {
//                  followings are delayed load, not active after Android.P
//                            AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//                            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
//                            assert intent != null;
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            PendingIntent alarmIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//                            assert alarmMgr != null;
//                        alarmMgr.set(AlarmManager.RTC, System.currentTimeMillis() + DELAY_I_WILL_BACK, alarmIntent);
//                            alarmMgr.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + DELAY_I_WILL_BACK ,alarmIntent);
//                            mActivity.finish();
                            //                        Runtime.getRuntime().exit(0);
//                            System.exit(0);
//                            android.os.Process.killProcess(android.os.Process.myPid());
//                        }
                    }, 1000);
                }
                break;
            default :
                Log.e("jumpTo","jumpTo Error : "+ xcode);
        }
    }
}
