package com.urrecliner.blackbox;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import static com.urrecliner.blackbox.Vars.DELAY_WAIT_EXIT_SECONDS;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.utils;

public class BeBackSoon extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... s) {
        String xcode = s[0];   // currently 'x' only
        int downCount = (xcode.equals("x"))? DELAY_WAIT_EXIT_SECONDS : 1;
        while (downCount > 0) {
            if (!mExitApplication) {
                publishProgress("" + downCount);
                SystemClock.sleep(1000);
            }
            downCount--;
        }
        return "Done";
    }
    protected void onProgressUpdate(String... s) {
        String msg = mContext.getString(R.string.i_will_back) +"\n"+s[0];
        utils.displayCount(msg);
    }

    @Override
    protected void onPostExecute(String m) {
        if (mExitApplication)
            return;
//                    utils.beepOnce(7,0.7f); // I will be back
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.urrecliner.blackboxwait");
            assert sendIntent != null;
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(sendIntent);
            System.exit(0);
            Process.killProcess(Process.myPid());
        }, 1000);
    }
}