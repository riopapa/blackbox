package com.urrecliner.blackbox;

import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.MAX_IMAGES_SIZE;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.snapBytes;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;

class SnapShotSave {

    byte[][] jpgBytes;
    int startBias;
    int maxSize;

    void startSave(File path2Write, int snapPointer, final int phase) {
        jpgBytes = new byte[MAX_IMAGES_SIZE][];
        int jpgIdx = 0;
        maxSize = MAX_IMAGES_SIZE - 20;
        if (phase == 2)
            maxSize = MAX_IMAGES_SIZE - 30;
        else if (phase == 3)
            maxSize = 40;
        for (int i = snapPointer; i < MAX_IMAGES_SIZE; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
            if (jpgIdx > maxSize)
                break;
        }
        for (int i = 0; i < snapPointer; i++) {
            jpgBytes[jpgIdx++] = snapBytes[i];
            snapBytes[i] = null;
            if (jpgIdx > maxSize)
                break;
        }
        Thread th = new Thread(() -> {
            startBias = phase * 200;
            for (int i = 0; i < maxSize; i++) {
                byte [] imageBytes = jpgBytes[i];
                if (imageBytes != null && imageBytes.length > 1) {
                    File imageFile = new File(path2Write, "CameraShot_" + ("" + (startBias + i)) + ".jpg");
                    bytes2File(imageBytes, imageFile);
                    jpgBytes[i] = null;
                    SystemClock.sleep(85);  // not to hold all the time
                }
            }
            if (phase == 3)
                sayEventCompleted(path2Write);
        });
        th.start();
    }

    private void sayEventCompleted(File thisEventPath) {
        utils.beepOnce(3, 1f);
        mActivity.runOnUiThread(() -> {
            String countStr = "" + ++CountEvent;
            vTextCountEvent.setText(countStr);
            activeEventCount--;
            String text = (activeEventCount == 0) ? "" : " "+activeEventCount+" ";
            vTextActiveCount.setText(text);
//            if (activeEventCount == 0)
//                try {
//                    zoom.setZoom(mCaptureRequestBuilder, 1.3f);
//                    mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
//                } catch (Exception e) { e.printStackTrace();}
        });

        String logID = "SnapShot";
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
