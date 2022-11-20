package com.urrecliner.blackbox;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.urrecliner.blackbox.Vars.share_left_right;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.photoCapture;
import static com.urrecliner.blackbox.Vars.snapNowPos;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.videoMain;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

class StartStopExit {

    private final String logID = "StartStop";

    void startVideo() {
        utils.logBoth(logID, "Record On");
        mIsRecording = true;
        vBtnRecord.setImageResource(R.mipmap.recording_on);
        videoMain.prepareRecord();
        new Timer().schedule(new TimerTask() {
            public void run() {
                mediaRecorder.start();
                startSnapBigShot();
                startNormal();
            }
        }, 3000);
    }

    static void reRunApplication(String msg, Exception e) {
        Toast.makeText(mContext, "Exception " + msg, Toast.LENGTH_LONG).show();
        utils.logE("return", "/// application reloaded ///", e);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
                assert sendIntent != null;
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                sendIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(sendIntent);
            }
        }, 5000);
        mActivity.finish();
        mActivity.finishAffinity();
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());

    }

    private final static Handler zoomChangeTimer = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            photoCapture.zoomShotCamera();
        }
    };

    private Timer timerSnapCamera;

    public void startSnapBigShot() {
        snapNowPos = 0;
        final TimerTask cameraTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
                    zoomChangeTimer.sendEmptyMessage(0);
                else
                    timerSnapCamera.cancel();
            }
        };
        timerSnapCamera = new Timer();
        timerSnapCamera.schedule(cameraTask, 3000, share_left_right);
    }

    private Timer normalTimer;

    private void startNormal() {
        NormalMerge normalMerge = new NormalMerge();
        normalTimer = new Timer();
        final TimerTask normalTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording && !mExitApplication)
                    normalMerge.merge();
            }
        };
        normalTimer.schedule(normalTask, INTERVAL_NORMAL, INTERVAL_NORMAL);
    }

    void stopVideo() {
        vBtnRecord.setImageResource(R.mipmap.recording_off);
        mIsRecording = false;
        try {
            timerSnapCamera.cancel();
            timerSnapCamera.purge();
            timerSnapCamera = null;
        } catch (Exception e) {
            utils.logE(logID, "timerSnapCamera Stop 1", e);
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder = null;
        } catch (Exception e) {
            utils.logE(logID, "mediaRecorder Stop 2", e);
        }
        try {
            normalTimer.cancel();
            normalTimer.purge();
            normalTimer = null;
        } catch (Exception e) {
            utils.logE(logID, "normalTimer Stop 3", e);
        }
    }

    void exitApp(boolean reRun) {
        if (!reRun)
            utils.beepOnce(8, 1f); // Exit BlackBox
        mExitApplication = true;
        if (mIsRecording) stopVideo();
        displayTime.stop();
        gpsTracker.stopGPS();
        new Timer().schedule(new TimerTask() {
            public void run() {
                if (reRun) {
                    reStartApp();
//                    triggerRebirth();
                }
                utils.logOnly(logID, "Exit App");
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 3000);
    }

    public static void reStartApp() {
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra(KEY_RESTART_INTENT, nextIntent);
        mContext.startActivity(intent);
        if (mContext instanceof Activity) {
            ((Activity) mContext).finish();
        }

        Runtime.getRuntime().exit(0);
    }

    public static void triggerRebirth() {
        PackageManager pm = mContext.getPackageManager();
        //check if we got the PackageManager
        if (pm != null) {
            //create the intent with the default start activity for your application
            Intent mStartActivity = pm.getLaunchIntentForPackage(mContext.getPackageName());
            if (mStartActivity != null) {
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //create a pending intent so the application is restarted after System.exit(0) was called.
                // We use an AlarmManager to call this intent in 100ms
                int mPendingIntentId = 223344;
                PendingIntent mPendingIntent = PendingIntent
                        .getActivity(mContext, mPendingIntentId, mStartActivity,
                                PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 3000, mPendingIntent);
                //kill the application
                System.exit(0);
            }
        }
    }
}