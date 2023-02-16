package com.urrecliner.blackbox;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.imageStack;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.shot_02;
import static com.urrecliner.blackbox.Vars.shot_00;
import static com.urrecliner.blackbox.Vars.shot_01;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.zoomHuge;

public class EventRecord {

    File thisEventJpgPath;
    void start() {

        if (!mIsRecording) return;
        imageStack.addShot(shot_00);
        imageStack.addShot(shot_00);

        zoomHuge = true;
        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT / 3;
        thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);

        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("start"+(CountEvent+1+activeEventCount),thisEventJpgPath.getName());
        utils.setVolume(70);
        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, 1);
            }
        }, 100);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShot(shot_01);
                imageStack.addShot(shot_01);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, 2);
                zoomHuge = false;
            }
        }, INTERVAL_EVENT * 6 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShot(shot_02);
                imageStack.addShot(shot_02);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, 3);
                zoomHuge = false;
            }
        }, INTERVAL_EVENT * 11 / 10);

//        new Timer().schedule(new TimerTask() {
//            public void run() {
//                SnapShotSave snapShotSave = new SnapShotSave();
//                snapShotSave.startSave(thisEventJpgPath, 4);
//            }
//        }, INTERVAL_EVENT * 17 / 10);

        gpsTracker.askLocation();
        new Timer().schedule(new TimerTask() {
            public void run() {
                new EventMerge().merge(startTime);
            }
        }, INTERVAL_EVENT * 8 / 10);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed "+text, Toast.LENGTH_LONG, Color.RED);
        });
    }

}