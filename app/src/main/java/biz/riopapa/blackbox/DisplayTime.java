package biz.riopapa.blackbox;

import static biz.riopapa.blackbox.Vars.displayBattery;
import static biz.riopapa.blackbox.Vars.mActivity;
import static biz.riopapa.blackbox.Vars.mContext;
import static biz.riopapa.blackbox.Vars.mIsRecording;
import static biz.riopapa.blackbox.Vars.mainLayout;
import static biz.riopapa.blackbox.Vars.startStopExit;
import static biz.riopapa.blackbox.Vars.tvDegree;
import static biz.riopapa.blackbox.Vars.utils;
import static biz.riopapa.blackbox.Vars.vPreviewView;
import static biz.riopapa.blackbox.Vars.vTextTime;
import static biz.riopapa.blackbox.Vars.viewFinderActive;

import biz.riopapa.blackbox.utility.Celcius;

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
                        startStopExit.stopVideo();
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
                            (celDegree<42)? R.color.baseColor : R.color.hotColor));
                    mainLayout.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celDegree<44)? R.color.baseColor : R.color.hotColor));
                    displayBattery.show();
                });
            }
        };
        displayHHMM.schedule(tt, 3000, 50000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}