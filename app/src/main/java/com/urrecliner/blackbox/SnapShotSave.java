package com.urrecliner.blackbox;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.Toast;

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

    private String logID = "imageSave";
    private CountDownTimer countDownTimer;
    private int idx;

    void start(final File thisEventPath, final byte[][] jpgBytes) {
        utils.logBoth(logID, "Saving SnapShots ..");
        idx =0;
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                countDownTimer = new CountDownTimer(320 * MAX_IMAGES_SIZE, 300) {
                    public void onTick(long millisUntilFinished) {
//                        utils.logOnly(logID, "idx="+idx);
                        if (idx < MAX_IMAGES_SIZE) {
                            if (jpgBytes[idx] != null && jpgBytes[idx].length > 0) {
                                final File jpgFile = new File(thisEventPath, "SnapShot" + "_" + ("" + (1000 + idx)).substring(1, 4) + ".jpg");
                                bytes2File(jpgBytes[idx], jpgFile);
                            }
                        }
                        idx++;
                    }
                    public void onFinish() {
                        utils.beepOnce(3, .7f);
                        String countStr = "" + ++CountEvent;
                        vTextCountEvent.setText(countStr);
                        activeEventCount--;
                        String text = (activeEventCount == 0) ? "" : "< " + activeEventCount + " >\n";
                        vTextActiveCount.setText(text);
                        ImageButton mEventButton = mActivity.findViewById(R.id.btnEvent);
                        mEventButton.setImageResource(R.mipmap.event_ready);
                        try {
                            countDownTimer.cancel();
//                        utils.customToast("Event Recording completed", Toast.LENGTH_SHORT, Color.CYAN);
                            utils.logBoth(logID, thisEventPath.getName());
                        } catch (Exception e) {
                            utils.logE(logID,"onFinish after saving snapshot", e);
                        }
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
