package com.urrecliner.blackbox;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    private String logID = "SnapShot";
    private CountDownTimer countDownTimer;
    private int idx;

    void start(final File thisEventPath, final byte[][] snapCloned, final int snapIdx, final boolean first) {
        int jdx = 0;
        final int startBias = (first) ? 1000: 1200; // for snapshot image sequence
        final int finishIdx = (first) ? MAX_IMAGES_SIZE-16: MAX_IMAGES_SIZE-20;  // to minimize snapshot image counts
        byte[][] jpgBytes = new byte[MAX_IMAGES_SIZE+1][];
        for (int i = snapIdx; i < MAX_IMAGES_SIZE; i++)
            jpgBytes[jdx++] = snapCloned[i];
        for (int i = 0; i < snapIdx; i++)
            jpgBytes[jdx++] = snapCloned[i];
        final int saveInterval = 200;   // check phone CPU Capability
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTimer = new CountDownTimer((saveInterval+50) * MAX_IMAGES_SIZE, saveInterval) {
                    public void onTick(long millisUntilFinished) {
//                        utils.logOnly(logID, "idx="+idx);
                        if (idx < finishIdx) {
                            if (jpgBytes[idx] != null && jpgBytes[idx].length > 0) {
                                final File jpgFile = new File(thisEventPath, "SnapShot_"+("" + (startBias + idx)).substring(1, 4) + ".jpg");
                                bytes2File(jpgBytes[idx], jpgFile);
                            }
                            idx++;
                        }
                    }
                    public void onFinish() {
//                        utils.logBoth(logID, "SnapShots saved .. "+startBias);
                        if (!first)
                            showCompleted(thisEventPath);
                    }
                };
                countDownTimer.start();
            }
        }, 0);
    }

    private void showCompleted(File thisEventPath) {
        utils.beepOnce(3, .7f);
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
