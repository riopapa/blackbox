package com.urrecliner.blackbox;

import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.gpsUpdateTime;
import static com.urrecliner.blackbox.Vars.isCompassShown;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextTime;

class DisplayTime implements Runnable {

    DisplayTime() { }
    private final Timer displayTime = new Timer();
    public void run() {
        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                vTextTime.setText(utils.getNowTimeString("HH:mm"));
                displayBattery.showBattery();
                gpsTracker.drawCompass((int) (Math.random()*16f));
//                gpsTracker.init();
//                gpsTracker.askLocation();
//                if (System.currentTimeMillis() > (gpsUpdateTime + 70000)) {
//                    utils.logBothac("GPS","GPS not Active"+" ");
//                    isCompassShown = false;
//                    gpsTracker.askLocation();
//                    gpsTracker.drawCompass((int)(Math.random()* 16)); // for testing GPS NEWS
//                    vWheel.setVisibility(View.INVISIBLE);
//                }
            }
        };
        displayTime.schedule(tt, 100, 58000);
    }

    void stop() {
        displayTime.cancel();
    }
}
