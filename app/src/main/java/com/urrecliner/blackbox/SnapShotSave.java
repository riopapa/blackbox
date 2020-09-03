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

    void start(File path2Write, int snapIdx, boolean first) {
        jpgBytes = new byte[MAX_IMAGES_SIZE+1][];
        jpgIdx = 0;
        for (int i = snapIdx; i < MAX_IMAGES_SIZE; i++)
            jpgBytes[jpgIdx++] = snapBytes[i];
        for (int i = 0; i < snapIdx; i++)
            jpgBytes[jpgIdx++] = snapBytes[i];
        startBias = (first) ? 100: 216; // for snapshot image sequence, dependency : snap interval, snap size
        int startIdx = (first) ? 0: 0;
        int finishIdx = (first) ? MAX_IMAGES_SIZE-3: MAX_IMAGES_SIZE-15;  // to minimize snapshot image counts
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                for (jpgIdx = startIdx; jpgIdx < finishIdx; jpgIdx++) {
                    byte [] imageBytes = jpgBytes[jpgIdx];
//                    Log.w("idx "+ jpgIdx,"log "+jpgFile.getName());
                    if (imageBytes != null && imageBytes.length > 1) {
                        File imageFile = new File(path2Write, "SnapShot_" + ("" + (startBias + (jpgIdx * SNAP_SHOT_INTERVAL) / 1100)) + "." + jpgIdx + ".jpg");
                        Log.w("id",imageFile.getName());
                        bytes2File(imageBytes, imageFile);
//
//                        BitMapSave bs = new BitMapSave();
//                        bs.save(jpgBytes[jpgIdx], jpgFile);
                        SystemClock.sleep(40);  // not to hold too long time
                    }
                }
            }
        });
        th.start();
//                SnapAsyncTask snapAsyncTask = new SnapAsyncTask();
//                snapAsyncTask.execute(""+idx);

        if (!first)
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
