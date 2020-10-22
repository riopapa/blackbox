package com.urrecliner.blackbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.SNAP_SHOT_INTERVAL;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    private static String logID = "SnapShot";
    private int jpgIdx;
    byte[][] jpgBytes;
    int startBias;

    void start(File path2Write, int snapPointer, final int phase) {
        int startIdx = 0;
        int finishIdx = 0;
        switch (phase) {
            case 1:
                startIdx = 0; finishIdx = MAX_IMAGES_SIZE; startBias = 100;
                break;
            case 2:
                startIdx = 20; finishIdx = MAX_IMAGES_SIZE; startBias = 200;
                break;
            case 3:
                startIdx = 20; finishIdx = MAX_IMAGES_SIZE; startBias = 300;
                break;
        }
//        Log.w("start copy","to jpg array "+startIdx+" ~ finish "+finishIdx);
        jpgBytes = new byte[MAX_IMAGES_SIZE][];
        jpgIdx = 0;
        for (int i = snapPointer; i < MAX_IMAGES_SIZE; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
        }
        for (int i = 0; i < snapPointer; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
        }
        int finalStartIdx = startIdx;
        int finalFinishIdx = finishIdx;
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                for (jpgIdx = finalStartIdx; jpgIdx < finalFinishIdx; jpgIdx++) {
                    byte [] imageBytes = jpgBytes[jpgIdx];
                    if (imageBytes != null && imageBytes.length > 1) {
                        File imageFile = new File(path2Write, "SnapShot_" + ("" + (startBias + jpgIdx)) + ".jpg");
                        bytes2File(imageBytes, imageFile);
                        jpgBytes[jpgIdx] = null;
                        SystemClock.sleep(30);  // not to hold all the time
                    }
                }
            }
        });
        th.start();

        if (phase == 3)
            sayEventCompleted(path2Write);
    }

    private void sayEventCompleted(File thisEventPath) {
        utils.beepOnce(3, 1f);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String countStr = "" + ++CountEvent;
                vTextCountEvent.setText(countStr);
                activeEventCount--;
                String text = (activeEventCount == 0) ? "" : "" + activeEventCount;
                vTextActiveCount.setText(text);
            }
        });

        utils.logBoth(logID, thisEventPath.getName());
    }


    void bytes2File(byte[] bytes, File file) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            utils.logE("snap", "IOException catch", e);
        } finally {
            bytes = null;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    utils.logE("snap", "IOException finally", e);
                }
            }
            fileOutputStream = null;
        }
    }

}
