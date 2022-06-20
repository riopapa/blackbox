package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.mContext;
import static com.urrecliner.blackbox.Vars.tvDegree;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextTime;

import android.graphics.Color;
import android.widget.TextView;

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
                String txt = (celcius>43)? ">"+celcius+"<":" "+celcius+" ";
                if (celcius>42)
                    utils.beepOnce(9, 1f);
                mActivity.runOnUiThread(() -> {
                    tvDegree.setText(txt);
                    tvDegree.setTextColor((celcius<42)? Color.WHITE : Color.MAGENTA);
                    tvDegree.setBackgroundColor(ContextCompat.getColor(mContext,
                            (celcius<40)? R.color.baseColor : R.color.hotColor));
                });
            }
        };
        displayHHMM.schedule(tt, 3000, 32000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}