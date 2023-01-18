package com.urrecliner.blackbox;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.imageStack;
import static com.urrecliner.blackbox.Vars.share_image_size;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    int startBias;
    int maxSize;
    String prefixTime;
    void startSave(File path2Write, final int phase) {

        final int minPos = (phase == 0)? 30: 0;

        maxSize = share_image_size - 15;
        if (phase == 2)
            maxSize = share_image_size - 20;
        else if (phase == 3)
            maxSize = share_image_size - 30;
        else if (phase == 4)
            maxSize = share_image_size - 20;

        byte [][] jpgBytes = imageStack.getClone();

        startBias = phase * 200;
        prefixTime = path2Write.getName();
        prefixTime = "D"+prefixTime.substring(1, prefixTime.length()-1)+".";
        Thread th = new Thread(() -> {
            for (int i = minPos; i < maxSize; i++) {
                if (jpgBytes[i] == null)
                    continue;
                jpgBytes[i] = null;
                File imageFile = new File(path2Write, prefixTime + (startBias + i) + ".jpg");
                if (jpgBytes[i].length > 1) {
                    bytes2File(jpgBytes[i], imageFile);
                    SystemClock.sleep(22);  // not to hold all the time
                } else
                    Log.e( phase+" image error "+i, imageFile.getName());
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
                utils.logBoth("finish", path2Write.getName());
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