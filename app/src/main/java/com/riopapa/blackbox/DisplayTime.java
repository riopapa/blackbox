package com.riopapa.blackbox;

import static com.riopapa.blackbox.MainActivity.stopHandler;
import static com.riopapa.blackbox.Vars.displayBattery;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.mainLayout;
import static com.riopapa.blackbox.Vars.tvDegree;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vPreviewView;
import static com.riopapa.blackbox.Vars.vTextTime;
import static com.riopapa.blackbox.Vars.viewFinderActive;

import com.riopapa.blackbox.utility.Celcius;

import android.graphics.Color;
import android.view.View;

import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

class DisplayTime implements Runnable {

    private final Timer displayHHMM = new Timer();

    public void run() {
        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                int celDegree = Celcius.get();
                String txt = (celDegree>42)? ">"+celDegree+"<":" "+celDegree+" ";
                if (celDegree > 44) {
                    utils.beepOnce(10, 1f);
                    if (mIsRecording)
                        stopHandler.sendEmptyMessage(0);
                    utils.beepOnce(10, 1f);
                    new BeBackSoon().execute("x");
                } else if (celDegree > 43)
                    utils.beepOnce(10, 1f);
                else if (celDegree > 41) {
                    utils.beepOnce(9, 1f);
                    if (viewFinderActive) {
                        viewFinderActive = false;
                        vPreviewView.setVisibility(View.INVISIBLE);
                    }
                }
                mActivity.runOnUiThread(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    vTextTime.setText(sdf.format(System.currentTimeMillis()));
                    tvDegree.setText(txt);
                    tvDegree.setTextColor((celDegree<42)? Color.WHITE : Color.YELLOW);
                    tvDegree.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celDegree<40)? R.color.baseColor : R.color.hotColor));
                    mainLayout.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celDegree<43)? R.color.baseColor : R.color.hotColor));
                    displayBattery.show();
                });
            }
        };
        displayHHMM.schedule(tt, 3000, 57000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}