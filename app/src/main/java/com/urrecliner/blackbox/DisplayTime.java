package com.urrecliner.blackbox;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.vTextTime;

class DisplayTime implements Runnable {

    DisplayTime() { }
    private final Timer displayHHMM = new Timer();
    public void run() {
        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                vTextTime.setText(sdf.format(System.currentTimeMillis()));
                displayBattery.showBattery();
            }
        };
        displayHHMM.schedule(tt, 100, 58000);
    }

    void stop() {
        displayHHMM.cancel();
    }
}
