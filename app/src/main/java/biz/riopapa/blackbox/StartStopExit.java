package biz.riopapa.blackbox;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static biz.riopapa.blackbox.Vars.displayTime;
import static biz.riopapa.blackbox.Vars.gpsTracker;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mExitApplication;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.mediaRecorder;
import static biz.riopapa.blackbox.Vars.nextCount;
import static biz.riopapa.blackbox.Vars.normal_duration;
import static biz.riopapa.blackbox.Vars.photoCapture;
import static biz.riopapa.blackbox.Vars.share_left_right_interval;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vPower;
import static biz.riopapa.blackbox.Vars.vTextRecord;
import static biz.riopapa.blackbox.Vars.videoMain;

import android.app.Activity;
import android.content.Intent;

import java.util.Timer;
import java.util.TimerTask;

public class StartStopExit {

    private final String logID = "StartStop";
    Timer timerSnapCamera;

    void startVideo() {
        mActivity.runOnUiThread(() -> {
            utils.logBoth(logID, "Record On");
            mIsRecording = true;
            vPower.setImageResource(R.drawable.circle0);
            nextCount = 0;
            vTextRecord.setText("0");
            videoMain.prepareRecord();
            new Timer().schedule(new TimerTask() {
                public void run() {
                    try {
                        mediaRecorder.start();
                    } catch (Exception e) {
                        reStartApp();
                    }
                    startSnapBigShot();
                    startNormal();
                }
            }, 1000);
        });
    }

    void startSnapBigShot() {

        photoCapture = new PhotoCapture();
        photoCapture.photoInit();

        final TimerTask cameraTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording)
                    photoCapture.zoomedShot();
            }
        };
        timerSnapCamera = new Timer();
        timerSnapCamera.schedule(cameraTask, 1300, share_left_right_interval);
    }

    private Timer normalTimer;

    private void startNormal() {
        MergeNormal mergeNormal = new MergeNormal();
        normalTimer = new Timer();
        final TimerTask normalTask = new TimerTask() {
            @Override
            public void run() {
                if (mIsRecording && !mExitApplication) {
                    mergeNormal.exec();
                }
            }
        };
        normalTimer.schedule(normalTask, normal_duration, normal_duration);
    }

    void stopVideo() {
        vPower.setImageResource(R.drawable.recording_off);
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

    void exitApp() {
        utils.beepOnce(8, 1f); // Exit BlackBox
        mExitApplication = true;
        if (mIsRecording) stopVideo();
        mIsRecording = false;
        displayTime.stop();
        gpsTracker.stopGPS();
        new Timer().schedule(new TimerTask() {
            public void run() {
            utils.logOnly(logID, "Exit App");
            System.exit(0);
            android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 3000);
    }

    public static void reStartApp() {
        if (mContext instanceof Activity) {
            ((Activity) mContext).finish();
        }
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);

        Runtime.getRuntime().exit(0);
    }

}