package com.riopapa.blackbox;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.riopapa.blackbox.Vars.CountEvent;
import static com.riopapa.blackbox.Vars.imageStack;
import static com.riopapa.blackbox.Vars.share_image_size;
import static com.riopapa.blackbox.Vars.activeEventCount;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vTextActiveCount;
import static com.riopapa.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    int startBias;
    int maxSize;
    String prefixTime;
    void startSave(File path2Write, final int phase, boolean last) {

        final int minPos = 0;

        maxSize = share_image_size - 1;
        if (phase == 2)
            maxSize = share_image_size - 12;
        else if (phase == 3)
            maxSize = share_image_size - 13;
        else if (phase == 4)
            maxSize = share_image_size - 20;

        byte [][] jpgBytes = imageStack.getClone(imageStack.snapNowPos);

        startBias = phase * 200;
        prefixTime = path2Write.getName();
        prefixTime = "D"+prefixTime.substring(1, prefixTime.length()-1)+".";
        Thread th = new Thread(() -> {
            for (int i = minPos; i < maxSize; i++) {
                if (jpgBytes[i] == null)
                    continue;
                File imageFile = new File(path2Write, prefixTime + (startBias + i) + ".jpg");
                if (jpgBytes[i].length > 1) {
                    bytes2File(jpgBytes[i], imageFile);
                    SystemClock.sleep(20);  // not to hold all the time
                } else
                    Log.e( phase+" image error "+i, imageFile.getName());
                jpgBytes[i] = null;
            }
            if (last) { // last phase
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