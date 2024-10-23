package biz.riopapa.blackbox;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import static biz.riopapa.blackbox.Vars.CountEvent;
import static biz.riopapa.blackbox.Vars.DATE_PREFIX;
import static biz.riopapa.blackbox.Vars.FORMAT_TIME;
import static biz.riopapa.blackbox.Vars.SUFFIX;
import static biz.riopapa.blackbox.Vars.activeEventCount;
import static biz.riopapa.blackbox.Vars.imageStack;
import static biz.riopapa.blackbox.Vars.mPackageEventJpgPath;
import static biz.riopapa.blackbox.Vars.share_event_sec;
import static biz.riopapa.blackbox.Vars.shot_00;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vTextActiveCount;
public class EventRecord {

    File thisEventJpgPath;

    void start() {

        if (!mIsRecording) return;

        imageStack.addShotBuff(shot_00);
        final long startTime = System.currentTimeMillis()
                - ((share_event_sec + share_event_sec / 9) * 1000);
        thisEventJpgPath = new File(mPackageEventJpgPath, DATE_PREFIX+utils.getMilliSec2String(startTime, FORMAT_TIME)+ SUFFIX);

        utils.readyPackageFolder(thisEventJpgPath);
        utils.logBoth("start "+(CountEvent+1+activeEventCount),thisEventJpgPath.getName());
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
        }, share_event_sec * 400);

        new Timer().schedule(new TimerTask() {
            public void run() {
                snapShotSave.exec(thisEventJpgPath, 4, true);
            }
        }, share_event_sec * 700);

        new Timer().schedule(new TimerTask() {
            public void run() {
                MergeEvent mergeEvent = new MergeEvent();
                mergeEvent.exec(startTime);
            }
        }, share_event_sec * 800);

        activeEventCount++;
        mActivity.runOnUiThread(() -> {
            String text = " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
            utils.customToast("EVENT\nbutton\nPressed "+text);
        });
    }
}