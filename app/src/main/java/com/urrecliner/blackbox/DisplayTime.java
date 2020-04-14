package com.urrecliner.blackbox;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackbox.Vars.displayBattery;
import static com.urrecliner.blackbox.Vars.gpsTracker;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vTextTime;

class DisplayTime implements Runnable {

    private double dLatitude = 0, dLongitude = 0;
    DisplayTime() { }
    private Timer displayTime = new Timer();
    public void run() {
        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                vTextTime.setText(utils.getNowTimeString("HH:mm"));
                displayBattery.showBattery();
                gpsTracker.askLocation();
//                if (System.currentTimeMillis() > (gpsUpdateTime + 7000)) {
//                    isSatelliteShown = false;
//                    isCompassShown = false;
//                    dLatitude = 0;
//                    dLongitude = 0;
//                    gpsTracker.askLocation();
//                    vSatellite.setVisibility(View.INVISIBLE);
//                    vCompass.setVisibility(View.INVISIBLE);
//                } else {
//                    dLatitude = gpsTracker.getLatitude();
//                    dLongitude = gpsTracker.getLongitude();
//                }
            }
        };
        displayTime.schedule(tt, 100, 59000);
    }

    void stop() {
        displayTime.cancel();
    }
}

