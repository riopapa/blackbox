package com.urrecliner.blackbox;

import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.SUFFIX;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    int startBias;
    int maxSize;
    String prefixTime;
    void startSave(File path2Write, final int snapPos, final int phase) {
        byte[][] jpgBytes;
        int jpgIdx = 0;
        maxSize = MAX_IMAGES_SIZE - 30;
        if (phase == 2)
            maxSize = MAX_IMAGES_SIZE - 30;
////        else if (phase == 3)
////            maxSize = MAX_IMAGES_SIZE - 30;
        else if (phase == 4)
            maxSize = MAX_IMAGES_SIZE - 40;
        jpgBytes = new byte[MAX_IMAGES_SIZE][];
        for (int i = snapPos; i < MAX_IMAGES_SIZE; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
            if (jpgIdx > maxSize)
                break;
        }
        for (int i = 0; i < snapPos; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
            if (jpgIdx > maxSize)
                break;
        }

        startBias = phase * 200;
        prefixTime = path2Write.getName();
        prefixTime = "D"+prefixTime.substring(1,prefixTime.length()-4)+" ";
        Thread th = new Thread(() -> {
            for (int i = 0; i < maxSize; i++) {
                byte[] imageBytes = jpgBytes[i];
                jpgBytes[i] = null;
                if (imageBytes != null && imageBytes.length > 1) {
                    File imageFile = new File(path2Write, prefixTime + (startBias + i) + SUFFIX + ".jpg");
                    bytes2File(imageBytes, imageFile);
                    SystemClock.sleep(30);  // not to hold all the time
                }
            }
            if (phase == 4) { // last phase
                utils.beepOnce(3, 1f);
                mActivity.runOnUiThread(() -> {
                    String countStr = "" + ++CountEvent;
                    vTextCountEvent.setText(countStr);
                    activeEventCount--;
                    String text = (activeEventCount == 0) ? "" : " "+activeEventCount+" ";
                    vTextActiveCount.setText(text);
                });
                utils.logBoth("snapshot", path2Write.getName());
            }
        });
        th.start();
    }

    void bytes2File(byte[] bytes, File file) {

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            utils.logE("snap", "IOException catch", e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    utils.logE("snap", "IOException finally", e);
                }
            }
        }
    }

}