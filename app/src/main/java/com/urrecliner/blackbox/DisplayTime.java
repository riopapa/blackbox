package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.vTemperature;
import static com.urrecliner.blackbox.Vars.vTextTime;

import android.graphics.Color;

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
                vTemperature.setText(""+celcius);
                vTemperature.setTextColor((celcius > 42f)? Color.RED:((celcius > 38)? Color.YELLOW:Color.WHITE));
            }
        };
        displayHHMM.schedule(tt, 500, 18000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}