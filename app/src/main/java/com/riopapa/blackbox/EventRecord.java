package com.riopapa.blackbox;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.riopapa.blackbox.Vars.CountEvent;
import static com.riopapa.blackbox.Vars.DATE_PREFIX;
import static com.riopapa.blackbox.Vars.FORMAT_TIME;
import static com.riopapa.blackbox.Vars.SUFFIX;
import static com.riopapa.blackbox.Vars.activeEventCount;
import static com.riopapa.blackbox.Vars.imageStack;
import static com.riopapa.blackbox.Vars.mPackageEventJpgPath;
import static com.riopapa.blackbox.Vars.share_event_sec;
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
        final long startTime = System.currentTimeMillis()
                - ((share_event_sec + share_event_sec /4) * 1000);
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
        }, 20);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_01);
                SnapShotSave snapShotSave = new SnapShotSave();
                imageStack.addShotBuff(shot_01);
                snapShotSave.startSave(thisEventJpgPath, 2, false);
            }
        }, share_event_sec * 300);

        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_02);
                SnapShotSave snapShotSave = new SnapShotSave();
                imageStack.addShotBuff(shot_02);
                snapShotSave.startSave(thisEventJpgPath, 3, true);
            }
        }, share_event_sec * 600);

//        new Timer().schedule(new TimerTask() {
//            public void run() {
//                SnapShotSave snapShotSave = new SnapShotSave();
//                snapShotSave.startSave(thisEventJpgPath, 4);
//            }
//        }, INTERVAL_EVENT * 17 / 10);


//        gpsTracker.askLocation();
        new Timer().schedule(new TimerTask() {
            public void run() {
//                new EventMerge().merge(startTime);
            MergeEvent mergeEvent = new MergeEvent();
            mergeEvent.exec(startTime);
//                new com.riopapa.blackbox.MergeEvent();
//            new MergeEvent().exec(startTime);

            }
        }, share_event_sec * 900);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed "+text, Toast.LENGTH_LONG, Color.RED);
        });
    }

}