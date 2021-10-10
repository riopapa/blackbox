package com.urrecliner.blackbox;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;

public class EventRecord {

    void start() {

        if (!mIsRecording) return;
        utils.logBoth("event","Event Starting ...");

//        cameraZoomIn = new Timer();
//        zoomFactor = 1.818f;
//        TimerTask cameraTask = new TimerTask() {
//            @Override
//            public void run() {
//                if (zoomFactor < 2.8f) {
//                    utils.logOnly("zoom","change factor "+zoomFactor);
//                    videoUtils.buildCameraSession(zoomFactor);
//                    zoomFactor += 0.1f;
//                }
//                else
//                    cameraZoomIn.cancel();
//            }
//        };
//        cameraZoomIn.schedule(cameraTask, 100, 100);

        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT;
        final File thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);
        utils.readyPackageFolder(thisEventJpgPath);

        gpsTracker.askLocation();

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 1);
            }
        }, 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 2);
            }
        }, INTERVAL_EVENT * 6 / 10);

//        new Timer().schedule(new TimerTask() {
//            public void run() {
//                SnapShotSave snapShotSave = new SnapShotSave();
//                snapShotSave.startSave(thisEventJpgPath, snapMapIdx,3);
//            }
//        }, INTERVAL_EVENT * 8 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx,4);
            }
        }, INTERVAL_EVENT * 12 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                EventMerge ev = new EventMerge();
                ev.merge(startTime);
            }
        }, INTERVAL_EVENT * 10 / 10);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed", Toast.LENGTH_LONG, Color.RED);
        });
    }

}