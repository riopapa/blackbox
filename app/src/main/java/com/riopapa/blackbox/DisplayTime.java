package com.riopapa.blackbox;

import static com.riopapa.blackbox.MainActivity.stopHandler;
import static com.riopapa.blackbox.Vars.displayBattery;
import static com.riopapa.blackbox.Vars.mActivity;
import static com.riopapa.blackbox.Vars.mContext;
import static com.riopapa.blackbox.Vars.mIsRecording;
import static com.riopapa.blackbox.Vars.mainLayout;
import static com.riopapa.blackbox.Vars.tvDegree;
import static com.riopapa.blackbox.Vars.utils;
import static com.riopapa.blackbox.Vars.vTextTime;
import com.riopapa.blackbox.utility.Celcius;

import android.graphics.Color;
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
                int celcius = Celcius.get();
                String txt = (celcius>42)? ">"+celcius+"<":" "+celcius+" ";
                if (celcius>44) {
                    utils.beepOnce(10, 1f);
                    if (mIsRecording)
                        stopHandler.sendEmptyMessage(0);
                    utils.beepOnce(10, 1f);
                    new BeBackSoon().execute("x");
                } else if (celcius>42)
                    utils.beepOnce(10, 1f);
                else if (celcius>40)
                    utils.beepOnce(9, 1f);
                mActivity.runOnUiThread(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    vTextTime.setText(sdf.format(System.currentTimeMillis()));
                    tvDegree.setText(txt);
                    tvDegree.setTextColor((celcius<44)? Color.WHITE : Color.YELLOW);
                    tvDegree.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celcius<41)? R.color.baseColor : R.color.hotColor));
                    mainLayout.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celcius<44)? R.color.baseColor : R.color.hotColor));
                    displayBattery.show();
                });
            }
        };
        displayHHMM.schedule(tt, 3000, 32000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}