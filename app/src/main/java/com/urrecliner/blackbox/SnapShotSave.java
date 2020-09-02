package com.urrecliner.blackbox;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.SNAP_SHOT_INTERVAL;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    private String logID = "SnapShot";
    private CountDownTimer countDownTimer;
    private int idx;
    byte[][] jpgBytes;
    long savedTime = 0;

    void start(final File thisEventPath, byte[][] snapCloned, final int snapIdx, final boolean first) {
        final int startBias = (first) ? 100: 214; // for snapshot image sequence, dependency : snap interval, snap size
        final int startIdx = (first) ? 0: 10;
        final int finishIdx = (first) ? MAX_IMAGES_SIZE-2: MAX_IMAGES_SIZE-40;  // to minimize snapshot image counts
        jpgBytes = new byte[MAX_IMAGES_SIZE+1][];
        idx = 0;
        for (int i = snapIdx; i < MAX_IMAGES_SIZE; i++)
            jpgBytes[idx++] = snapCloned[i];
        for (int i = 0; i < snapIdx; i++)
            jpgBytes[idx++] = snapCloned[i];
        snapCloned = null;
        final int saveInterval = 150;   // check phone CPU Capability
        idx = startIdx;
        countDownTimer = new CountDownTimer((saveInterval+20) * MAX_IMAGES_SIZE, saveInterval) {
            public void onTick(long millisUntilFinished) {
//                Log.w(finishIdx+" milsec "+idx,""+(millisUntilFinished-savedTime));
//                savedTime = millisUntilFinished;
//                Log.w("idx ",""+idx);
//                        utils.logOnly(logID, "idx="+idx);
                if (idx < finishIdx) {
                    if (jpgBytes[idx] != null && jpgBytes[idx].length > 1) {
                        final File jpgFile = new File(thisEventPath, "SnapShot_"+("" + (startBias+(idx*SNAP_SHOT_INTERVAL)/1150))+"."+idx+".jpg");
                        bytes2File(jpgBytes[idx], jpgFile);
//                                Log.w(""+idx,""+idx);
                        jpgBytes[idx] = null;
                    }
                }
                idx++;
            }
            public void onFinish() {
                jpgBytes = null;
//                        utils.logBoth(logID, "SnapShots saved .. "+startBias);
                if (!first)
                    sayEventCompleted(thisEventPath);
            }
        };
        countDownTimer.start();
    }

    private void sayEventCompleted(File thisEventPath) {
        utils.beepOnce(3, 1f);
        String countStr = "" + ++CountEvent;
        vTextCountEvent.setText(countStr);
        activeEventCount--;
        String text = (activeEventCount == 0) ? "" : "" + activeEventCount;
        vTextActiveCount.setText(text);
        ImageButton mEventButton = mActivity.findViewById(R.id.btnEvent);
        mEventButton.setImageResource(R.mipmap.event_ready);
        utils.logBoth(logID, thisEventPath.getName());
    }

    private void bytes2File(byte[] bytes, File file) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            utils.logE(logID, "IOException catch", e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    utils.logE(logID, "IOException finally", e);
                }
            }
        }
    }
}
