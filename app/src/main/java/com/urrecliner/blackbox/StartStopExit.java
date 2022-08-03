package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.INTERVAL_LEFT_RIGHT;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.photoCapture;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.videoMain;

import android.content.Intent;
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
        }, 5000);
    }

    static void reRunApplication(String msg, Exception e) {
        Toast.makeText(mContext,"Exception "+msg,Toast.LENGTH_LONG).show();
        utils.logE("return","/// application reloaded ///",e);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Intent sendIntent = mActivity.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
                assert sendIntent != null;
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(sendIntent);
            }
        }, 5000);
        mActivity.finish();
        mActivity.finishAffinity();
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());

    }
    private final static Handler zoomChangeTimer = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { photoCapture.zoomShotCamera(); }
    };
    
    private final Timer timerSnapCamera = new Timer();
    private void startSnapBigShot() {
        snapMapIdx = 0;
        final TimerTask cameraTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
                    zoomChangeTimer.sendEmptyMessage(0);
                else
                    timerSnapCamera.cancel();
            }
        };
        timerSnapCamera.schedule(cameraTask, 3000, INTERVAL_LEFT_RIGHT);
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
            timerSnapCamera.cancel();
            vBtnRecord.setImageResource(R.mipmap.recording_off);
            mediaRecorder.stop();
            mediaRecorder.reset();
        } catch (Exception e) {
            utils.logE(logID, "Stop 1", e);
        }
        try {
//            videoUtils.startPreview();
            normalTimer.cancel();
//            obdAccessUnused.stop();
//            directionSensor.stop();
        } catch (Exception e) {
            utils.logE(logID, "Stop 2", e);
        }
    }

    void exitBlackBoxApp() {
        utils.beepOnce(8,1f); // Exit BlackBox
        mExitApplication = true;
        if (mIsRecording) stopVideo();
        displayTime.stop();
        gpsTracker.stopGPS();
//        if (OBDConnected)
//            updateKiloChronology();
        utils.logOnly(logID,"Exit App");
        new Timer().schedule(new TimerTask() {
            public void run() {
                mActivity.finish();
                mActivity.finishAffinity();
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 2000);
    }

}