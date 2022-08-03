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
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.shot_02;
import static com.urrecliner.blackbox.Vars.shot_00;
import static com.urrecliner.blackbox.Vars.shot_01;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.shot_03;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.zoomHuge;

public class EventRecord {

    File thisEventJpgPath;
    void start() {

        if (!mIsRecording) return;
        appendEventShot(shot_00);
        appendEventShot(shot_00);

        zoomHuge = true;
        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT / 3;
        thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);

        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("start"+(CountEvent+1+activeEventCount),thisEventJpgPath.getName());
        utils.setVolume(70);
        gpsTracker.askLocation();
        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 1);
            }
        }, 100);

        new Timer().schedule(new TimerTask() {
            public void run() {
                appendEventShot(shot_01);
                appendEventShot(shot_01);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 2);
                zoomHuge = false;
            }
        }, INTERVAL_EVENT * 6 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                appendEventShot(shot_02);
                appendEventShot(shot_02);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 3);
                zoomHuge = false;
            }
        }, INTERVAL_EVENT * 12 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 4);
            }
        }, INTERVAL_EVENT * 17 / 10);

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

    public void appendEventShot(byte[] byteImage) {
        snapBytes[snapMapIdx] = byteImage;
        snapMapIdx++;
        if (snapMapIdx >= MAX_IMAGES_SIZE)
            snapMapIdx = 0;
    }
}