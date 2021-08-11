package com.urrecliner.blackbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.SNAP_SHOT_INTERVAL;
import static com.urrecliner.blackbox.Vars.chronoLogs;
import static com.urrecliner.blackbox.Vars.kiloMeter;
import static com.urrecliner.blackbox.Vars.photoCapture;
import static com.urrecliner.blackbox.Vars.displayTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.mExitApplication;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mediaRecorder;
import static com.urrecliner.blackbox.Vars.obdAccess;
import static com.urrecliner.blackbox.Vars.sharedPref;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.chronoNowDate;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vBtnRecord;
import static com.urrecliner.blackbox.Vars.videoMain;
import static com.urrecliner.blackbox.Vars.ChronoLog;

import com.google.gson.Gson;

class StartStopExit {

    private final String logID = "StartStop";
    void startVideo() {
        utils.logBoth(logID, "Start Recording ---");
        mIsRecording = true;
        vBtnRecord.setImageResource(R.mipmap.recording_on);
        videoMain.prepareRecord();
        mediaRecorder.start();
        try {
            startSnapBigShot(SNAP_SHOT_INTERVAL * 12 / 17);
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
    private final static Handler cameraTimer = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { photoCapture.zoomShotCamera(); }
    };
    
    private final Timer timerSnapCamera = new Timer();
    private void startSnapBigShot(long interval) {
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
        timerSnapCamera.schedule(cameraTask, 3000, interval);
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
            obdAccess.stop();
//            directionSensor.stop();
        } catch (Exception e) {
            utils.logE(logID, "Stop 2", e);
        }
    }

    void exitBlackBoxApp() {
        utils.beepOnce(8,0.7f); // Exit BlackBox
        mExitApplication = true;
        if (mIsRecording) stopVideo();
        displayTime.stop();
        if (chronoNowDate != null)
           updateKiloChronology();
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

    private void updateKiloChronology() {
        if (chronoLogs.size() == 0) {
            addTodayKilo();
        } else {
            if (chronoLogs.size() > 5)
                chronoLogs.remove(0);
            ChronoLog chronoOld = chronoLogs.get(chronoLogs.size() - 1);
            if (chronoOld.chroDate.equals(chronoNowDate)) {
                chronoOld.chroKilo = kiloMeter;
                chronoLogs.set(chronoLogs.size() - 1, chronoOld);
            } else {
                addTodayKilo();
            }
        }
        if (chronoLogs.size() > 0) {
            SharedPreferences.Editor prefsEditor = sharedPref.edit();
            Gson gson = new Gson();
            String json = gson.toJson(chronoLogs);
            prefsEditor.putString("chrono", json);
            prefsEditor.apply();
        }
    }

    private void addTodayKilo() {
        if (kiloMeter != -1) {
            ChronoLog chronoLog = new ChronoLog();
            chronoLog.chroDate = chronoNowDate;
            chronoLog.chroKilo = kiloMeter;
            chronoLogs.add(chronoLog);
        }
    }
}
