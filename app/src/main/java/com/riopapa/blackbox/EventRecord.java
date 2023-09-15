package com.riopapa.blackbox;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.riopapa.blackbox.Vars.CountEvent;
import static com.riopapa.blackbox.Vars.DATE_PREFIX;
import static com.riopapa.blackbox.Vars.FORMAT_TIME;
import static com.riopapa.blackbox.Vars.INTERVAL_EVENT;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.activeEventCount;
import static com.riopapa.blackbox.Vars.imageStack;
import static com.riopapa.blackbox.Vars.mPackageEventJpgPath;
import static com.riopapa.blackbox.Vars.shot_02;
import static com.riopapa.blackbox.Vars.shot_00;
import static com.riopapa.blackbox.Vars.shot_01;
import static com.riopapa.blackbox.Vars.gpsTracker;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vTextActiveCount;

public class EventRecord {

    File thisEventJpgPath;
    void start() {

        if (!mIsRecording) return;

        imageStack.addShotBuff(shot_00);
        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT/4;
        thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);

        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("start "+(CountEvent+1+activeEventCount),thisEventJpgPath.getName());
        utils.setVolume(70);
        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_00);
                SnapShotSave snapShotSave = new SnapShotSave();
                imageStack.addShotBuff(shot_00);
                snapShotSave.startSave(thisEventJpgPath, 1, false);
            }
        }, 100);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_01);
                imageStack.addShotBuff(shot_01);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, 2, false);
            }
        }, INTERVAL_EVENT * 5 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_02);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, 3, true);
            }
        }, INTERVAL_EVENT * 10 / 10);

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

//                new com.riopapa.blackbox.MergeEvent();
//            new MergeEvent().exec(startTime);

            }
        }, INTERVAL_EVENT * 9 / 10);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed "+text, Toast.LENGTH_LONG, Color.RED);
        });
    }

}