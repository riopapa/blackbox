package com.urrecliner.blackbox;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.photoCapture;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.videoMain;

class StartStopExit {

    private final String logID = "StartStop";
    void startVideo() {
        utils.logBoth(logID, "Start Recording ---");
        mIsRecording = true;
        vBtnRecord.setImageResource(R.mipmap.on_recording);
//        utils.logBoth(logID, "Step 1 prepareRecord");
        try {
            videoMain.prepareRecord();
        } catch (Exception e) {
            reRunApplication("Prepare Error", e);
        }
        try {
            mediaRecorder.start();
        } catch (Exception e) {
            reRunApplication("Start Error", e);
        }
        try {
            snapBiggerCamera();
            startNormal();
        } catch (Exception e) {
            reRunApplication("Start Camera, Normal Error", e);
        }
    }

    static void reRunApplication(String msg, Exception e) {
        Toast.makeText(mContext,"Exception "+msg,Toast.LENGTH_LONG).show();
        utils.logE("return","/// application reloaded ///",e);
        Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
        assert sendIntent != null;
        mContext.startActivity(sendIntent);

    }
    private final static Handler cameraTimer = new Handler() {
        public void handleMessage(Message msg) { photoCapture.zoomShotCamera(); }
    };
    private final Timer timerSnapCamera = new Timer();
    final long BIGGER_SNAPSHOT_INTERVAL = 160;
    private void snapBiggerCamera() {
        snapMapIdx = 0;
        final TimerTask cameraTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
                    cameraTimer.sendEmptyMessage(0);
                else
                    timerSnapCamera.cancel();
            }
        };
        timerSnapCamera.schedule(cameraTask, 1000, BIGGER_SNAPSHOT_INTERVAL);
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
        try {
            mIsRecording = false;
            vBtnRecord.setImageResource(R.mipmap.off_recording);
            mediaRecorder.stop();
            mediaRecorder.reset();
            timerSnapCamera.cancel();
        } catch (Exception e) {
            utils.logE(logID, "Stop", e);
        }
        try {
//            videoUtils.startPreview();
            normalTimer.cancel();
            obdAccess.stop();
//            directionSensor.stop();
        } catch (Exception e) {
            utils.logE(logID, "Stop 2", e);
        }
    }

    void exitBlackBoxApp() {
        mExitApplication = true;
        String s = "\nExit\nBlackBox";
        utils.customToast(s, Toast.LENGTH_SHORT, Color.BLACK);
        if (mIsRecording)
            stopVideo();
        displayTime.stop();
        utils.beepOnce(8,0.3f); // Exit BlackBox
        utils.logOnly(logID,s);
        mActivity.finish();
        mActivity.finishAffinity();
        new Timer().schedule(new TimerTask() {
            public void run() {
//                if (CountEvent> 0) {
//                    Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage("com.urrecliner.blackboxjpg");
//                    assert sendIntent != null;
//                    mActivity.startActivity(sendIntent);
//                }
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 2000);
    }

}
