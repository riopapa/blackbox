package biz.riopapa.blackbox;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import static biz.riopapa.blackbox.Vars.DELAY_WAIT_EXIT_SECONDS;
import static biz.riopapa.blackbox.Vars.displayBattery;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mExitApplication;
import static biz.riopapa.blackbox.Vars.mainLayout;
import static biz.riopapa.blackbox.Vars.tvDegree;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vTextTime;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BeBackSoon extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... s) {
        String xcode = s[0];   // currently 'x' only
        int downCount = (xcode.equals("x"))? DELAY_WAIT_EXIT_SECONDS : 1;
        while (downCount > 0) {
            if (!mExitApplication) {
                publishProgress("" + downCount);
                SystemClock.sleep(800);
            }
            downCount--;
        }
        return "Done";
    }
    protected void onProgressUpdate(String... s) {
//        mActivity.runOnUiThread(() -> {
//            String msg = mContext.getString(R.string.i_will_back) +"\n"+s[0];
//            utils.displayCount(msg);
//        });
    }

    @Override
    protected void onPostExecute(String m) {
        if (mExitApplication)
            return;
//                    utils.beepOnce(7,0.7f); // I will be back
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage("biz.riopapa.blackwait");
            assert sendIntent != null;
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(sendIntent);
            System.exit(0);
            Process.killProcess(Process.myPid());
        }, 1000);
    }
}