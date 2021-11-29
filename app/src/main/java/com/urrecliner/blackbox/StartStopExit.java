package com.urrecliner.blackbox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.INTERVAL_LEFT_RIGHT;
import static com.urrecliner.blackbox.Vars.INTERVAL_NORMAL;
import static com.urrecliner.blackbox.Vars.OBDConnected;
import static com.urrecliner.blackbox.Vars.chronoLogs;
import static com.urrecliner.blackbox.Vars.chronoKiloMeter;
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
import static com.urrecliner.blackbox.Vars.todayKiloMeter;
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
            startSnapBigShot();
            startNormal();
        } catch (Exception e) {
            reRunApplication("Start Camera, Normal Error", e);
        }
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
            obdAccess.stop();
//            directionSensor.stop();
        } catch (Exception e) {
            utils.logE(logID, "Stop 2", e);
        }
    }

    void exitBlackBoxApp() {
        utils.beepOnce(8,0.3f); // Exit BlackBox
        mExitApplication = true;
        if (mIsRecording) stopVideo();
        displayTime.stop();
        if (OBDConnected)
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
        if (chronoNowDate == null)
            return;
        if (chronoLogs.size() == 0) {
            addTodayKilo();
        } else {
            if (chronoLogs.size() > 10)
                chronoLogs.remove(0);
            ChronoLog chronoLatest = chronoLogs.get(chronoLogs.size() - 1);
            if (chronoLatest.chroDate.equals(chronoNowDate) && chronoLatest.todayKilo < todayKiloMeter) {
                chronoLatest.chroKilo = chronoKiloMeter;
                chronoLatest.todayKilo = todayKiloMeter;
                chronoLogs.set(chronoLogs.size() - 1, chronoLatest);
            } else {
                addTodayKilo();
            }
        }
        SharedPreferences.Editor prefsEditor = sharedPref.edit();
        Gson gson = new Gson();
        String json = gson.toJson(chronoLogs);
        prefsEditor.putString("chrono", json);
        prefsEditor.apply();
    }

    private void addTodayKilo() {
        if (todayKiloMeter != -1) {
            ChronoLog chronoLog = new ChronoLog();
            chronoLog.chroDate = chronoNowDate;
            chronoLog.chroKilo = chronoKiloMeter;
            chronoLog.todayKilo = todayKiloMeter;
            chronoLogs.add(chronoLog);
        }
    }
}