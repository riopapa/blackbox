package biz.riopapa.blackbox;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import static biz.riopapa.blackbox.Vars.DELAY_WAIT_EXIT_SECONDS;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mExitApplication;

public class BeBackSoon extends AsyncTask<String, String, String> {

    String xcode;
    @Override
    protected String doInBackground(String... s) {
        xcode = s[0];   // 'x' : blackWait, else blackMove
        int downCount = DELAY_WAIT_EXIT_SECONDS;
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
            String launchApp = (xcode.equals("x") ? "biz.riopapa.blackwait" : "biz.riopapa.blackmove");
            Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage(launchApp);
            assert sendIntent != null;
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(sendIntent);
            System.exit(0);
            Process.killProcess(Process.myPid());
        }, 1000);
    }
}