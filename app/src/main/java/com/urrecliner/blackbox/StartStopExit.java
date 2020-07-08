package com.urrecliner.blackbox;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.SNAP_SHOT_INTERVAL;
import static com.urrecliner.blackbox.Vars.cameraUtils;
import static com.urrecliner.blackbox.Vars.directionSensor;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.normalMerge;
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnEvent;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.videoUtils;

class StartStopExit {

    private String logID = "StartStop";
    void startVideo() {
        utils.logBoth(logID, "Start Recording ---");
        mIsRecording = true;
        vBtnRecord.setImageResource(R.mipmap.on_recording);
        vBtnEvent.setImageResource(R.mipmap.event_ready);
//        utils.logBoth(logID, "Step 1 prepareRecord");
        try {
            videoUtils.prepareRecord();
        } catch (Exception e) {
            utils.logE(logID, "Prepare Error", e);
        }
        try {
            mediaRecorder.start();
        } catch (Exception e) {
            utils.logE(logID, "Start Error", e);
        }
        try {
            startCamera();
            startNormal();
        } catch (Exception e) {
            utils.logE(logID, "Start Camera, Normal Error", e);
        }
    }

    private Handler cameraTimer = new Handler() {
        public void handleMessage(Message msg) { cameraUtils.snapshotCamera(); }
    };
    private Timer timerSnapCamera = new Timer();
    private void startCamera() {
        snapMapIdx = 0;
//        timerSnapCamera = new Timer();
//        final TimerTask cameraTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (mIsRecording)
//                    cameraTimer.sendEmptyMessage(0);
//                else
//                    timerSnapCamera.cancel();
//            }
//        };
//        timerSnapCamera.schedule(cameraTask, 100, SNAP_SHOT_INTERVAL);

        timerSnapCamera = new Timer();
        final TimerTask cameraTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
                    cameraTimer.sendEmptyMessage(0);
                else
                    timerSnapCamera.cancel();
            }
        };
        timerSnapCamera.schedule(cameraTask, 100, SNAP_SHOT_INTERVAL);
    }

    private Timer normalTimer;
    private void startNormal() {
        normalTimer = new Timer();
        final TimerTask normalTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
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
            directionSensor.stop();
        } catch (Exception e) {
            utils.logE(logID, "Stop 2", e);
        }
    }

    void exitBlackBoxApp() {
        String s = "Exit\nBlackBox";
        utils.customToast(s, Toast.LENGTH_LONG, Color.BLACK);
        mExitApplication = true;
        snapBytes = null;
        if (mIsRecording)
            stopVideo();
        displayTime.stop();
        utils.logOnly(logID,s);
        new Timer().schedule(new TimerTask() {
            public void run() {
                mActivity.finish();
                mActivity.finishAffinity();
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 3000);
    }

}
