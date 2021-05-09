package com.urrecliner.blackbox;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import static com.urrecliner.blackbox.Vars.DELAY_WAIT_EXIT_SECONDS;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.utils;

class BeBackSoon extends AsyncTask<String, String, String> {

    private String xcode;
    private int downCount;

//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//    }

    @Override
    protected String doInBackground(String... s) {
        xcode = s[0];   // currently 'x' only
        downCount = DELAY_WAIT_EXIT_SECONDS;
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
        String msg = mContext.getString(R.string.i_will_back) +"\n"+downCount;
        utils.displayCount(msg, Toast.LENGTH_SHORT, Color.DKGRAY);
    }

    @Override
    protected void onPostExecute(String m) {
        if (!"x".equals(xcode))
            return;
        if (mExitApplication)
            return;
//                    utils.beepOnce(7,0.7f); // I will be back
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.urrecliner.blackboxwait");
            assert sendIntent != null;
//          sendIntent.putExtra("delay", DELAY_I_WILL_BACK);
            mActivity.startActivity(sendIntent);
            System.exit(0);
            Process.killProcess(Process.myPid());
        }, 500);
    }
}
