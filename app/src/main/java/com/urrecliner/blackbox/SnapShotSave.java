package com.urrecliner.blackbox;

import android.os.CountDownTimer;
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
    private CountDownTimer countDownTimer;
    private int idx;
    static byte[][] jpgBytes;
    static int startBias;

    void start(File path2Write, int snapIdx, boolean first) {
        jpgBytes = new byte[MAX_IMAGES_SIZE+1][];
        idx = 0;
        for (int i = snapIdx; i < MAX_IMAGES_SIZE; i++)
            jpgBytes[idx++] = snapBytes[i];
        for (int i = 0; i < snapIdx; i++)
            jpgBytes[idx++] = snapBytes[i];
        startBias = (first) ? 100: 219; // for snapshot image sequence, dependency : snap interval, snap size
        int startIdx = (first) ? 0: 11;
        int finishIdx = (first) ? MAX_IMAGES_SIZE-3: MAX_IMAGES_SIZE-25;  // to minimize snapshot image counts
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int idx = startIdx; idx < finishIdx; idx++) {
                    if (jpgBytes[idx] != null && jpgBytes[idx].length > 1) {
//                        Log.w("idx "+ idx,"log");
                        File jpgFile = new File(path2Write, "SnapShot_" + ("" + (startBias + (idx * SNAP_SHOT_INTERVAL) / 1100)) + "." + idx + ".jpg");
//                        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
//                        Bitmap bitmap = BitmapFactory.decodeByteArray( jpgBytes[idx], 0, jpgBytes[idx].length ) ;
//                        Bitmap converted = bitmap.copy(Bitmap.Config.RGB_565, false);
//                        converted.compress(Bitmap.CompressFormat.JPEG, 90, stream);
//                        byte[] byteArray = stream.toByteArray();
//                        bytes2File(byteArray, jpgFile);
//                        bitmap = null; converted = null; stream = null;
                        bytes2File(jpgBytes[idx], jpgFile);
                        jpgBytes[idx] = null;
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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

    private static void bytes2File(byte[] bytes, File file) {

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
