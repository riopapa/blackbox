package com.urrecliner.blackbox;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.utils;

class SnapShotSave {

    private String logID = "SnapShot";
    private CountDownTimer countDownTimer;
    private int idx;

    void start(final File thisEventPath, final byte[][] snapCloned, final int snapIdx, final String prefix) {
        idx =0;
        int jdx = 0;
        byte[][] jpgBytes = new byte[MAX_IMAGES_SIZE+1][];
        for (int i = snapIdx; i < MAX_IMAGES_SIZE; i++)
            jpgBytes[jdx++] = snapCloned[i];
        for (int i = 0; i < snapIdx; i++)
            jpgBytes[jdx++] = snapCloned[i];

        final int interval = 220;
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTimer = new CountDownTimer((interval+50) * MAX_IMAGES_SIZE, interval) {
                    public void onTick(long millisUntilFinished) {
//                        utils.logOnly(logID, "idx="+idx);
                        if (idx < MAX_IMAGES_SIZE) {
                            if (jpgBytes[idx] != null && jpgBytes[idx].length > 0) {
                                final File jpgFile = new File(thisEventPath, "SnapShot "+prefix+"_"+("" + (1000 + idx)).substring(1, 4) + ".jpg");
                                bytes2File(jpgBytes[idx], jpgFile);
                            }
                            idx++;
                        }
                    }
                    public void onFinish() {
                        utils.logBoth(logID, "SnapShots saved .. "+prefix);
                    }
                };
//        utils.logBoth(logID, "countDownTimer start ---");
                countDownTimer.start();
            }
        }, 0);
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
