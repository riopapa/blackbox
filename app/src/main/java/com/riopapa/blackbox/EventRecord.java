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
                - ((share_event_sec + share_event_sec /7) * 1000);
        thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);

        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("start "+(CountEvent+1+activeEventCount),thisEventJpgPath.getName());
        utils.setVolume(70);
//        long veryFirst;
        SnapShotSave snapShotSave = new SnapShotSave();
        new Timer().schedule(new TimerTask() {
            public void run() {
                imageStack.addShotBuff(shot_00);    // first phase should be 1
                snapShotSave.exec(thisEventJpgPath, 1, false);
            }
        }, 20);

        new Timer().schedule(new TimerTask() {
            public void run() {
                snapShotSave.exec(thisEventJpgPath, 2, false);
            }
        }, share_event_sec * 600);

        new Timer().schedule(new TimerTask() {
            public void run() {
                snapShotSave.exec(thisEventJpgPath, 4, true);
            }
        }, share_event_sec * 1100);

        new Timer().schedule(new TimerTask() {
            public void run() {
            MergeEvent mergeEvent = new MergeEvent();
            mergeEvent.exec(startTime);
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