package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.CountEvent;
import static com.urrecliner.blackbox.Vars.activeEventCount;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.tvCelcius;
import static com.urrecliner.blackbox.Vars.vTextActiveCount;
import static com.urrecliner.blackbox.Vars.vTextCountEvent;
import static com.urrecliner.blackbox.Vars.vTextTime;

import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.urrecliner.blackbox.utility.Celcius;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

class DisplayTime implements Runnable {

    DisplayTime() { }
    private final Timer displayHHMM = new Timer();
    public void run() {
        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                vTextTime.setText(sdf.format(System.currentTimeMillis()));
                int celcius = Celcius.get();
                String txt = " "+celcius+" ";
                tvCelcius.setText(txt);
                mActivity.runOnUiThread(() -> {
                    if (celcius<40) {
                        tvCelcius.setTextColor(Color.WHITE);
                        tvCelcius.setBackgroundColor(ContextCompat.getColor(mContext, R.color.baseColor));
                    } else {
                        tvCelcius.setTextColor(Color.RED);
                        tvCelcius.setBackgroundColor(ContextCompat.getColor(mContext, R.color.hotColor));
                    }
                });
            }
        };
        displayHHMM.schedule(tt, 3000, 32000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}