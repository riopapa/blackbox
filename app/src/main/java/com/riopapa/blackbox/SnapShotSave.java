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
import static com.riopapa.blackbox.utility.ImageStack.snapBytes;
import static com.riopapa.blackbox.utility.ImageStack.snapTime;

class SnapShotSave {

    int startBias;
    int maxSize;
    String prefixTime;
    long [] times;
    byte [][] jpgBytes;

    void startSave(File path2Write, final int phase, boolean last) {

        final int minPos = 0;

        maxSize = share_image_size - 2;
        if (phase == 2)
            maxSize = share_image_size - 4;
        else if (phase == 3)
            maxSize = share_image_size - 6;
        else if (phase == 4)
            maxSize = share_image_size - 8;

        times = new long[share_image_size];
        jpgBytes = new byte[share_image_size][];
        getClone(imageStack.snapNowPos);
        long veryFirst = times[0];
        Log.w("x", path2Write+" "+veryFirst);
        startBias = phase * 200;
        prefixTime = path2Write.getName();
        prefixTime = "D"+prefixTime.substring(1, prefixTime.length()-1)+".";
        Thread th = new Thread(() -> {
            for (int i = minPos; i < maxSize; i++) {
                if (jpgBytes[i] == null)
                    continue;
                int inc = (int) ((times[i] - veryFirst) / 10);
                int sec = inc / 100 / 60;
                int mil = inc - sec * 60 * 100;
                File imageFile = new File(path2Write, prefixTime + ((sec+1)*1000+mil) + ".jpg");
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
                    vTextCountEvent.setText(String.valueOf(CountEvent));
                    activeEventCount--;
                    String text = (activeEventCount == 0) ? "" : " "+activeEventCount+" ";
                    vTextActiveCount.setText(text);
                });
                utils.logBoth("finish", path2Write.getName());
                System.gc();
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

    void getClone(int startPos) {

        int jpgIdx = 0;
        for (int i = startPos; i < share_image_size; i++) {
            if (snapBytes[i] != null) {
                times[jpgIdx] = snapTime[i];
                jpgBytes[jpgIdx++] = snapBytes[i].clone();
                snapBytes[i] = null;
            }
        }
        for (int i = 0; i < startPos-1; i++) {
            if (jpgIdx >= share_image_size)
                break;
            if (snapBytes[i] != null) {
                times[jpgIdx] = snapTime[i];
                jpgBytes[jpgIdx++] = snapBytes[i].clone();
                snapBytes[i] = null;
            }
        }
    }


}