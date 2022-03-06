package com.urrecliner.blackbox;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.DATE_PREFIX;
import static com.urrecliner.blackbox.Vars.FORMAT_TIME;
import static com.urrecliner.blackbox.Vars.INTERVAL_EVENT;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.bytesEventActive;
import static com.urrecliner.blackbox.Vars.bytesEventStarted;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mIsRecording;
import static com.urrecliner.blackbox.Vars.mPackageEventJpgPath;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.snapMapIdx;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.zoomHuge;

public class EventRecord {

    void start() {

        if (!mIsRecording) return;

        appendEventShot(bytesEventStarted);
        appendEventShot(bytesEventStarted);
        final long startTime = System.currentTimeMillis() - INTERVAL_EVENT - INTERVAL_EVENT / 2;
        final File thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);
        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("event","Starting ... "+thisEventJpgPath.getName());

        gpsTracker.askLocation();

        zoomHuge = true;
        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 1);
            }
        }, 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                appendEventShot(bytesEventActive);
                appendEventShot(bytesEventActive);
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 2);
            }
        }, INTERVAL_EVENT * 8 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SnapShotSave snapShotSave = new SnapShotSave();
                snapShotSave.startSave(thisEventJpgPath, snapMapIdx, 4);
                zoomHuge = false;
            }
        }, INTERVAL_EVENT * 17 / 10);

        new Timer().schedule(new TimerTask() {
            public void run() {
                EventMerge ev = new EventMerge();
                ev.merge(startTime);
            }
        }, INTERVAL_EVENT * 9 / 10);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed "+text, Toast.LENGTH_LONG, Color.RED);
        });
    }

    private void appendEventShot(byte[] byteImage) {
        snapBytes[snapMapIdx] = byteImage;
        snapMapIdx++;
        if (snapMapIdx >= MAX_IMAGES_SIZE)
            snapMapIdx = 0;
    }

}